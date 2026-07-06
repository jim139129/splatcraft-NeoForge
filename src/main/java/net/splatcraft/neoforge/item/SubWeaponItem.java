package net.splatcraft.neoforge.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.entity.sub.AbstractSubWeaponEntity;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.registry.SplatcraftStats;

public class SubWeaponItem<T extends AbstractSubWeaponEntity> extends WeaponItem {
    private static final int USE_DURATION = 72000;
    private static final String SINGLE_USE_KEY = "SingleUse";
    private static final String ENTITY_DATA_KEY = "EntityData";
    private static final String COOK_TIME_KEY = "CookTime";

    private final Supplier<EntityType<T>> entityType;

    public SubWeaponItem(String settingsId, Supplier<EntityType<T>> entityType, Properties properties) {
        super(settingsId, WeaponClass.SUB, properties);
        this.entityType = entityType;
    }

    public EntityType<T> entityType() {
        return entityType.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        float inkConsumption = inkConsumption();
        if (!isSingleUse(stack) && !level.isClientSide && !InkTankUtils.canConsume(player, this, inkConsumption)) {
            InkTankUtils.sendFailureFeedback(player, this, inkConsumption, true);
            return InteractionResultHolder.fail(stack);
        }
        if (holdTime() <= 0) {
            return useFromStack(level, player, stack);
        }

        clearCookTime(stack);
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    public InteractionResultHolder<ItemStack> useFromStack(Level level, Player player, ItemStack stack) {
        int cooldown = recoveryCooldown(stack);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        float inkConsumption = inkConsumption();
        if (!isSingleUse(stack) && !level.isClientSide && !InkTankUtils.canConsume(player, this, inkConsumption)) {
            InkTankUtils.sendFailureFeedback(player, this, inkConsumption, true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!isSingleUse(stack) && !InkTankUtils.tryConsume(player, this, inkConsumption, cooldown, true, true)) {
                return InteractionResultHolder.fail(stack);
            }
            player.swing(throwHand(player, stack), false);
            T entity = AbstractSubWeaponEntity.create(entityType(), level, player, stack);
            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), floatSetting("throw_angle", -30.0F),
                    floatSetting("throw_velocity", 0.75F), 0.0F);
            level.addFreshEntity(entity);
            clearCookTime(stack);
            player.getCooldowns().addCooldown(this, cooldown);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.awardStat(SplatcraftStats.SUBS_USED);
            if (isSingleUse(stack) && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.SUB_THROW.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public InteractionResultHolder<ItemStack> useFromStack(Level level, Player player, ItemStack stack, int useTime) {
        storeCookTime(stack, useTime);
        return useFromStack(level, player, stack);
    }

    public boolean canStartUseFromStack(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        float inkConsumption = inkConsumption();
        if (!isSingleUse(stack) && !level.isClientSide && !InkTankUtils.canConsume(player, this, inkConsumption)) {
            InkTankUtils.sendFailureFeedback(player, this, inkConsumption, true);
            return false;
        }
        return true;
    }

    private static InteractionHand throwHand(Player player, ItemStack stack) {
        return player.getOffhandItem().equals(stack) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int useTime = getUseDuration(stack, livingEntity) - remainingUseDuration;
        storeCookTime(stack, useTime);
        if (useTime >= holdTime()) {
            useFromStack(level, player, stack);
            livingEntity.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int useTime = getUseDuration(stack, livingEntity) - timeCharged;
        if (useTime < holdTime()) {
            storeCookTime(stack, useTime);
            useFromStack(level, player, stack);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            storeCookTime(stack, getUseDuration(stack, livingEntity));
            useFromStack(level, player, stack);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (isSingleUse(stack)) {
            tooltipComponents.add(Component.translatable("item.splatcraft.tooltip.single_use"));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public static boolean isSingleUse(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(SINGLE_USE_KEY);
    }

    public static ItemStack setSingleUse(ItemStack stack) {
        stack.set(DataComponents.MAX_STACK_SIZE, 16);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(SINGLE_USE_KEY, true);
        });
        return stack;
    }

    public static ItemStack setSingleUse(ItemStack stack, int color, boolean colorLocked) {
        setSingleUse(stack);
        InkColorComponent.setColorAndLock(stack, color, colorLocked);
        return stack;
    }

    public static Optional<Integer> color(ItemStack stack) {
        OptionalInt color = InkColorComponent.color(stack);
        return color.isPresent() ? Optional.of(color.getAsInt()) : Optional.empty();
    }

    public int holdTime() {
        return intSetting("hold_time", USE_DURATION);
    }

    private int recoveryCooldown(ItemStack stack) {
        Optional<Integer> cookTime = nestedIntSetting("curling", "cook_time");
        if (cookTime.isEmpty()) {
            return Math.max(1, settings().stats().inkRecoveryCooldown().orElse(10));
        }

        int maxCookTime = Math.max(1, cookTime.get());
        int cookedTicks = customData(stack).getCompound(ENTITY_DATA_KEY).getInt(COOK_TIME_KEY);
        float recoveryCooldown = 70.0F / 3.0F - cookedTicks / (float) maxCookTime * 40.0F / 3.0F;
        return Math.max(1, (int) recoveryCooldown);
    }

    private void storeCookTime(ItemStack stack, int useTime) {
        Optional<Integer> cookTime = nestedIntSetting("curling", "cook_time");
        if (cookTime.isEmpty()) {
            return;
        }

        int value = Math.max(0, Math.min(useTime, cookTime.get()));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag entityData = tag.contains(ENTITY_DATA_KEY, CompoundTag.TAG_COMPOUND)
                    ? tag.getCompound(ENTITY_DATA_KEY)
                    : new CompoundTag();
            entityData.putInt(COOK_TIME_KEY, value);
            tag.put(ENTITY_DATA_KEY, entityData);
        });
    }

    private void clearCookTime(ItemStack stack) {
        if (nestedIntSetting("curling", "cook_time").isEmpty()) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(ENTITY_DATA_KEY));
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public int intSetting(String name, int fallback) {
        return rawNumber(name).map(Number::intValue).orElse(fallback);
    }

    public float floatSetting(String name, float fallback) {
        return rawNumber(name).map(Number::floatValue).orElse(fallback);
    }

    private Optional<Integer> nestedIntSetting(String objectName, String name) {
        JsonObject object = rawObject(objectName);
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? Optional.of(value.getAsNumber().intValue())
                : Optional.empty();
    }

    private Optional<Number> rawNumber(String name) {
        JsonObject raw = settings().raw();
        JsonElement value = raw.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? Optional.of(value.getAsNumber())
                : Optional.empty();
    }

    private JsonObject rawObject(String name) {
        JsonObject raw = settings().raw();
        JsonElement value = raw.get(name);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    public static class DispenseBehavior extends DefaultDispenseItemBehavior {
        private static final float POWER = 0.7F;
        private static final float UNCERTAINTY = 0.0F;

        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            if (!(stack.getItem() instanceof SubWeaponItem<?> subWeapon) || !isSingleUse(stack)) {
                return super.execute(source, stack);
            }

            ItemStack thrownStack = stack.copyWithCount(1);
            Level level = source.level();
            Position position = DispenserBlock.getDispensePosition(source);
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            AbstractSubWeaponEntity projectile = AbstractSubWeaponEntity.create(
                    subWeapon.entityType(),
                    level,
                    position.x(),
                    position.y(),
                    position.z(),
                    thrownStack);
            projectile.shoot(direction.getStepX(), direction.getStepY() + 0.1F, direction.getStepZ(), POWER, UNCERTAINTY);
            level.addFreshEntity(projectile);
            stack.shrink(1);

            Vec3 center = source.center();
            level.playSound(null, center.x(), center.y(), center.z(), SplatcraftSounds.SUB_THROW.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            return stack;
        }
    }
}
