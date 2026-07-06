package net.splatcraft.neoforge.particle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class SplatcraftParticleEffects {
    private SplatcraftParticleEffects() {
    }

    public static void inkExplosion(ServerLevel level, Vec3 center, float radius, int color) {
        level.sendParticles(
                new InkExplosionParticleOptions(color, Math.max(0.5F, radius * 2.0F)),
                center.x,
                center.y,
                center.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
        inkTerrainBurst(level, center, radius, color);
    }

    private static void inkTerrainBurst(ServerLevel level, Vec3 center, float radius, int color) {
        RandomSource random = level.getRandom();
        int count = Mth.clamp(Math.round(radius * 8.0F), 4, 24);
        for (int i = 0; i < count; i++) {
            double xOffset = (random.nextDouble() - 0.5D) * radius;
            double yOffset = (random.nextDouble() - 0.5D) * radius * 0.5D;
            double zOffset = (random.nextDouble() - 0.5D) * radius;
            level.sendParticles(
                    new InkTerrainParticleOptions(color),
                    center.x + xOffset,
                    center.y + yOffset,
                    center.z + zOffset,
                    1,
                    xOffset * 0.08D,
                    0.08D + random.nextDouble() * 0.12D,
                    zOffset * 0.08D,
                    0.15D);
        }
    }
}
