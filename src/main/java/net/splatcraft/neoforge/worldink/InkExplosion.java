package net.splatcraft.neoforge.worldink;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class InkExplosion {
    private InkExplosion() {
    }

    public static int create(
            Level level,
            @Nullable Entity source,
            @Nullable Entity directSource,
            Vec3 center,
            float radius,
            int color,
            net.minecraft.resources.ResourceLocation inkType,
            float blockDamage,
            float minDamage,
            float maxDamage,
            boolean fullDamageToMobs) {
        return create(level, source, directSource, center, radius, color, inkType,
                blockDamage, minDamage, maxDamage, fullDamageToMobs, null);
    }

    /**
     * @param excludedFromDamage entity already damaged by a direct hit this tick; excluded from the
     *                           splash pass so a projectile can't stack direct + explosion damage on
     *                           the same target in one tick.
     */
    public static int create(
            Level level,
            @Nullable Entity source,
            @Nullable Entity directSource,
            Vec3 center,
            float radius,
            int color,
            net.minecraft.resources.ResourceLocation inkType,
            float blockDamage,
            float minDamage,
            float maxDamage,
            boolean fullDamageToMobs,
            @Nullable Entity excludedFromDamage) {
        if (level.isClientSide || radius <= 0.0F) {
            return 0;
        }

        Set<BlockPos> affected = collectAffectedBlocks(level, center, radius);
        int count = 0;
        for (BlockPos pos : affected) {
            InkBlockUtils.InkResult result = source instanceof Player player
                    ? InkBlockUtils.playerInkBlock(player, level, pos, color, blockDamage, inkType)
                    : InkBlockUtils.inkBlock(level, pos, color, blockDamage, inkType);
            if (result == InkBlockUtils.InkResult.SUCCESS) {
                count++;
            }
        }
        damageEntities(level, source, directSource, excludedFromDamage, center, radius, color, minDamage, maxDamage, fullDamageToMobs);
        return count;
    }

    /**
     * Casts rays from the center to a 16x16x16 shell of directions, like vanilla explosions and
     * the original 1.18 implementation. Only the first collider hit by each ray can be inked, so
     * walls block ink and fully enclosed blocks are never painted. A plain sphere sweep here
     * previously inked through walls and flooded chunk storage with invisible entries.
     */
    private static Set<BlockPos> collectAffectedBlocks(Level level, Vec3 center, float radius) {
        Set<BlockPos> affected = new HashSet<>();
        for (int j = 0; j < 16; j++) {
            for (int k = 0; k < 16; k++) {
                for (int l = 0; l < 16; l++) {
                    if (j != 0 && j != 15 && k != 0 && k != 15 && l != 0 && l != 15) {
                        continue;
                    }

                    double dirX = j / 15.0D * 2.0D - 1.0D;
                    double dirY = k / 15.0D * 2.0D - 1.0D;
                    double dirZ = l / 15.0D * 2.0D - 1.0D;
                    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                    float strength = radius * (0.7F + level.getRandom().nextFloat() * 0.6F);
                    Vec3 end = center.add(
                            dirX / length * strength,
                            dirY / length * strength,
                            dirZ / length * strength);

                    BlockHitResult hit = level.clip(new ClipContext(
                            center, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
                    if (hit.getType() != HitResult.Type.MISS
                            && InkBlockUtils.canInkFromFace(level, hit.getBlockPos(), hit.getDirection())) {
                        affected.add(hit.getBlockPos());
                    }
                }
            }
        }
        return affected;
    }

    private static void damageEntities(
            Level level,
            @Nullable Entity source,
            @Nullable Entity directSource,
            @Nullable Entity excludedFromDamage,
            Vec3 center,
            float radius,
            int color,
            float minDamage,
            float maxDamage,
            boolean fullDamageToMobs
    ) {
        float damageRadius = radius * 1.2F;
        if (damageRadius <= 0.0F || maxDamage <= 0.0F) {
            return;
        }

        AABB bounds = new AABB(
                center.x - damageRadius,
                center.y - damageRadius,
                center.z - damageRadius,
                center.x + damageRadius,
                center.y + damageRadius,
                center.z + damageRadius
        );
        double damageRadiusSqr = damageRadius * damageRadius;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds, target -> canHitTarget(target, source, directSource, excludedFromDamage))) {
            double distanceSqr = target.distanceToSqr(center);
            if (distanceSqr > damageRadiusSqr) {
                continue;
            }

            float distanceScale = Math.max(0.0F, (float) ((damageRadiusSqr - distanceSqr) / damageRadiusSqr));
            float seenScale = Explosion.getSeenPercent(center, target);
            float damage = Mth.lerp(distanceScale, minDamage, maxDamage) * seenScale;
            InkDamageUtils.doSplatDamage(level, target, damage, color, source, directSource, fullDamageToMobs);
        }
    }

    private static boolean canHitTarget(LivingEntity target, @Nullable Entity source, @Nullable Entity directSource, @Nullable Entity excludedFromDamage) {
        return target.isAlive()
                && !target.isSpectator()
                && target != source
                && target != directSource
                && target != excludedFromDamage;
    }
}
