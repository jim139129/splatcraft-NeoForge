package net.splatcraft.neoforge.player;

import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.entity.SplatcraftEntityState;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.worldink.WorldInkStorage;

public final class SquidInkMovement {
    private SquidInkMovement() {
    }

    public static boolean canSquidHide(LivingEntity entity) {
        return !entity.isSpectator()
                && ((entity.onGround() || !entity.level().getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 0.1D, entity.getZ())).is(Blocks.AIR))
                && canSquidSwim(entity)
                || canSquidClimb(entity));
    }

    public static boolean canSquidSwim(LivingEntity entity) {
        BlockPos pos = blockStandingOn(entity);
        Level level = entity.level();
        int entityColor = SplatcraftEntityState.color(entity);
        if (entityColor < 0) {
            return false;
        }

        InkSurface worldInk = worldInkSurface(level, pos);
        if (worldInk != null) {
            return worldInk.canSwim() && sameColor(level, pos, entityColor, worldInk.color());
        }

        InkSurface coloredBlock = coloredBlockSurface(level, pos);
        return coloredBlock != null && coloredBlock.canSwim() && sameColor(level, pos, entityColor, coloredBlock.color());
    }

    public static boolean onEnemyInk(LivingEntity entity) {
        if (!entity.onGround()) {
            return false;
        }

        BlockPos pos = blockStandingOn(entity);
        Level level = entity.level();
        int entityColor = SplatcraftEntityState.color(entity);
        InkSurface surface = worldInkSurface(level, pos);
        if (surface == null) {
            surface = coloredBlockSurface(level, pos);
        }
        return surface != null
                && surface.canDamage()
                && (entityColor < 0 || !sameColor(level, pos, entityColor, surface.color()));
    }

    public static boolean canSquidClimb(LivingEntity entity) {
        if (entity.isPassenger() || onEnemyInk(entity)) {
            return false;
        }

        Level level = entity.level();
        int entityColor = SplatcraftEntityState.color(entity);
        if (entityColor < 0) {
            return false;
        }

        BlockPos standingPos = blockStandingOn(entity);
        double localY = entity.getY() - entity.blockPosition().getY();
        for (int i = 0; i < 4; i++) {
            double xOffset = (i < 2 ? 0.32D : 0.0D) * (i % 2 == 0 ? 1.0D : -1.0D);
            double zOffset = (i < 2 ? 0.0D : 0.32D) * (i % 2 == 0 ? 1.0D : -1.0D);
            BlockPos pos = BlockPos.containing(entity.getX() - xOffset, entity.getY(), entity.getZ() - zOffset);
            if (pos.equals(standingPos)) {
                continue;
            }

            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.of(entity));
            if (!shape.isEmpty() && (shape.bounds().maxY < localY || shape.bounds().minY > localY)) {
                continue;
            }

            InkSurface worldInk = worldInkSurface(level, pos);
            if (worldInk != null && worldInk.canClimb() && sameColor(level, pos, entityColor, worldInk.color())) {
                return true;
            }

            InkSurface coloredBlock = coloredBlockSurface(level, pos);
            if (coloredBlock != null && coloredBlock.canClimb() && sameColor(level, pos, entityColor, coloredBlock.color())) {
                return true;
            }
        }
        return false;
    }

    private static InkSurface worldInkSurface(Level level, BlockPos pos) {
        return WorldInkStorage.getInk(level, pos)
                .map(ink -> new InkSurface(ink.color(), true, true, true))
                .orElse(null);
    }

    public static OptionalInt inkSurfaceColor(Level level, BlockPos pos) {
        InkSurface surface = worldInkSurface(level, pos);
        if (surface == null) {
            surface = coloredBlockSurface(level, pos);
        }
        return surface == null ? OptionalInt.empty() : OptionalInt.of(surface.color());
    }

    private static InkSurface coloredBlockSurface(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(SplatcraftTags.Blocks.INKED_BLOCKS)) {
            return inkColorSurface(level, pos, true, true, true);
        }

        if (state.is(SplatcraftBlocks.CANVAS.get())) {
            if (!state.hasProperty(BlockStateCompatBlocks.INKED) || !state.getValue(BlockStateCompatBlocks.INKED)) {
                return null;
            }
            return inkColorSurface(level, pos, true, true, false);
        }

        if (state.is(SplatcraftBlocks.SPAWN_PAD.get())) {
            return inkColorSurface(level, pos, true, false, false);
        }

        return null;
    }

    private static InkSurface inkColorSurface(
            Level level,
            BlockPos pos,
            boolean canSwim,
            boolean canClimb,
            boolean canDamage) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof InkColorBlockEntity inkColor)) {
            return null;
        }

        int color = inkColor.effectiveColor();
        return color < 0 ? null : new InkSurface(color, canSwim, canClimb, canDamage);
    }

    public static BlockPos blockStandingOn(Entity entity) {
        Level level = entity.level();
        for (double offset = 0.0D; offset >= -0.5D; offset -= 0.1D) {
            BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() + offset, entity.getZ());
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.of(entity));
            if (!shape.isEmpty() && shape.bounds().minY <= entity.getY() - pos.getY()) {
                return pos;
            }
        }
        return BlockPos.containing(entity.getX(), entity.getY() - 0.6D, entity.getZ());
    }

    private static boolean sameColor(Level level, BlockPos pos, int left, int right) {
        return SplatcraftGameRules.localizedBoolean(level, pos, SplatcraftGameRules.UNIVERSAL_INK)
                || (left & 0xFFFFFF) == (right & 0xFFFFFF);
    }

    private record InkSurface(int color, boolean canSwim, boolean canClimb, boolean canDamage) {
    }
}
