package net.splatcraft.neoforge.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftData;
import net.splatcraft.neoforge.data.WeaponSettings;
import net.splatcraft.neoforge.entity.ColoredEntity;
import net.splatcraft.neoforge.entity.InkProjectileEntity;
import net.splatcraft.neoforge.entity.SquidBumperEntity;
import net.splatcraft.neoforge.particle.InkSplashParticleOptions;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.worldink.InkBlockUtils;
import net.splatcraft.neoforge.worldink.InkDamageUtils;
import net.splatcraft.neoforge.worldink.InkExplosion;

public class WeaponItem extends Item {
    private static final int USE_DURATION = 72000;
    private static final String HIDE_TOOLTIP_KEY = "HideTooltip";
    private static final String SPLATLING_CHARGE_KEY = "SplatlingCharge";
    private static final String SPLATLING_FIRING_TICKS_KEY = "SplatlingFiringTicks";
    private static final String SPLATLING_TOTAL_FIRING_TICKS_KEY = "SplatlingTotalFiringTicks";
    private static final String SPLATLING_STORED_CHARGE_KEY = "SplatlingStoredCharge";
    private static final String SPLATLING_STORAGE_TICKS_KEY = "SplatlingStorageTicks";
    private static final String DUALIE_ROLL_COUNT_KEY = "DualieRollCount";
    private static final String DUALIE_ROLL_RESET_TICKS_KEY = "DualieRollResetTicks";
    private static final String DUALIE_TURRET_TICKS_KEY = "DualieTurretTicks";
    private static final String CHARGER_STORED_CHARGE_KEY = "ChargerStoredCharge";
    private static final String CHARGER_STORAGE_TICKS_KEY = "ChargerStorageTicks";

    private final String settingsId;
    private final ResourceLocation settingsLocation;
    private final WeaponClass weaponClass;

