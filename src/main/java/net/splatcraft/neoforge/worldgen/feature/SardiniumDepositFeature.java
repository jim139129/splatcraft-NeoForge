package net.splatcraft.neoforge.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;

public class SardiniumDepositFeature extends Feature<NoneFeatureConfiguration> {
    public SardiniumDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos center = new BlockPos(
                origin.getX(),
                level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ()),
                origin.getZ());

        boolean ellipse = random.nextDouble() > 0.7D;
        double rotation = random.nextDouble() * 2.0D * Math.PI;
        int upperRadius = 11 - random.nextInt(5);
        int ellipseHeightBias = 3 + random.nextInt(3);
        boolean steep = random.nextDouble() > 0.7D;
        int height = steep ? random.nextInt(6) + 6 : random.nextInt(15) + 3;
        if (!steep && random.nextDouble() > 0.9D) {
            height += random.nextInt(19) + 7;
        }

        int lowerHeight = Math.min(height + random.nextInt(11), 18);
        int lowerRadius = Math.min(height + random.nextInt(7) - random.nextInt(5), 11);
        int horizontalRadius = steep ? upperRadius : 11;
        boolean placed = false;

        for (int x = -horizontalRadius; x < horizontalRadius; x++) {
            for (int z = -horizontalRadius; z < horizontalRadius; z++) {
                for (int y = 0; y < height; y++) {
                    int radius = steep
                            ? heightDependentRadiusEllipse(y, height, lowerRadius)
                            : heightDependentRadiusRound(random, y, height, lowerRadius);
                    if (steep || x < radius) {
                        BlockState state = random.nextFloat() < 0.2F
                                ? SplatcraftBlocks.SARDINIUM_ORE.get().defaultBlockState()
                                : SplatcraftBlocks.CORALITE.get().defaultBlockState();
                        placed |= generateDepositBlock(level, random, center, height, x, y, z, radius, horizontalRadius, steep, ellipseHeightBias, rotation, state);
                    }
                }
            }
        }

        for (int x = -horizontalRadius; x < horizontalRadius; x++) {
            for (int z = -horizontalRadius; z < horizontalRadius; z++) {
                for (int y = -1; y > -lowerHeight; y--) {
                    int ellipseRadius = steep
                            ? Mth.ceil((float) horizontalRadius * (1.0F - (float) Math.pow(y, 2.0D) / ((float) lowerHeight * 8.0F)))
                            : horizontalRadius;
                    int radius = heightDependentRadiusSteep(random, -y, lowerHeight, lowerRadius);
                    if (x < radius) {
                        BlockState state = lowerDepositState(random);
                        placed |= generateDepositBlock(level, random, center, lowerHeight, x, y, z, radius, ellipseRadius, steep, ellipseHeightBias, rotation, state);
                    }
                }
            }
        }

        return placed;
    }

    private static BlockState lowerDepositState(RandomSource random) {
        if (random.nextFloat() < 0.05F) {
            return SplatcraftBlocks.RAW_SARDINIUM_BLOCK.get().defaultBlockState();
        }
        return random.nextFloat() < 0.3F
                ? SplatcraftBlocks.SARDINIUM_ORE.get().defaultBlockState()
                : SplatcraftBlocks.CORALITE.get().defaultBlockState();
    }

    private boolean generateDepositBlock(
            WorldGenLevel level,
            RandomSource random,
            BlockPos center,
            int height,
            int x,
            int y,
            int z,
            int radius,
            int horizontalRadius,
            boolean ellipse,
            int ellipseHeightBias,
            double rotation,
            BlockState state
    ) {
        double distance = ellipse
                ? signedDistanceEllipse(x, z, horizontalRadius, getEllipseC(y, height, ellipseHeightBias), rotation)
                : signedDistanceCircle(x, z, radius, random);
        if (distance >= 0.0D) {
            return false;
        }

        BlockPos pos = center.offset(x, y, z);
        double threshold = ellipse ? -0.5D : -6 - random.nextInt(3);
        if (distance > threshold && random.nextDouble() > 0.9D) {
            return false;
        }

        level.setBlock(pos, state, 2);
        return true;
    }

    private static int getEllipseC(int y, int height, int bias) {
        if (y > 0 && height - y <= 3) {
            return bias - (4 - (height - y));
        }
        return bias;
    }

    private static double signedDistanceCircle(int x, int z, int radius, RandomSource random) {
        float noise = 10.0F * Mth.clamp(random.nextFloat(), 0.2F, 0.8F) / (float) radius;
        return noise + Math.pow(x, 2.0D) + Math.pow(z, 2.0D) - Math.pow(radius, 2.0D);
    }

    private static double signedDistanceEllipse(int x, int z, int radiusX, int radiusZ, double rotation) {
        return Math.pow(((double) x * Math.cos(rotation) - (double) z * Math.sin(rotation)) / (double) radiusX, 2.0D)
                + Math.pow(((double) x * Math.sin(rotation) + (double) z * Math.cos(rotation)) / (double) radiusZ, 2.0D)
                - 1.0D;
    }

    private static int heightDependentRadiusSteep(RandomSource random, int y, int height, int radius) {
        float scale = 1.0F + random.nextFloat() / 2.0F;
        float value = (1.0F - (float) y / ((float) height * scale)) * (float) radius;
        return Mth.ceil(value / 2.0F);
    }

    private static int heightDependentRadiusRound(RandomSource random, int y, int height, int radius) {
        float scale = 3.5F - random.nextFloat();
        float value = (1.0F - (float) Math.pow(y, 2.0D) / ((float) height * scale)) * (float) radius;
        if (height > 15 + random.nextInt(5)) {
            int adjustedY = y < 3 + random.nextInt(6) ? y / 2 : y;
            value = (1.0F - (float) adjustedY / ((float) height * scale * 0.4F)) * (float) radius;
        }

        return Mth.ceil(value / 2.0F);
    }

    private static int heightDependentRadiusEllipse(int y, int height, int radius) {
        float value = (1.0F - (float) Math.pow(y, 2.0D) / (float) height) * (float) radius;
        return Mth.ceil(value / 2.0F);
    }
}
