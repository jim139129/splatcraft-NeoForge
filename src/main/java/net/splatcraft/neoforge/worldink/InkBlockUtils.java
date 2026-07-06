package net.splatcraft.neoforge.worldink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.blockentity.CrateBlockEntity;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.registry.SplatcraftStats;

public final class InkBlockUtils {
    private InkBlockUtils() {
    }

    public static InkResult playerInkBlock(Player player, Level level, BlockPos pos, int color, net.minecraft.resources.ResourceLocation inkType) {
        return playerInkBlock(player, level, pos, color, 0.0F, inkType);
    }

    public static InkResult playerInkBlock(Player player, Level level, BlockPos pos, int color, float blockDamage, net.minecraft.resources.ResourceLocation inkType) {
        InkResult result = inkBlock(level, pos, color, blockDamage, inkType);
        if (result == InkResult.SUCCESS && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(Stats.CUSTOM.get(SplatcraftStats.BLOCKS_INKED));
        }
        return result;
    }

    public static InkResult inkBlock(Level level, BlockPos pos, int color, net.minecraft.resources.ResourceLocation inkType) {
        return inkBlock(level, pos, color, 0.0F, inkType);
    }

    public static InkResult inkBlock(Level level, BlockPos pos, int color, float blockDamage, net.minecraft.resources.ResourceLocation inkType) {
        if (level.isClientSide) {
            return InkResult.FAIL;
        }
        if (blockedBySpawnShield(level, pos, color)) {
            return InkResult.FAIL;
        }
        if (level.getBlockState(pos).getBlock() instanceof BlockStateCompatBlocks.SplatSwitchBlockEntityBlock) {
            return BlockStateCompatBlocks.SplatSwitchBlockEntityBlock.activate(level, pos, color);
        }
        if (level.getBlockState(pos).getBlock() instanceof BlockStateCompatBlocks.CanvasBlock) {
            return BlockStateCompatBlocks.CanvasBlock.ink(level, pos, color);
        }
        if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            crate.ink(color, blockDamage);
            return InkResult.FAIL;
        }
        if (isUninkable(level, pos)) {
            return InkResult.FAIL;
        }
        if (!SplatcraftGameRules.localizedBoolean(level, pos, SplatcraftGameRules.INKABLE_GROUND)) {
            return InkResult.FAIL;
        }

        WorldInk.Entry current = WorldInkStorage.getInk(level, pos).orElse(null);
        if (current != null && current.color() == color && current.type().equals(inkType)) {
            return InkResult.ALREADY_INKED;
        }

        boolean sameColor = current != null && current.color() == color;
        WorldInkStorage.ink(level, pos, color, inkType);
        destroyFoliageAbove(level, pos);
        sendBlockUpdate(level, pos);
        return sameColor ? InkResult.ALREADY_INKED : InkResult.SUCCESS;
    }

    public static boolean clearInk(Level level, BlockPos pos) {
        return clearInk(level, pos, false);
    }

    public static boolean clearInk(Level level, BlockPos pos, boolean removePermanent) {
        if (!level.isClientSide && level.getBlockState(pos).getBlock() instanceof BlockStateCompatBlocks.SplatSwitchBlockEntityBlock) {
            return BlockStateCompatBlocks.SplatSwitchBlockEntityBlock.deactivate(level, pos);
        }
        if (!level.isClientSide && level.getBlockState(pos).getBlock() instanceof BlockStateCompatBlocks.CanvasBlock) {
            return BlockStateCompatBlocks.CanvasBlock.clear(level, pos);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            if (!crate.isDamaged()) {
                return false;
            }
            crate.resetHealth();
            return true;
        }
        boolean changed = WorldInkStorage.clearInk(level, pos, removePermanent);
        if (changed) {
            sendBlockUpdate(level, pos);
        }
        return changed;
    }

    public static boolean canInkFromFace(Level level, BlockPos pos, Direction face) {
        if (isUninkable(level, pos)) {
            return false;
        }
        BlockPos adjacent = pos.relative(face);
        return canInkPassthrough(level, adjacent) || !level.getBlockState(adjacent).is(SplatcraftTags.Blocks.DETERS_INK);
    }

    public static boolean isUninkable(Level level, BlockPos pos) {
        if (!level.isInWorldBounds(pos)) {
            return true;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.liquid() || state.is(SplatcraftTags.Blocks.INKPROOF)) {
            return true;
        }
        if (isTouchingInkClearingFluid(level, pos)) {
            return true;
        }
        if (!state.is(SplatcraftTags.Blocks.RENDER_INK_AS_CUBE) && state.getRenderShape() != RenderShape.MODEL) {
            return true;
        }
        return canInkPassthrough(level, pos);
    }

    public static boolean isTouchingInkClearingFluid(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().is(FluidTags.WATER)) {
            return true;
        }

        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        for (Direction direction : Direction.values()) {
            mutablePos.setWithOffset(pos, direction);
            BlockState neighborState = level.getBlockState(mutablePos);
            if (neighborState.is(SplatcraftTags.Blocks.CLEARS_INK)) {
                return true;
            }
            if (direction != Direction.DOWN
                    && neighborState.getFluidState().is(FluidTags.WATER)
                    && !neighborState.isFaceSturdy(level, mutablePos, direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    public static boolean canInkPassthrough(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() || state.is(SplatcraftTags.Blocks.INK_PASSTHROUGH);
    }

    private static void sendBlockUpdate(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 2);
    }

    private static void destroyFoliageAbove(Level level, BlockPos pos) {
        BlockPos foliagePos = pos.above();
        if (SplatcraftGameRules.localizedBoolean(level, foliagePos, SplatcraftGameRules.INK_DESTROYS_FOLIAGE)
                && isBlockFoliage(level.getBlockState(foliagePos))) {
            level.destroyBlock(foliagePos, true);
        }
    }

    public static boolean isBlockFoliage(BlockState state) {
        return state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || (state.canBeReplaced() && !state.isAir() && !state.liquid() && !state.is(BlockTags.SNOW) && !state.is(BlockTags.FIRE));
    }

    private static boolean blockedBySpawnShield(Level level, BlockPos pos, int color) {
        for (SpawnShieldEntity shield : level.getEntitiesOfClass(SpawnShieldEntity.class, new AABB(pos))) {
            if (!InkDamageUtils.sameColor(level, pos, shield.color(), color)) {
                return true;
            }
        }
        return false;
    }

    public enum InkResult {
        SUCCESS,
        FAIL,
        ALREADY_INKED
    }
}
