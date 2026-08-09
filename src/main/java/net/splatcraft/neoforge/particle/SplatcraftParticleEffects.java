package net.splatcraft.neoforge.particle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    public static void inkDestroy(ServerLevel level, BlockPos pos, int color) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }

        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            int xSteps = Math.max(2, Mth.ceil(Math.min(1.0D, maxX - minX) / 0.25D));
            int ySteps = Math.max(2, Mth.ceil(Math.min(1.0D, maxY - minY) / 0.25D));
            int zSteps = Math.max(2, Mth.ceil(Math.min(1.0D, maxZ - minZ) / 0.25D));
            for (int x = 0; x < xSteps; x++) {
                for (int y = 0; y < ySteps; y++) {
                    for (int z = 0; z < zSteps; z++) {
                        double xProgress = ((double) x + 0.5D) / xSteps;
                        double yProgress = ((double) y + 0.5D) / ySteps;
                        double zProgress = ((double) z + 0.5D) / zSteps;
                        level.sendParticles(
                                new InkTerrainParticleOptions(color),
                                pos.getX() + minX + xProgress * (maxX - minX),
                                pos.getY() + minY + yProgress * (maxY - minY),
                                pos.getZ() + minZ + zProgress * (maxZ - minZ),
                                1,
                                xProgress - 0.5D,
                                yProgress - 0.5D,
                                zProgress - 0.5D,
                                1.0D);
                    }
                }
            }
        });
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