    public WeaponItem(String settingsId, WeaponClass weaponClass, Properties properties) {
        super(properties.stacksTo(1));
        this.settingsId = settingsId;
        this.settingsLocation = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, settingsId);
        this.weaponClass = weaponClass;
    }

    public String settingsId() {
        return settingsId;
    }

    public ResourceLocation settingsLocation() {
        return settingsLocation;
    }

    public WeaponSettings settings() {
        return SplatcraftData.weaponSettings(settingsLocation);
    }

    public WeaponClass weaponClass() {
        return weaponClass;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        if (SplatcraftPlayerInfoEvents.isSquid(player) && !player.isUsingItem()) {
            clearStoredCharge(stack);
        }

        boolean splatlingFiring = weaponClass == WeaponClass.SPLATLING && hasSplatlingFiring(stack);
        if (splatlingFiring) {
            tickSplatlingFiring(level, player, stack);
            splatlingFiring = hasSplatlingFiring(stack);
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem()) || splatlingFiring) {
            SplatcraftPlayerInfoEvents.setSquid(player, false);
            player.setSprinting(false);
            if (Inventory.isHotbarSlot(slotId)) {
                player.getInventory().selected = slotId;
            }
        }

        if (weaponClass == WeaponClass.CHARGER) {
            tickChargerStoredCharge(stack);
        }

        if (weaponClass == WeaponClass.SPLATLING && !player.getUseItem().equals(stack)) {
            tickSplatlingStoredCharge(stack);
        }

        if (weaponClass == WeaponClass.DUALIE) {
            tickDualieDodgeState(stack);
        }

        if (InkColorComponent.isColorLocked(stack)) {
            return;
        }
        int playerColor = SplatcraftPlayerInfoEvents.color(player);
        if (InkColorComponent.color(stack).orElse(-1) != playerColor) {
            InkColorComponent.setColor(stack, playerColor);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!InkColorComponent.lockFromInkwell(stack, entity)
                && !(stack.getItem() instanceof SubWeaponItem<?> && SubWeaponItem.isSingleUse(stack))) {
            InkColorComponent.clearFromInkClearingBlock(stack, entity);
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        float requiredInk = switch (weaponClass) {
            case CHARGER -> chargerInkConsumption(0.0F);
            case SPLATLING -> 0.1F;
            default -> inkConsumption();
        };
        if (!level.isClientSide && !InkTankUtils.canConsume(player, this, requiredInk)) {
            InkTankUtils.delayRecharge(player, this, recoveryCooldown());
            InkTankUtils.sendFailureFeedback(player, this, requiredInk, false);
            return InteractionResultHolder.fail(stack);
        }

        if (weaponClass == WeaponClass.CHARGER && hasStoredChargerCharge(stack) && !SplatcraftPlayerInfoEvents.isSquid(player)) {
            if (!level.isClientSide) {
                float charge = storedChargerCharge(stack);
                if (!tryFireCharger(level, player, stack, charge)) {
                    return InteractionResultHolder.fail(stack);
                }
                clearStoredChargerCharge(stack);
                playFireSound(level, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (isContinuousFire() || isDelayedSingleFire() || weaponClass == WeaponClass.ROLLER
                || weaponClass == WeaponClass.CHARGER || weaponClass == WeaponClass.SPLATLING) {
            if (weaponClass == WeaponClass.SPLATLING && !player.getCooldowns().isOnCooldown(this) && !hasSplatlingFiring(stack)) {
                clearSplatlingFiring(stack);
            }
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        }

        if (!level.isClientSide) {
            if (!tryFire(level, player, stack, true)) {
                return InteractionResultHolder.fail(stack);
            }
            playFireSound(level, player);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isContinuousFire() || isDelayedSingleFire() || weaponClass == WeaponClass.ROLLER
                || weaponClass == WeaponClass.CHARGER || weaponClass == WeaponClass.SPLATLING
                ? USE_DURATION
                : super.getUseDuration(stack, entity);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        if (weaponClass == WeaponClass.ROLLER) {
            tickRollerUse(level, player, stack, remainingUseDuration);
            return;
        }

        if (weaponClass == WeaponClass.CHARGER) {
            tickChargerUse(level, player, stack, remainingUseDuration);
            return;
        }

        if (weaponClass == WeaponClass.SPLATLING) {
            tickSplatlingUse(level, player, stack, remainingUseDuration);
            return;
        }

        if (isDelayedSingleFire()) {
            tickDelayedSingleFire(level, player, stack, remainingUseDuration);
            return;
        }

        if (!isContinuousFire()) {
            return;
        }

        int useTime = getUseDuration(stack, livingEntity) - remainingUseDuration;
        boolean primaryTurret = weaponClass == WeaponClass.DUALIE && isDualieTurretActive(stack);
        int startupTicks = continuousStartupTicks(stack, primaryTurret);
        int firingSpeed = continuousFiringSpeed(stack, primaryTurret);

        boolean primaryFireTick = isFireTick(useTime, startupTicks, firingSpeed, 0);
        ItemStack pairedDualie = weaponClass == WeaponClass.DUALIE ? pairedDualie(player) : ItemStack.EMPTY;
        WeaponItem pairedWeapon = pairedDualie.getItem() instanceof WeaponItem weapon ? weapon : null;
        boolean pairedTurret = pairedWeapon != null && pairedWeapon.isDualieTurretActive(pairedDualie);
        int pairedFiringSpeed = pairedWeapon == null ? 1 : pairedWeapon.continuousFiringSpeed(pairedDualie, pairedTurret);
        boolean pairedFireTick = pairedWeapon != null && pairedWeapon.weaponClass() == WeaponClass.DUALIE
                && isFireTick(
                        useTime,
                        pairedWeapon.continuousStartupTicks(pairedDualie, pairedTurret),
                        pairedFiringSpeed,
                        pairedFiringSpeed / 2);

        if (!primaryFireTick && !pairedFireTick) {
            return;
        }

        if (!level.isClientSide) {
            if (primaryFireTick && !tryFire(level, player, stack, false, primaryTurret)) {
                player.stopUsingItem();
                return;
            }
            if (primaryFireTick) {
                playFireSound(level, player);
            }
            if (pairedFireTick && !pairedWeapon.tryFire(level, player, pairedDualie, false, pairedTurret)) {
                player.stopUsingItem();
                return;
            }
            if (pairedFireTick) {
                pairedWeapon.playFireSound(level, player);
            }
        }
    }

    private void tickSplatlingUse(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) {
            return;
        }

        int useTime = getUseDuration(stack, player) - remainingUseDuration;
        float previousCharge = splatlingCharge(stack, Math.max(0, useTime - 1));
        float charge = splatlingCharge(stack, useTime);
        if ((previousCharge < 1.0F && charge >= 1.0F) || (previousCharge < 2.0F && charge >= 2.0F)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.SPLATLING_READY.get(),
                    SoundSource.PLAYERS, 0.7F, randomPitch(level, Math.min(1.0F, Math.max(0.5F, charge * 0.5F))));
        }
        float inkConsumption = splatlingInkConsumption(charge);
        if (useTime % 4 == 0 && !InkTankUtils.canConsume(player, this, inkConsumption)) {
            InkTankUtils.sendFailureFeedback(player, this, inkConsumption, false);
        }
    }

    private void tickChargerUse(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) {
            return;
        }

        int useTime = getUseDuration(stack, player) - remainingUseDuration;
        if (useTime == chargerChargeTime(player)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.CHARGER_READY.get(),
                    SoundSource.PLAYERS, 0.7F, randomPitch(level, 1.0F));
        }
        float inkConsumption = chargerInkConsumption(chargerCharge(player, useTime));
        if (useTime % 4 == 0 && !InkTankUtils.canConsume(player, this, inkConsumption)) {
            InkTankUtils.sendFailureFeedback(player, this, inkConsumption, false);
        }
    }

    private void tickRollerUse(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        boolean airborne = !player.onGround();
        JsonObject action = rollerAction(airborne);
        int startupTicks = Math.max(0, intValue(action, "startup_time", intValue(object(settings().raw(), "swing"), "startup_time", 0)));
        int useTime = getUseDuration(stack, player) - remainingUseDuration;
        boolean brush = booleanValue(settings().raw(), "is_brush", false);
        if (brush && useTime < startupTicks) {
            if (!level.isClientSide && tryRollerFling(level, player, stack, airborne)) {
                playFireSound(level, player);
            }
            return;
        }

        if (useTime < startupTicks) {
            return;
        }

        int fireTick = Math.max(1, startupTicks);
        if (!brush && !level.isClientSide && useTime == fireTick) {
            if (tryRollerFling(level, player, stack, airborne)) {
                playFireSound(level, player);
            } else {
                player.stopUsingItem();
                return;
            }
        }

        if (!level.isClientSide && useTime > fireTick) {
            tickRollerRoll(level, player, stack, useTime - fireTick);
        }
    }

    private void tickDelayedSingleFire(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        int useTime = getUseDuration(stack, player) - remainingUseDuration;
        int startupTicks = Math.max(0, settings().stats().startupTicks().orElse(0));
        if (useTime < startupTicks) {
            return;
        }

        if (!level.isClientSide) {
            if (tryFire(level, player, stack, true)) {
                playFireSound(level, player);
            }
        }
        player.stopUsingItem();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (weaponClass == WeaponClass.SPLATLING && livingEntity instanceof Player player) {
            releaseSplatling(stack, level, player, timeCharged);
            return;
        }

        if (weaponClass != WeaponClass.CHARGER || !(livingEntity instanceof Player player)) {
            super.releaseUsing(stack, level, livingEntity, timeCharged);
            return;
        }

        int useTime = getUseDuration(stack, livingEntity) - timeCharged;
        float charge = chargerCharge(player, useTime);
        if (charge <= 0.05F) {
            return;
        }
        if (SplatcraftPlayerInfoEvents.isSquid(player)) {
            if (!level.isClientSide && charge >= 1.0F) {
                storeChargerCharge(stack, charge);
            }
            return;
        }
        if (!level.isClientSide && tryFireCharger(level, player, stack, charge)) {
            playFireSound(level, player);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (weaponClass == WeaponClass.SPLATLING && livingEntity instanceof Player player) {
            releaseSplatling(stack, level, player, 0);
        }
        return stack;
    }

    private void releaseSplatling(ItemStack stack, Level level, Player player, int remainingUseDuration) {
        int useTime = getUseDuration(stack, player) - remainingUseDuration;
        float charge = splatlingCharge(stack, useTime);
        if (charge <= 0.05F) {
            return;
        }
        if (SplatcraftPlayerInfoEvents.isSquid(player)) {
            if (!level.isClientSide) {
                storeSplatlingCharge(stack, charge);
            }
            return;
        }
        if (!level.isClientSide && tryStartSplatlingFiring(player, stack, charge)) {
            clearStoredSplatlingCharge(stack);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.SPLATLING_FIRING.get(),
                    SoundSource.PLAYERS, 0.7F, randomPitch(level, 0.95F));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (InkColorComponent.isColorLocked(stack)) {
            tooltipComponents.add(colorName(InkColorComponent.colorOrDefault(stack)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.empty());
        }
        if (hideTooltip(stack)) {
            return;
        }

        WeaponSettings.WeaponStats stats = settings().stats();
        addStat(tooltipComponents, "damage", stats.maxDamage());
        addStat(tooltipComponents, "range", stats.range());
        addStat(tooltipComponents, "fire_rate", stats.firingSpeed().map(value -> (float) value));
        addStat(tooltipComponents, "ink_consumption", stats.inkConsumption());
        addStat(tooltipComponents, "mobility", Optional.of(stats.mobility()).filter(value -> value != 1.0F));

        if (tooltipFlag.isAdvanced()) {
            tooltipComponents.add(Component.literal(settingsLocation.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void addStat(List<Component> tooltipComponents, String key, Optional<Float> value) {
        value.ifPresent(number -> tooltipComponents.add(Component.translatable(
                "item.splatcraft.weapon_stat." + key,
                format(number)
        ).withStyle(ChatFormatting.GRAY)));
    }

    private static String format(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }

        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static boolean hideTooltip(ItemStack stack) {
        return customData(stack).getBoolean(HIDE_TOOLTIP_KEY);
    }

    private static MutableComponent colorName(int color) {
        return InkColorData.builtInId(color)
                .map(WeaponItem::colorName)
                .orElseGet(() -> Component.literal(String.format("#%06X", color & 0xFFFFFF)));
    }

    private static MutableComponent colorName(ResourceLocation id) {
        return Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath());
    }

    private void fireProjectiles(Level level, Player player, ItemStack stack) {
        fireProjectiles(level, player, stack, false);
    }

    private void fireProjectiles(Level level, Player player, ItemStack stack, boolean dualieTurret) {
        JsonObject projectile = dualieTurret ? dualieTurretProjectile() : object(settings().raw(), "projectile");
        JsonObject shot = dualieTurret ? dualieTurretShot() : object(settings().raw(), "shot");
        JsonObject baseProjectile = object(settings().raw(), "projectile");
        JsonObject baseShot = object(settings().raw(), "shot");
        int count = Math.max(1, intValue(projectile, "count", 1));
        float angleBetween = floatValue(projectile, "angle_between_projectiles", 0.0F);
        float speed = floatValue(projectile, "speed", floatValue(baseProjectile, "speed", 1.5F));
        float pitchCompensation = floatValue(shot, "pitch_compensation", floatValue(baseShot, "pitch_compensation", 0.0F));
        float defaultInaccuracy = weaponClass == WeaponClass.SLOSHER ? 2.0F : 0.0F;
        float inaccuracy = player.onGround()
                ? floatValue(shot, "ground_inaccuracy", floatValue(baseShot, "ground_inaccuracy", defaultInaccuracy))
                : floatValue(shot, "airborne_inaccuracy", floatValue(shot, "air_inaccuracy",
                        floatValue(baseShot, "airborne_inaccuracy", floatValue(baseShot, "air_inaccuracy", defaultInaccuracy))));

        for (int index = 0; index < count; index++) {
            float yawOffset = (index - (count - 1) / 2.0F) * angleBetween;
            InkProjectileEntity projectileEntity = InkProjectileEntity.create(level, player, stack);
            if (dualieTurret) {
                projectileEntity.setDualieTurret(true);
                projectileEntity.setSize(floatValue(projectile, "size", floatValue(baseProjectile, "size", projectileEntity.projectileSize())));
            }
            projectileEntity.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, pitchCompensation, speed, inaccuracy);
            level.addFreshEntity(projectileEntity);
        }
    }

    private boolean tryFireCharger(Level level, Player player, ItemStack stack, float charge) {
        if (!InkTankUtils.tryConsume(player, this, chargerInkConsumption(charge), chargerRecoveryCooldown(), true)) {
            return false;
        }

        fireChargerProjectile(level, player, stack, charge);
        player.getCooldowns().addCooldown(this, chargerEndlagTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private void fireChargerProjectile(Level level, Player player, ItemStack stack, float charge) {
        JsonObject projectile = object(settings().raw(), "projectile");
        InkProjectileEntity projectileEntity = InkProjectileEntity.create(level, player, stack);
        projectileEntity.setChargerCharge(charge);
        projectileEntity.setSize(floatValue(projectile, "size", projectileEntity.projectileSize()));
        projectileEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                floatValue(projectile, "speed", 1.5F), 0.1F);
        level.addFreshEntity(projectileEntity);
    }

    private boolean tryStartSplatlingFiring(Player player, ItemStack stack, float charge) {
        if (!InkTankUtils.tryConsume(player, this, splatlingInkConsumption(charge), splatlingRecoveryCooldown(), true)) {
            return false;
        }

        int firingTicks = splatlingFiringTicks(charge);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putFloat(SPLATLING_CHARGE_KEY, charge);
            tag.putInt(SPLATLING_FIRING_TICKS_KEY, firingTicks);
            tag.putInt(SPLATLING_TOTAL_FIRING_TICKS_KEY, firingTicks);
        });
        if (!canRechargeWhileSplatlingFiring()) {
            player.getCooldowns().addCooldown(this, firingTicks);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private void tickSplatlingFiring(Level level, Player player, ItemStack stack) {
        CompoundTag tag = customData(stack);
        int firingTicks = tag.getInt(SPLATLING_FIRING_TICKS_KEY);
        if (firingTicks <= 0) {
            clearSplatlingFiring(stack);
            return;
        }

        float charge = clamp(tag.getFloat(SPLATLING_CHARGE_KEY), 0.0F, 2.0F);
        if (SplatcraftPlayerInfoEvents.isSquid(player)) {
            refundInterruptedSplatling(player, stack, charge, firingTicks);
            return;
        }

        int elapsed = Math.max(0, splatlingFiringTicks(charge) - firingTicks);
        if (elapsed % splatlingFiringSpeed(charge) == 0) {
            fireSplatlingProjectiles(level, player, stack, charge);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.SPLATLING_FIRING.get(),
                    SoundSource.PLAYERS, 0.7F, randomPitch(level, 0.95F));
        }

        int nextTicks = firingTicks - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutableTag -> {
            if (nextTicks <= 0) {
                mutableTag.remove(SPLATLING_CHARGE_KEY);
                mutableTag.remove(SPLATLING_FIRING_TICKS_KEY);
                mutableTag.remove(SPLATLING_TOTAL_FIRING_TICKS_KEY);
            } else {
                mutableTag.putInt(SPLATLING_FIRING_TICKS_KEY, nextTicks);
            }
        });
    }

    private void fireSplatlingProjectiles(Level level, Player player, ItemStack stack, float charge) {
        JsonObject projectile = splatlingProjectile(charge);
        JsonObject baseProjectile = object(settings().raw(), "projectile");
        JsonObject shot = splatlingShot(charge);
        JsonObject baseShot = object(settings().raw(), "shot");
        int count = Math.max(1, intValue(projectile, "count", intValue(baseProjectile, "count", 1)));
        float speed = floatValue(projectile, "speed", floatValue(baseProjectile, "speed", 1.0F));
        float pitchCompensation = floatValue(shot, "pitch_compensation", floatValue(baseShot, "pitch_compensation", 0.0F));
        float inaccuracy = player.onGround()
                ? floatValue(shot, "ground_inaccuracy", floatValue(baseShot, "ground_inaccuracy", 6.0F))
                : floatValue(shot, "airborne_inaccuracy", floatValue(baseShot, "airborne_inaccuracy", 12.0F));

        for (int index = 0; index < count; index++) {
            InkProjectileEntity projectileEntity = InkProjectileEntity.create(level, player, stack);
            projectileEntity.setSplatlingCharge(charge);
            projectileEntity.setSize(floatValue(projectile, "size", floatValue(baseProjectile, "size", projectileEntity.projectileSize())));
            projectileEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), pitchCompensation, speed, inaccuracy);
            level.addFreshEntity(projectileEntity);
        }
    }

    private void clearSplatlingFiring(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(SPLATLING_CHARGE_KEY);
            tag.remove(SPLATLING_FIRING_TICKS_KEY);
            tag.remove(SPLATLING_TOTAL_FIRING_TICKS_KEY);
        });
    }

    private void refundInterruptedSplatling(Player player, ItemStack stack, float charge, int remainingFiringTicks) {
        int totalFiringTicks = Math.max(1, customData(stack).getInt(SPLATLING_TOTAL_FIRING_TICKS_KEY));
        float refundScale = clamp(remainingFiringTicks / (float) totalFiringTicks, 0.0F, 1.0F);
        InkTankUtils.refund(player, this, splatlingInkConsumption(charge) * refundScale);
        player.getCooldowns().removeCooldown(this);
        clearSplatlingFiring(stack);
    }

    public static boolean hasActiveChargeState(ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return false;
        }
        return switch (weapon.weaponClass()) {
            case CHARGER -> hasStoredChargerCharge(stack);
            case SPLATLING -> hasSplatlingFiring(stack) || hasStoredSplatlingCharge(stack);
            default -> false;
        };
    }

    public float hudCharge(Player player, ItemStack stack) {
        if (player.isUsingItem() && player.getUseItem().equals(stack)) {
            int useTime = getUseDuration(stack, player) - player.getUseItemRemainingTicks();
            return switch (weaponClass) {
                case CHARGER -> chargerChargeForUse(player, useTime);
                case SPLATLING -> splatlingChargeForUse(stack, useTime);
                default -> 0.0F;
            };
        }

        return switch (weaponClass) {
            case CHARGER -> storedChargerCharge(stack);
            case SPLATLING -> hasSplatlingFiring(stack)
                    ? clamp(customData(stack).getFloat(SPLATLING_CHARGE_KEY), 0.0F, 2.0F)
                    : storedSplatlingCharge(stack);
            default -> 0.0F;
        };
    }

    public float hudMaxCharge() {
        return switch (weaponClass) {
            case CHARGER -> 1.0F;
            case SPLATLING -> 2.0F;
            default -> 0.0F;
        };
    }

    public static void clearStoredCharge(ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return;
        }
        switch (weapon.weaponClass()) {
            case CHARGER -> clearStoredChargerCharge(stack);
            case SPLATLING -> clearStoredSplatlingCharge(stack);
            default -> {
            }
        }
    }

    private static boolean hasSplatlingFiring(ItemStack stack) {
        return customData(stack).getInt(SPLATLING_FIRING_TICKS_KEY) > 0;
    }

    private boolean canRechargeWhileSplatlingFiring() {
        return booleanValue(object(settings().raw(), "charge"), "can_recharge_while_firing", false);
    }

    private float chargerCharge(Player player, int useTime) {
        return clamp(useTime / (float) chargerChargeTime(player), 0.0F, 1.0F);
    }

    public float chargerChargeForUse(Player player, int useTime) {
        return weaponClass == WeaponClass.CHARGER
                ? chargerCharge(player, Math.max(0, useTime))
                : 0.0F;
    }

    private int chargerChargeTime(Player player) {
        JsonObject charge = object(settings().raw(), "charge");
        int groundTime = Math.max(1, intValue(charge, "charge_time_ticks", Math.max(1, settings().stats().startupTicks().orElse(20))));
        return player.onGround()
                ? groundTime
                : Math.max(1, intValue(charge, "airborne_charge_time_ticks", groundTime));
    }

    private float chargerInkConsumption(float charge) {
        JsonObject shot = object(settings().raw(), "shot");
        float minInkConsumption = floatValue(shot, "min_charge_ink_consumption", inkConsumption());
        float maxInkConsumption = floatValue(shot, "full_charge_ink_consumption", minInkConsumption);
        return minInkConsumption + (maxInkConsumption - minInkConsumption) * clamp(charge, 0.0F, 1.0F);
    }

    private int chargerRecoveryCooldown() {
        return Math.max(1, intValue(object(settings().raw(), "shot"), "ink_recovery_cooldown", recoveryCooldown()));
    }

    private int chargerEndlagTicks() {
        return Math.max(1, intValue(object(settings().raw(), "shot"), "endlag_ticks", useCooldown()));
    }

    private void storeChargerCharge(ItemStack stack, float charge) {
        int storageTicks = intValue(object(settings().raw(), "charge"), "charge_storage_ticks", 0);
        if (storageTicks <= 0) {
            clearStoredChargerCharge(stack);
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putFloat(CHARGER_STORED_CHARGE_KEY, clamp(charge, 0.0F, 1.0F));
            tag.putInt(CHARGER_STORAGE_TICKS_KEY, storageTicks);
        });
    }

    private void tickChargerStoredCharge(ItemStack stack) {
        CompoundTag tag = customData(stack);
        int storageTicks = tag.getInt(CHARGER_STORAGE_TICKS_KEY);
        if (storageTicks <= 0) {
            if (tag.contains(CHARGER_STORED_CHARGE_KEY) || tag.contains(CHARGER_STORAGE_TICKS_KEY)) {
                clearStoredChargerCharge(stack);
            }
            return;
        }

        int nextStorageTicks = storageTicks - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutableTag -> {
            if (nextStorageTicks <= 0) {
                mutableTag.remove(CHARGER_STORED_CHARGE_KEY);
                mutableTag.remove(CHARGER_STORAGE_TICKS_KEY);
            } else {
                mutableTag.putInt(CHARGER_STORAGE_TICKS_KEY, nextStorageTicks);
            }
        });
    }

    private static boolean hasStoredChargerCharge(ItemStack stack) {
        CompoundTag tag = customData(stack);
        return tag.getInt(CHARGER_STORAGE_TICKS_KEY) > 0 && tag.getFloat(CHARGER_STORED_CHARGE_KEY) > 0.05F;
    }

    private static float storedChargerCharge(ItemStack stack) {
        return clamp(customData(stack).getFloat(CHARGER_STORED_CHARGE_KEY), 0.0F, 1.0F);
    }

    private static void clearStoredChargerCharge(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(CHARGER_STORED_CHARGE_KEY);
            tag.remove(CHARGER_STORAGE_TICKS_KEY);
        });
    }

    private float splatlingCharge(ItemStack stack, int useTime) {
        float storedCharge = storedSplatlingCharge(stack);
        JsonObject charge = object(settings().raw(), "charge");
        int firstChargeTime = Math.max(1, intValue(charge, "first_charge_time_ticks", 20));
        int secondChargeTime = Math.max(1, intValue(charge, "second_charge_time_ticks", firstChargeTime));
        if (storedCharge < 1.0F) {
            float firstCharge = storedCharge + useTime / (float) firstChargeTime;
            if (firstCharge <= 1.0F) {
                return clamp(firstCharge, 0.0F, 1.0F);
            }
            return 1.0F + clamp((firstCharge - 1.0F) * firstChargeTime / secondChargeTime, 0.0F, 1.0F);
        }
        return storedCharge + clamp(useTime / (float) secondChargeTime, 0.0F, 2.0F - storedCharge);
    }

    public float splatlingChargeForUse(ItemStack stack, int useTime) {
        return weaponClass == WeaponClass.SPLATLING
                ? splatlingCharge(stack, Math.max(0, useTime))
                : 0.0F;
    }

    private void storeSplatlingCharge(ItemStack stack, float charge) {
        int storageTicks = intValue(object(settings().raw(), "charge"), "charge_storage_ticks", 0);
        if (storageTicks <= 0) {
            clearStoredSplatlingCharge(stack);
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putFloat(SPLATLING_STORED_CHARGE_KEY, clamp(charge, 0.0F, 2.0F));
            tag.putInt(SPLATLING_STORAGE_TICKS_KEY, storageTicks);
        });
    }

    private void tickSplatlingStoredCharge(ItemStack stack) {
        CompoundTag tag = customData(stack);
        int storageTicks = tag.getInt(SPLATLING_STORAGE_TICKS_KEY);
        if (storageTicks <= 0) {
            if (tag.contains(SPLATLING_STORED_CHARGE_KEY) || tag.contains(SPLATLING_STORAGE_TICKS_KEY)) {
                clearStoredSplatlingCharge(stack);
            }
            return;
        }

        int nextStorageTicks = storageTicks - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutableTag -> {
            if (nextStorageTicks <= 0) {
                mutableTag.remove(SPLATLING_STORED_CHARGE_KEY);
                mutableTag.remove(SPLATLING_STORAGE_TICKS_KEY);
            } else {
                mutableTag.putInt(SPLATLING_STORAGE_TICKS_KEY, nextStorageTicks);
            }
        });
    }

    private static boolean hasStoredSplatlingCharge(ItemStack stack) {
        CompoundTag tag = customData(stack);
        return tag.getInt(SPLATLING_STORAGE_TICKS_KEY) > 0 && tag.getFloat(SPLATLING_STORED_CHARGE_KEY) > 0.05F;
    }

    private static float storedSplatlingCharge(ItemStack stack) {
        CompoundTag tag = customData(stack);
        return tag.getInt(SPLATLING_STORAGE_TICKS_KEY) > 0
                ? clamp(tag.getFloat(SPLATLING_STORED_CHARGE_KEY), 0.0F, 2.0F)
                : 0.0F;
    }

    private static void clearStoredSplatlingCharge(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(SPLATLING_STORED_CHARGE_KEY);
            tag.remove(SPLATLING_STORAGE_TICKS_KEY);
        });
    }

    private float splatlingInkConsumption(float charge) {
        return floatValue(settings().raw(), "max_ink_consumption", inkConsumption()) * clamp(charge, 0.0F, 2.0F) * 0.5F;
    }

    private int splatlingRecoveryCooldown() {
        return Math.max(1, intValue(settings().raw(), "ink_recovery_cooldown", recoveryCooldown()));
    }

    private int splatlingTotalFiringDuration() {
        return Math.max(1, intValue(object(settings().raw(), "charge"), "total_firing_duration", Math.max(1, settings().stats().endlagTicks().orElse(20))));
    }

    private int splatlingFiringTicks(float charge) {
        return Math.max(1, Math.round(splatlingTotalFiringDuration() * clamp(charge, 0.0F, 2.0F) * 0.5F));
    }

    private int splatlingFiringSpeed(float charge) {
        return Math.max(1, intValue(splatlingShot(charge), "firing_speed", intValue(object(settings().raw(), "shot"), "firing_speed", 3)));
    }

    private JsonObject splatlingProjectile(float charge) {
        return mergedSplatlingObject("projectile", "second_charge_projectile", charge);
    }

    private JsonObject splatlingShot(float charge) {
        return mergedSplatlingObject("shot", "second_charge_shot", charge);
    }

    private JsonObject mergedSplatlingObject(String baseName, String secondName, float charge) {
        JsonObject merged = object(settings().raw(), baseName).deepCopy();
        if (charge > 1.0F) {
            for (var entry : object(settings().raw(), secondName).entrySet()) {
                merged.add(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    private boolean tryRollerFling(Level level, Player player, ItemStack stack, boolean airborne) {
        JsonObject action = rollerAction(airborne);
        float inkConsumption = floatValue(action, "ink_consumption", inkConsumption());
        int recoveryCooldown = intValue(action, "ink_recovery_cooldown", recoveryCooldown());
        if (!InkTankUtils.tryConsume(player, this, inkConsumption, recoveryCooldown, true)) {
            return false;
        }

        fireRollerProjectiles(level, player, stack, airborne, action);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private void fireRollerProjectiles(Level level, Player player, ItemStack stack, boolean airborne, JsonObject action) {
        JsonObject roll = object(settings().raw(), "roll");
        int rollSize = Math.max(1, intValue(roll, "ink_size", 1));
        boolean brush = booleanValue(settings().raw(), "is_brush", false);
        float speed = rollerProjectileSpeed(airborne, action, brush);
        float pitchCompensation = airborne ? 0.0F : floatValue(action, "projectile_pitch_compensation", 0.0F);
        int count = brush ? rollSize * 2 + 1 : rollSize;

        for (int index = 0; index < count; index++) {
            InkProjectileEntity projectileEntity = InkProjectileEntity.create(level, player, stack);
            projectileEntity.setSize(1.6F);
            projectileEntity.setRollerAirborne(airborne);
            float yawOffset = brush ? (index - count / 2.0F) * 20.0F : 0.0F;
            projectileEntity.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset,
                    pitchCompensation, speed, 0.05F);
            moveRollerProjectile(player, projectileEntity, airborne, brush, index, count);
            level.addFreshEntity(projectileEntity);
        }
    }

    private void tickRollerRoll(Level level, Player player, ItemStack stack, int rollTicks) {
        if (!player.onGround() || !isRollerMoving(player)) {
            return;
        }

        JsonObject roll = object(settings().raw(), "roll");
        float inkConsumption = rollerRollInkConsumption(roll, rollTicks);
        int recoveryCooldown = intValue(roll, "ink_recovery_cooldown", recoveryCooldown());
        if (!InkTankUtils.canConsume(player, this, inkConsumption)) {
            InkTankUtils.sendFailureFeedback(player, this, inkConsumption, false);
            player.stopUsingItem();
            return;
        }

        int rollSize = Math.max(1, intValue(roll, "ink_size", 1));
        int hitboxSize = Math.max(0, intValue(roll, "hitbox_size", rollSize));
        float rollDamage = floatValue(roll, "damage", 0.0F);
        int color = weaponColor(stack, player);
        ResourceLocation inkType = SplatcraftPlayerInfoEvents.playerInfo(player).inkType();
        RollState state = rollOnGround(level, player, rollSize, hitboxSize, rollDamage, color, inkType);
        if (!state.active()) {
            return;
        }

        if (!InkTankUtils.tryConsume(player, this, inkConsumption, recoveryCooldown, true)) {
            player.stopUsingItem();
            return;
        }
        if (state.recoil()) {
            applyRollerRecoil(player, 0.8D);
        }
    }

    public boolean isRollerRollActiveForUse(Player player, ItemStack stack) {
        if (weaponClass != WeaponClass.ROLLER || !player.isUsingItem() || !player.getUseItem().equals(stack) || !player.onGround()) {
            return false;
        }

        int useTime = getUseDuration(stack, player) - player.getUseItemRemainingTicks();
        return useTime > rollerRollStartTick();
    }

    public float rollerRollInkConsumptionForUse(Player player, ItemStack stack) {
        if (weaponClass != WeaponClass.ROLLER) {
            return 0.0F;
        }

        int useTime = Math.max(0, getUseDuration(stack, player) - player.getUseItemRemainingTicks());
        int rollTicks = Math.max(0, useTime - rollerRollStartTick());
        return rollerRollInkConsumption(object(settings().raw(), "roll"), rollTicks);
    }

    public boolean isRollerBrush() {
        return weaponClass == WeaponClass.ROLLER
                && booleanValue(settings().raw(), "is_brush", settingsId().contains("brush"));
    }

    private int rollerRollStartTick() {
        JsonObject action = rollerAction(false);
        int startupTicks = Math.max(0, intValue(action, "startup_time", intValue(object(settings().raw(), "swing"), "startup_time", 0)));
        return Math.max(1, startupTicks);
    }

    private RollState rollOnGround(
            Level level,
            Player player,
            int rollSize,
            int hitboxSize,
            float rollDamage,
            int color,
            ResourceLocation inkType
    ) {
        double forwardX = forwardX(player);
        double forwardZ = forwardZ(player);
        double sideX = sideX(player);
        double sideZ = sideZ(player);
        double forwardOffsetX = forwardX;
        double forwardOffsetZ = forwardZ;
        for (int distance = 1; distance <= 2; distance++) {
            forwardOffsetX = forwardX * distance;
            forwardOffsetZ = forwardZ * distance;
            BlockPos pos = BlockPos.containing(player.getX() + forwardOffsetX, player.getY(), player.getZ() + forwardOffsetZ);
            if (!InkBlockUtils.canInkPassthrough(level, pos)) {
                break;
            }
        }

        boolean active = false;
        boolean recoil = false;
        for (int index = 0; index < rollSize; index++) {
            double offset = index - (rollSize - 1) / 2.0D;
            double xOffset = sideX * offset;
            double zOffset = sideZ * offset;
            boolean hitboxColumn = index < hitboxSize;
            active |= inkRollerColumn(level, player, color, inkType, rollDamage, xOffset, zOffset, forwardOffsetX, forwardOffsetZ, forwardX, forwardZ, hitboxColumn);
            if (hitboxColumn) {
                RollState damageState = damageRollerColumn(level, player, color, rollDamage, xOffset, zOffset, forwardOffsetX, forwardOffsetZ);
                active |= damageState.active();
                recoil |= damageState.recoil();
            }
        }
        return new RollState(active, recoil);
    }

    private boolean inkRollerColumn(
            Level level,
            Player player,
            int color,
            ResourceLocation inkType,
            float rollDamage,
            double xOffset,
            double zOffset,
            double forwardOffsetX,
            double forwardOffsetZ,
            double fallbackForwardX,
            double fallbackForwardZ,
            boolean spawnParticles
    ) {
        for (int yOffset = 0; yOffset >= -3; yOffset--) {
            double columnForwardX = yOffset == -3 ? fallbackForwardX : forwardOffsetX;
            double columnForwardZ = yOffset == -3 ? fallbackForwardZ : forwardOffsetZ;
            BlockPos pos = BlockPos.containing(
                    player.getX() + xOffset + columnForwardX,
                    player.getY() + yOffset,
                    player.getZ() + zOffset + columnForwardZ);

            if (level.getBlockEntity(pos) instanceof ColoredBarrierBlockEntity barrier && barrier.canAllowThrough(player)) {
                continue;
            }
            if (InkBlockUtils.canInkPassthrough(level, pos)) {
                continue;
            }

            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            InkBlockUtils.InkResult result = InkBlockUtils.playerInkBlock(player, level, pos, color, rollDamage, inkType);
            if (yOffset != -3 && !hasFullFootprint(shape)) {
                InkBlockUtils.InkResult belowResult = InkBlockUtils.playerInkBlock(player, level, pos.below(), color, rollDamage, inkType);
                if (result == InkBlockUtils.InkResult.FAIL) {
                    result = belowResult;
                }
            }

            if (result != InkBlockUtils.InkResult.FAIL && spawnParticles) {
                spawnRollerInkParticle(level, color, player.getX() + xOffset + columnForwardX, pos, player.getZ() + zOffset + columnForwardZ, shape);
            }
            return result != InkBlockUtils.InkResult.FAIL;
        }
        return false;
    }

    private RollState damageRollerColumn(
            Level level,
            Player player,
            int color,
            float rollDamage,
            double xOffset,
            double zOffset,
            double forwardOffsetX,
            double forwardOffsetZ
    ) {
        if (rollDamage <= 0.0F) {
            return RollState.INACTIVE;
        }

        BlockPos attackPos = BlockPos.containing(player.getX() + xOffset + forwardOffsetX, player.getY() - 1.0D, player.getZ() + zOffset + forwardOffsetZ);
        AABB bounds = new AABB(
                attackPos.getX(),
                attackPos.getY(),
                attackPos.getZ(),
                attackPos.getX() + 1.0D,
                attackPos.getY() + 2.0D,
                attackPos.getZ() + 1.0D);
        boolean active = false;
        boolean recoil = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds, EntitySelector.NO_SPECTATORS.and(entity -> entity instanceof LivingEntity living && living.isAlive()))) {
            if (target == player || !canRollerDamageTarget(level, target, color)) {
                continue;
            }

            active = true;
            boolean damaged = InkDamageUtils.doRollDamage(level, target, rollDamage, color, player, player, settings().stats().fullDamageToMobs());
            if (!damaged || !isRollerTargetSplatted(target)) {
                recoil = true;
            }
        }
        return new RollState(active, recoil);
    }

    private float rollerRollInkConsumption(JsonObject roll, int rollTicks) {
        float rollConsumption = floatValue(roll, "ink_consumption", inkConsumption());
        float dashConsumption = floatValue(roll, "dash_consumption", rollConsumption);
        int dashTime = Math.max(1, intValue(roll, "dash_time", 1));
        float dashScale = Math.min(1.0F, Math.max(0.0F, rollTicks / (float) dashTime));
        return rollConsumption + (dashConsumption - rollConsumption) * dashScale;
    }

    private static boolean isRollerMoving(Player player) {
        double horizontalMoveSqr = new Vec3(player.getX() - player.xo, 0.0D, player.getZ() - player.zo).lengthSqr();
        return horizontalMoveSqr > 1.0E-5D || player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5D;
    }

    private static boolean hasFullFootprint(VoxelShape shape) {
        if (shape.isEmpty()) {
            return false;
        }
        AABB bounds = shape.bounds();
        return bounds.minX <= 0.0D && bounds.minZ <= 0.0D && bounds.maxX >= 1.0D && bounds.maxZ >= 1.0D;
    }

    private static void spawnRollerInkParticle(Level level, int color, double x, BlockPos pos, double z, VoxelShape shape) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double blockHeight = shape.isEmpty() ? 0.0D : shape.bounds().maxY;
        serverLevel.sendParticles(new InkSplashParticleOptions(color, 1.0F), x, pos.getY() + blockHeight + 0.1D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static boolean isRollerTargetSplatted(LivingEntity target) {
        if (target instanceof SquidBumperEntity bumper) {
            return bumper.inkHealth() <= 0.0F;
        }
        return !target.isAlive();
    }

    private static boolean canRollerDamageTarget(Level level, LivingEntity target, int color) {
        if (target instanceof SquidBumperEntity bumper && bumper.inkHealth() <= 0.0F) {
            return false;
        }

        int targetColor = targetColor(target);
        return targetColor < 0 || InkDamageUtils.canDamageColor(level, target.blockPosition(), targetColor, color);
    }

    private static int targetColor(LivingEntity target) {
        if (target instanceof Player player) {
            return SplatcraftPlayerInfoEvents.color(player);
        }
        return target instanceof ColoredEntity colored ? colored.splatcraftColor() : -1;
    }

    private static void applyRollerRecoil(Player player, double power) {
        player.setDeltaMovement(new Vec3(
                forwardX(player) * -power,
                player.getDeltaMovement().y(),
                forwardZ(player) * -power));
        player.hurtMarked = true;
    }

    private static int weaponColor(ItemStack stack, Player player) {
        return InkColorComponent.color(stack).orElse(SplatcraftPlayerInfoEvents.color(player));
    }

    private static double forwardX(Player player) {
        return Math.cos(Math.toRadians(player.getYRot() + 90.0F));
    }

    private static double forwardZ(Player player) {
        return Math.sin(Math.toRadians(player.getYRot() + 90.0F));
    }

    private static double sideX(Player player) {
        return Math.cos(Math.toRadians(player.getYRot()));
    }

    private static double sideZ(Player player) {
        return Math.sin(Math.toRadians(player.getYRot()));
    }

    private float rollerProjectileSpeed(boolean airborne, JsonObject action, boolean brush) {
        float speed = floatValue(action, "projectile_speed", 0.55F);
        if (airborne && !brush && !hasObject(settings().raw(), "fling")) {
            return speed * 1.3F;
        }
        return speed;
    }

    private static void moveRollerProjectile(Player player, InkProjectileEntity projectileEntity, boolean airborne, boolean brush, int index, int count) {
        double offset = index - (count - 1) / 2.0D;
        if (airborne) {
            double yOffset = Math.sin(Math.toRadians(player.getXRot() + 90.0F)) * offset;
            double horizontalScale = Math.cos(Math.toRadians(player.getXRot() + 90.0F));
            double xOffset = Math.cos(Math.toRadians(player.getYRot() + 90.0F)) * offset * horizontalScale;
            double zOffset = Math.sin(Math.toRadians(player.getYRot() + 90.0F)) * offset * horizontalScale;
            projectileEntity.moveTo(projectileEntity.getX() + xOffset, projectileEntity.getY() + yOffset, projectileEntity.getZ() + zOffset);
        } else if (brush) {
            projectileEntity.moveTo(projectileEntity.getX(), projectileEntity.getY() - player.getEyeHeight() / 2.0F, projectileEntity.getZ());
        } else {
            double xOffset = Math.cos(Math.toRadians(player.getYRot())) * offset;
            double zOffset = Math.sin(Math.toRadians(player.getYRot())) * offset;
            projectileEntity.moveTo(projectileEntity.getX() + xOffset, projectileEntity.getY() - player.getEyeHeight() / 2.0F, projectileEntity.getZ() + zOffset);
        }
    }

    private JsonObject rollerAction(boolean airborne) {
        JsonObject raw = settings().raw();
        return airborne && hasObject(raw, "fling") ? object(raw, "fling") : object(raw, "swing");
    }

    public boolean tryPerformDualieDodge(Player player, ItemStack stack, float leftImpulse, float forwardImpulse) {
        if (weaponClass != WeaponClass.DUALIE || player.level().isClientSide || player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        if (leftImpulse == 0.0F && forwardImpulse == 0.0F) {
            return false;
        }

        ItemStack pairedStack = pairedDualie(player);
        int rollCount = dualieRollCount(stack);
        int maxRolls = dualieMaxRolls(stack, pairedStack);
        if (rollCount >= maxRolls) {
            return false;
        }

        JsonObject dodgeRoll = object(settings().raw(), "dodge_roll");
        float inkConsumption = floatValue(dodgeRoll, "ink_consumption", 0.0F);
        int recoveryCooldown = intValue(dodgeRoll, "ink_recovery_cooldown", recoveryCooldown());
        if (!InkTankUtils.tryConsume(player, this, inkConsumption, recoveryCooldown, true)) {
            return false;
        }

        int nextRollCount = rollCount + 1;
        int standardTurretTicks = intValue(dodgeRoll, "turret_duration", 8);
        int finalTurretTicks = intValue(dodgeRoll, "final_roll_turret_duration", standardTurretTicks);
        int turretTicks = nextRollCount >= maxRolls ? finalTurretTicks : standardTurretTicks;
        int resetTicks = Math.max(1, Math.round(finalTurretTicks * 0.75F));
        setDualieDodgeState(stack, nextRollCount, resetTicks, turretTicks);
        if (pairedStack.getItem() instanceof WeaponItem pairedWeapon && pairedWeapon.weaponClass() == WeaponClass.DUALIE) {
            setDualieDodgeState(pairedStack, nextRollCount, resetTicks, turretTicks);
        }

        moveDualieDodge(player, leftImpulse, forwardImpulse, floatValue(dodgeRoll, "movement_impulse", 0.0F));
        inkDualieDodge(player, stack);
        player.getCooldowns().addCooldown(this, Math.max(1, turretTicks));
        levelPlayDualieDodge(player);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private static void moveDualieDodge(Player player, float leftImpulse, float forwardImpulse, float movementImpulse) {
        if (movementImpulse <= 0.0F) {
            return;
        }

        Vec3 input = new Vec3(leftImpulse, 0.0D, forwardImpulse);
        if (input.lengthSqr() <= 1.0E-7D) {
            return;
        }

        player.moveRelative(movementImpulse, input.normalize());
        player.setDeltaMovement(player.getDeltaMovement().x, 0.05D, player.getDeltaMovement().z);
        player.hurtMarked = true;
    }

    private static void levelPlayDualieDodge(Player player) {
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.DUALIE_DODGE.get(),
                SoundSource.PLAYERS, 0.7F, randomPitch(level, 0.95F));
    }

    private void inkDualieDodge(Player player, ItemStack stack) {
        Level level = player.level();
        InkExplosion.create(
                level,
                player,
                player,
                player.position(),
                1.2F,
                weaponColor(stack, player),
                SplatcraftPlayerInfoEvents.playerInfo(player).inkType(),
                0.0F,
                0.0F,
                0.0F,
                settings().stats().fullDamageToMobs());
    }

    private void tickDualieDodgeState(ItemStack stack) {
        CompoundTag tag = customData(stack);
        int turretTicks = tag.getInt(DUALIE_TURRET_TICKS_KEY);
        int resetTicks = tag.getInt(DUALIE_ROLL_RESET_TICKS_KEY);
        if (turretTicks <= 0 && resetTicks <= 0) {
            if (tag.contains(DUALIE_ROLL_COUNT_KEY) || tag.contains(DUALIE_TURRET_TICKS_KEY) || tag.contains(DUALIE_ROLL_RESET_TICKS_KEY)) {
                clearDualieDodgeState(stack);
            }
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutableTag -> {
            int nextTurretTicks = Math.max(0, turretTicks - 1);
            int nextResetTicks = Math.max(0, resetTicks - 1);
            if (nextTurretTicks > 0) {
                mutableTag.putInt(DUALIE_TURRET_TICKS_KEY, nextTurretTicks);
            } else {
                mutableTag.remove(DUALIE_TURRET_TICKS_KEY);
            }
            if (nextResetTicks > 0) {
                mutableTag.putInt(DUALIE_ROLL_RESET_TICKS_KEY, nextResetTicks);
            } else {
                mutableTag.remove(DUALIE_ROLL_RESET_TICKS_KEY);
                mutableTag.remove(DUALIE_ROLL_COUNT_KEY);
            }
        });
    }

    private static void clearDualieDodgeState(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(DUALIE_ROLL_COUNT_KEY);
            tag.remove(DUALIE_ROLL_RESET_TICKS_KEY);
            tag.remove(DUALIE_TURRET_TICKS_KEY);
        });
    }

    private static void setDualieDodgeState(ItemStack stack, int rollCount, int resetTicks, int turretTicks) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(DUALIE_ROLL_COUNT_KEY, rollCount);
            tag.putInt(DUALIE_ROLL_RESET_TICKS_KEY, resetTicks);
            tag.putInt(DUALIE_TURRET_TICKS_KEY, turretTicks);
        });
    }

    private static int dualieRollCount(ItemStack stack) {
        return Math.max(0, customData(stack).getInt(DUALIE_ROLL_COUNT_KEY));
    }

    private int dualieMaxRolls(ItemStack stack, ItemStack pairedStack) {
        int count = dualieRollCountFor(stack);
        if (pairedStack.getItem() instanceof WeaponItem pairedWeapon && pairedWeapon.weaponClass() == WeaponClass.DUALIE) {
            count += pairedWeapon.dualieRollCountFor(pairedStack);
        }
        return Math.max(1, count);
    }

    private int dualieRollCountFor(ItemStack stack) {
        return Math.max(0, Math.round(floatValue(object(settings().raw(), "dodge_roll"), "count", 0.0F)));
    }

    private boolean isDualieTurretActive(ItemStack stack) {
        return weaponClass == WeaponClass.DUALIE && customData(stack).getInt(DUALIE_TURRET_TICKS_KEY) > 0;
    }

    private int continuousStartupTicks(ItemStack stack, boolean dualieTurret) {
        if (dualieTurret) {
            return Math.max(0, intValue(dualieTurretShot(), "startup_ticks",
                    intValue(object(settings().raw(), "shot"), "startup_ticks", 0)));
        }
        return Math.max(0, settings().stats().startupTicks().orElse(0));
    }

    private int continuousFiringSpeed(ItemStack stack, boolean dualieTurret) {
        if (dualieTurret) {
            return Math.max(1, intValue(dualieTurretShot(), "firing_speed",
                    intValue(object(settings().raw(), "shot"), "firing_speed", useCooldown())));
        }
        return Math.max(1, settings().stats().firingSpeed().orElse(useCooldown()));
    }

    private JsonObject dualieTurretProjectile() {
        return mergedObject("projectile", "turret_projectile");
    }

    private JsonObject dualieTurretShot() {
        return mergedObject("shot", "turret_shot");
    }

    private JsonObject mergedObject(String baseName, String overrideName) {
        JsonObject merged = object(settings().raw(), baseName).deepCopy();
        for (var entry : object(settings().raw(), overrideName).entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    private int useCooldown() {
        WeaponSettings.WeaponStats stats = settings().stats();
        return Math.max(1, stats.firingSpeed()
                .or(() -> stats.endlagTicks())
                .or(() -> stats.startupTicks())
                .orElse(6));
    }

    private int recoveryCooldown() {
        return Math.max(1, settings().stats().inkRecoveryCooldown().orElse(useCooldown()));
    }

    protected float inkConsumption() {
        return settings().stats().inkConsumption().orElse(0.0F);
    }

    private boolean tryFire(Level level, Player player, ItemStack stack, boolean addItemCooldown) {
        return tryFire(level, player, stack, addItemCooldown, false);
    }

    private boolean tryFire(Level level, Player player, ItemStack stack, boolean addItemCooldown, boolean dualieTurret) {
        JsonObject shot = dualieTurret ? dualieTurretShot() : object(settings().raw(), "shot");
        float inkConsumption = floatValue(shot, "ink_consumption", inkConsumption());
        int recoveryCooldown = intValue(shot, "ink_recovery_cooldown", recoveryCooldown());
        if (!InkTankUtils.tryConsume(player, this, inkConsumption, recoveryCooldown, true)) {
            return false;
        }

        fireProjectiles(level, player, stack, dualieTurret);
        if (addItemCooldown) {
            player.getCooldowns().addCooldown(this, useCooldown());
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private static boolean isFireTick(int useTime, int startupTicks, int firingSpeed, int offsetTicks) {
        int adjustedUseTime = useTime - startupTicks - offsetTicks;
        return adjustedUseTime > 0 && (adjustedUseTime - 1) % firingSpeed == 0;
    }

    private static ItemStack pairedDualie(Player player) {
        InteractionHand usedHand = player.getUsedItemHand();
        InteractionHand pairedHand = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack pairedStack = player.getItemInHand(pairedHand);
        return pairedStack.getItem() instanceof WeaponItem weapon && weapon.weaponClass() == WeaponClass.DUALIE
                ? pairedStack
                : ItemStack.EMPTY;
    }

    private boolean isContinuousFire() {
        return weaponClass == WeaponClass.SHOOTER || weaponClass == WeaponClass.DUALIE;
    }

    private boolean isDelayedSingleFire() {
        return weaponClass == WeaponClass.BLASTER || weaponClass == WeaponClass.SLOSHER;
    }

    protected void playFireSound(Level level, Player player) {
        SoundEvent sound = fireSound();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                fireSoundVolume(), randomPitch(level, 0.95F));
    }

    protected float fireSoundVolume() {
        return switch (weaponClass) {
            case ROLLER -> 0.8F;
            default -> 0.7F;
        };
    }

    private SoundEvent fireSound() {
        return switch (weaponClass) {
            case SHOOTER -> SplatcraftSounds.SHOOTER_FIRING.get();
            case BLASTER -> SplatcraftSounds.BLASTER_FIRING.get();
            case ROLLER -> isRollerBrush() ? SplatcraftSounds.BRUSH_FLING.get() : SplatcraftSounds.ROLLER_FLING.get();
            case CHARGER -> SplatcraftSounds.CHARGER_SHOT.get();
            case DUALIE -> SplatcraftSounds.DUALIE_FIRING.get();
            case SLOSHER -> SplatcraftSounds.SLOSHER_SHOT.get();
            case SPLATLING -> SplatcraftSounds.SPLATLING_FIRING.get();
            case SUB -> SplatcraftSounds.SUB_THROW.get();
        };
    }

    protected static float randomPitch(Level level, float base) {
        return ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * base;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static JsonObject object(JsonObject parent, String name) {
        JsonElement element = parent.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static float floatValue(JsonObject object, String name, float fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsFloat()
                : fallback;
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : fallback;
    }

    private static boolean booleanValue(JsonObject object, String name, boolean fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean()
                : fallback;
    }

    private static boolean hasObject(JsonObject parent, String name) {
        JsonElement element = parent.get(name);
        return element != null && element.isJsonObject();
    }

    public enum WeaponClass {
        SHOOTER,
        BLASTER,
        ROLLER,
        CHARGER,
        DUALIE,
        SLOSHER,
        SPLATLING,
        SUB
    }

    private record RollState(boolean active, boolean recoil) {
        private static final RollState INACTIVE = new RollState(false, false);
    }
}
