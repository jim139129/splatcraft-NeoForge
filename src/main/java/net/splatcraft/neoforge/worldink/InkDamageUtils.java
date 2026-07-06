package net.splatcraft.neoforge.worldink;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.data.InkOverlayInfo;
import net.splatcraft.neoforge.entity.ColoredEntity;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;
import net.splatcraft.neoforge.registry.SplatcraftDamageTypes;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;

public final class InkDamageUtils {
    private InkDamageUtils() {
    }

    public static boolean doSplatDamage(
            Level level,
            LivingEntity target,
            float damage,
            int color,
            @Nullable Entity source,
            @Nullable Entity directSource,
            boolean fullDamageToMobs
    ) {
        return doInkDamage(level, target, damage, color, source, directSource, fullDamageToMobs, SplatcraftDamageTypes.SPLAT, false);
    }

    public static boolean doRollDamage(
            Level level,
            LivingEntity target,
            float damage,
            int color,
            @Nullable Entity source,
            @Nullable Entity directSource,
            boolean fullDamageToMobs
    ) {
        return doInkDamage(level, target, damage, color, source, directSource, fullDamageToMobs, SplatcraftDamageTypes.ROLL, true);
    }

    private static boolean doInkDamage(
            Level level,
            LivingEntity target,
            float damage,
            int color,
            @Nullable Entity source,
            @Nullable Entity directSource,
            boolean fullDamageToMobs,
            ResourceKey<DamageType> damageType,
            boolean applyHurtCooldown
    ) {
        if (level.isClientSide || damage <= 0.0F || target.isRemoved()) {
            return false;
        }

        BlockPos pos = target.blockPosition();
        InkOverlayInfo overlay = target.getData(SplatcraftAttachments.INK_OVERLAY.get());
        if (overlay.inkproof()) {
            return false;
        }

        int targetColor = targetColor(target);
        if (targetColor >= 0 && !canDamageColor(level, pos, targetColor, color)) {
            return false;
        }
        if (targetColor >= 0 && protectedBySpawnShield(level, pos, target, targetColor)) {
            return false;
        }
        if (target instanceof Sheep sheep && !sheep.isSheared()) {
            overlay.setWoolColor(color);
            syncInkOverlay(target);
            return false;
        }

        float mobDamageScale = Math.max(0, SplatcraftGameRules.localizedInt(level, pos, SplatcraftGameRules.INK_MOB_DAMAGE_PERCENTAGE)) * 0.01F;
        float effectiveDamage = damage;
        if (!(target instanceof Player) && targetColor < 0 && !fullDamageToMobs) {
            effectiveDamage *= mobDamageScale;
        }

        applyInkOverlay(target, overlay, color, damage, mobDamageScale, fullDamageToMobs);
        if (effectiveDamage <= 0.0F) {
            return false;
        }

        boolean bypassDamageCooldown = !applyHurtCooldown
                && !SplatcraftGameRules.localizedBoolean(level, pos, SplatcraftGameRules.INK_DAMAGE_COOLDOWN);
        if (bypassDamageCooldown) {
            target.invulnerableTime = 0;
        }

        Vec3 deltaMovement = target.getDeltaMovement();
        boolean damaged = target.hurt(damageSource(target, source, directSource, damageType), effectiveDamage);
        if (damaged) {
            target.setDeltaMovement(deltaMovement);
            target.hurtMarked = false;
            if (bypassDamageCooldown) {
                target.hurtTime = 1;
            }
        }
        return damaged;
    }

    public static boolean canDamageColor(Level level, BlockPos pos, int targetColor, int sourceColor) {
        return SplatcraftGameRules.localizedBoolean(level, pos, SplatcraftGameRules.INK_FRIENDLY_FIRE)
                || !sameColor(level, pos, targetColor, sourceColor);
    }

    private static int targetColor(LivingEntity target) {
        if (target instanceof Player player) {
            return SplatcraftPlayerInfoEvents.color(player);
        }
        return target instanceof ColoredEntity colored ? colored.splatcraftColor() : -1;
    }

    private static void applyInkOverlay(
            LivingEntity target,
            InkOverlayInfo overlay,
            int color,
            float damage,
            float mobDamageScale,
            boolean fullDamageToMobs
    ) {
        if (target.isInWater()) {
            return;
        }

        float overlayScale = target instanceof ColoredEntity || fullDamageToMobs ? 1.0F : Math.max(0.5F, mobDamageScale);
        if (overlay.amount() < target.getMaxHealth() * 1.5F) {
            overlay.addAmount(damage * overlayScale);
        }
        overlay.setColor(color);
        syncInkOverlay(target);
    }

    private static boolean protectedBySpawnShield(Level level, BlockPos pos, LivingEntity target, int targetColor) {
        for (SpawnShieldEntity shield : level.getEntitiesOfClass(SpawnShieldEntity.class, target.getBoundingBox())) {
            if (sameColor(level, pos, shield.color(), targetColor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sameColor(Level level, BlockPos pos, int left, int right) {
        return SplatcraftGameRules.localizedBoolean(level, pos, SplatcraftGameRules.UNIVERSAL_INK)
                || (left & 0xFFFFFF) == (right & 0xFFFFFF);
    }

    private static void syncInkOverlay(LivingEntity target) {
        if (!target.level().isClientSide && canSync(target)) {
            target.syncData(SplatcraftAttachments.INK_OVERLAY.get());
        }
    }

    private static boolean canSync(LivingEntity target) {
        return !(target instanceof ServerPlayer player) || player.connection != null;
    }

    private static DamageSource damageSource(
            LivingEntity target,
            @Nullable Entity source,
            @Nullable Entity directSource,
            ResourceKey<DamageType> damageType
    ) {
        Holder<DamageType> holder = target.damageSources().damageTypes.getHolderOrThrow(damageType);
        if (source == null && directSource == null) {
            return new InkDamageSource(holder);
        }

        Entity direct = directSource == null ? source : directSource;
        return new InkDamageSource(holder, direct, source);
    }

    private static final class InkDamageSource extends DamageSource {
        private InkDamageSource(Holder<DamageType> type) {
            super(type);
        }

        private InkDamageSource(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
            super(type, directEntity, causingEntity);
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity target) {
            String base = "death.attack." + getMsgId();
            if (getEntity() == null && getDirectEntity() == null) {
                return Component.translatable(base, target.getDisplayName());
            }

            Entity attacker = getEntity() == null ? getDirectEntity() : getEntity();
            if (attacker == null) {
                return Component.translatable(base, target.getDisplayName());
            }

            Component attackerName = attacker.getDisplayName();
            ItemStack weapon = getEntity() instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
            String playerBase = base + ".player";
            return !weapon.isEmpty()
                    ? Component.translatable(playerBase + ".item", target.getDisplayName(), attackerName, weapon.getDisplayName())
                    : Component.translatable(playerBase, target.getDisplayName(), attackerName);
        }
    }
}
