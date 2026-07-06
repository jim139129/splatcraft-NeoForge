package net.splatcraft.neoforge.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.blockentity.CrateBlockEntity;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;

public class CrateFeature extends Feature<CountConfiguration> {
    private static final ResourceLocation STORAGE_EGG_CRATE = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "storage/egg_crate");
    private static final int AREA = 8;

    public CrateFeature(Codec<CountConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CountConfiguration> context) {
        int placed = 0;
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int attempts = context.config().count().sample(random);

        for (int i = 0; i < attempts; i++) {
            int xOffset = random.nextInt(AREA) - random.nextInt(AREA);
            int zOffset = random.nextInt(AREA) - random.nextInt(AREA);
            BlockPos pos = oceanFloor(level, origin.getX() + xOffset, origin.getZ() + zOffset);

            boolean sunken = random.nextFloat() <= 0.05F;
            BlockState state = sunken
                    ? SplatcraftBlocks.SUNKEN_CRATE.get().defaultBlockState()
                    : SplatcraftBlocks.CRATE.get().defaultBlockState();
            level.setBlock(pos, state, 2);

            if (!sunken && level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
                crate.setLootTable(STORAGE_EGG_CRATE);
            }
            placed++;
        }

        if (random.nextFloat() <= 0.0125F * attempts) {
            int xOffset = random.nextInt(AREA) - random.nextInt(AREA);
            int zOffset = random.nextInt(AREA) - random.nextInt(AREA);
            BlockPos pos = oceanFloor(level, origin.getX() + xOffset, origin.getZ() + zOffset);
            BlockState state = SplatcraftBlocks.AMMO_KNIGHTS_DEBRIS.get()
                    .defaultBlockState()
                    .setValue(BlockStateCompatBlocks.HorizontalFacingBlock.FACING, Direction.from2DDataValue(random.nextInt(4)))
                    .setValue(BlockStateCompatBlocks.WATERLOGGED, level.getFluidState(pos).is(FluidTags.WATER));

            if (state.canSurvive(level, pos)) {
                level.setBlock(pos, state, 2);
                placed++;
            }
        }

        return placed > 0;
    }

    private static BlockPos oceanFloor(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
    }
}
