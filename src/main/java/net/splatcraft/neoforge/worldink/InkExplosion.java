package net.splatcraft.neoforge.worldink;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
        damageEntities(level, source, directSource, center, radius, color, minDamage, maxDamage, fullDamageToMobs);
        return count;
    }

    private static Set<BlockPos> collectAffectedBlocks(Level level, Vec3 center, float radius) {
        Set<BlockPos> affected = new HashSet<>();
        int minX = Mth.floor(center.x - radius);
        int maxX = Mth.floor(center.x + radius);
        int minY = Mth.floor(center.y - radius);
        int maxY = Mth.floor(center.y + radius);
        int minZ = Mth.floor(center.z - radius);
        int maxZ = Mth.floor(center.z + radius);
        double radiusSqr = radius * radius;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (center.distanceToSqr(Vec3.atCenterOf(pos)) > radiusSqr) {
                        continue;
                    }
                    if (canReachInkableFace(level, pos)) {
                        affected.add(pos);
                    }
                }
            }
        }
        return affected;
    }

    private static boolean canReachInkableFace(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (InkBlockUtils.canInkFromFace(level, pos, direction)) {
                return true;
            }
        }
        return false;
    }

    private static void damageEntities(
            Level level,
            @Nullable Entity source,
            @Nullable Entity directSource,
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
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds, target -> canHitTarget(target, source, directSource))) {
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

    private static boolean canHitTarget(LivingEntity target, @Nullable Entity source, @Nullable Entity directSource) {
        return target.isAlive()
                && !target.isSpectator()
                && target != source
                && target != directSource;
    }
}
