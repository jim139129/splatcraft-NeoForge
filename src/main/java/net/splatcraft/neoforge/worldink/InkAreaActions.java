package net.splatcraft.neoforge.worldink;

import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.blockentity.CrateBlockEntity;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.blockentity.InkedBlockEntity;
import net.splatcraft.neoforge.blockentity.RemotePedestalBlockEntity;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;

public final class InkAreaActions {
    public static final int SCAN_TOP_DOWN = 0;
    public static final int SCAN_MULTI_LAYERED = 1;

    private InkAreaActions() {
    }

    public static BlockPos min(BlockPos left, BlockPos right) {
        return new BlockPos(
                Math.min(left.getX(), right.getX()),
                Math.min(left.getY(), right.getY()),
                Math.min(left.getZ(), right.getZ()));
    }

    public static BlockPos max(BlockPos left, BlockPos right) {
        return new BlockPos(
                Math.max(left.getX(), right.getX()),
                Math.max(left.getY(), right.getY()),
                Math.max(left.getZ(), right.getZ()));
    }

    public static boolean inWorldBounds(Level level, BlockPos from, BlockPos to) {
        return level.isInWorldBounds(min(from, to)) && level.isInWorldBounds(max(from, to));
    }

    public static int volume(BlockPos from, BlockPos to) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);
        return (max.getX() - min.getX() + 1)
                * (max.getY() - min.getY() + 1)
                * (max.getZ() - min.getZ() + 1);
    }

    public static int clearInk(Level level, BlockPos from, BlockPos to) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);
        int affectedBlocks = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockPos immutablePos = pos.immutable();
            BlockState state = level.getBlockState(immutablePos);
            BlockEntity blockEntity = level.getBlockEntity(immutablePos);
            boolean changed = InkBlockUtils.clearInk(level, immutablePos, false);

            if (blockEntity instanceof InkedBlockEntity inkedBlock && clearInkedBlock(level, immutablePos, state, inkedBlock)) {
                changed = true;
            } else if (state.hasProperty(BlockStateCompatBlocks.INKED)
                    && state.getValue(BlockStateCompatBlocks.INKED)) {
                level.setBlock(immutablePos, state.setValue(BlockStateCompatBlocks.INKED, false), 3);
                changed = true;
            }

            if (changed) {
                affectedBlocks++;
                sendBlockUpdate(level, immutablePos);
            }
        }
        return affectedBlocks;
    }

    public static int visitColorBlocks(Level level, BlockPos from, BlockPos to, InkColorBlockVisitor visitor) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);
        int affectedBlocks = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockPos immutablePos = pos.immutable();
            WorldInk worldInk = WorldInkStorage.getLoaded(level, immutablePos)
                    .filter(loadedWorldInk -> loadedWorldInk.getInk(immutablePos) != null)
                    .orElse(null);
            if (worldInk != null && visitor.visit(new WorldInkColorBlock(level, immutablePos, worldInk))) {
                sendBlockUpdate(level, immutablePos);
                affectedBlocks++;
            }

            BlockEntity blockEntity = colorActionBlockEntity(level, immutablePos);
            if (blockEntity instanceof RemotePedestalBlockEntity pedestal) {
                if (pedestal.hasRecolorableRemote() && visitor.visit(new RemotePedestalColorBlock(pedestal))) {
                    sendBlockUpdate(pedestal);
                    affectedBlocks++;
                }
            } else if (blockEntity instanceof CrateBlockEntity) {
                continue;
            } else if (blockEntity instanceof InkColorBlockEntity colorBlock) {
                if (visitor.visit(new InkColorBlock(colorBlock))) {
                    sendBlockUpdate(colorBlock);
                    affectedBlocks++;
                }
            } else if (blockEntity instanceof ColoredBarrierBlockEntity barrier) {
                if (visitor.visit(new ColoredBarrierColorBlock(barrier))) {
                    sendBlockUpdate(barrier);
                    affectedBlocks++;
                }
            }
        }
        return affectedBlocks;
    }

    private static BlockEntity colorActionBlockEntity(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(SplatcraftBlocks.SPAWN_PAD_EDGE.get())) {
            BlockPos parentPos = BlockStateCompatBlocks.spawnPadParent(level, pos);
            if (parentPos != null) {
                return level.getBlockEntity(parentPos);
            }
        }
        return level.getBlockEntity(pos);
    }

    public static TurfScanResult scanTurf(Level level, BlockPos from, BlockPos to, int mode) {
        return mode == SCAN_MULTI_LAYERED ? scanMultiLayered(level, from, to) : scanTopDown(level, from, to);
    }

    public static OptionalInt inkColorAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        OptionalInt worldInkColor = WorldInkStorage.getInk(level, pos)
                .map(ink -> OptionalInt.of(ink.color()))
                .orElse(OptionalInt.empty());
        if (worldInkColor.isPresent()) {
            return worldInkColor;
        }
        if (blockEntity instanceof InkedBlockEntity inkedBlock) {
            return OptionalInt.of(inkedBlock.getColor());
        }
        if (blockEntity instanceof InkColorBlockEntity colorBlock) {
            return OptionalInt.of(colorBlock.getColor());
        }
        if (blockEntity instanceof ColoredBarrierBlockEntity barrier) {
            return OptionalInt.of(barrier.getColor());
        }
        return OptionalInt.empty();
    }

    private static TurfScanResult scanTopDown(Level level, BlockPos from, BlockPos to) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);
        TreeMap<Integer, Integer> scores = new TreeMap<>();
        int scannedBlocks = 0;

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                BlockPos pos = topScannableBlock(level, x, z, min.getY(), max.getY());
                if (pos == null) {
                    continue;
                }

                scannedBlocks++;
                addScore(scores, turfColorAt(level, pos));
            }
        }

        return new TurfScanResult(scores, scannedBlocks);
    }

    private static TurfScanResult scanMultiLayered(Level level, BlockPos from, BlockPos to) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);
        TreeMap<Integer, Integer> scores = new TreeMap<>();
        int scannedBlocks = 0;

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockPos immutablePos = pos.immutable();
            if (!isScannableTurf(level, immutablePos) || hasCover(level, immutablePos, max.getY())) {
                continue;
            }

            scannedBlocks++;
            addScore(scores, turfColorAt(level, immutablePos));
        }

        return new TurfScanResult(scores, scannedBlocks);
    }

    private static BlockPos topScannableBlock(Level level, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(SplatcraftTags.Blocks.SCAN_TURF_IGNORED)) {
                continue;
            }
            if (!isPassThrough(level, pos) || state.blocksMotion()) {
                return isScannableTurf(level, pos) ? pos : null;
            }
        }
        return null;
    }

    private static boolean hasCover(Level level, BlockPos pos, int maxY) {
        for (int y = pos.getY() + 1; y <= Math.min(pos.getY() + 2, maxY); y++) {
            if (!isPassThrough(level, new BlockPos(pos.getX(), y, pos.getZ()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isScannableTurf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.liquid() || state.is(SplatcraftTags.Blocks.SCAN_TURF_IGNORED)) {
            return false;
        }
        if (state.is(SplatcraftTags.Blocks.SCAN_TURF_SCORED)) {
            return true;
        }
        return state.blocksMotion() && !isPassThrough(level, pos);
    }

    private static OptionalInt turfColorAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (WorldInkStorage.isInked(level, pos)
                || level.getBlockEntity(pos) instanceof InkedBlockEntity
                || state.is(SplatcraftTags.Blocks.SCAN_TURF_SCORED)) {
            return inkColorAt(level, pos);
        }
        return OptionalInt.empty();
    }

    private static boolean isPassThrough(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.empty()).isEmpty();
    }

    private static void addScore(TreeMap<Integer, Integer> scores, OptionalInt color) {
        color.ifPresent(value -> {
            if (value >= 0) {
                scores.merge(value, 1, Integer::sum);
            }
        });
    }

    private static boolean clearInkedBlock(Level level, BlockPos pos, BlockState state, InkedBlockEntity inkedBlock) {
        if (inkedBlock.getPermanentColor() != -1) {
            if (inkedBlock.getColor() == inkedBlock.getPermanentColor()) {
                return false;
            }

            inkedBlock.setColor(inkedBlock.getPermanentColor());
            sendBlockUpdate(inkedBlock);
            return true;
        }

        BlockState savedState = inkedBlock.getSavedState();
        if (savedState == null || savedState.isAir() || savedState.is(state.getBlock())) {
            return false;
        }

        level.setBlock(pos, savedState, 3);
        BlockEntity restoredBlockEntity = level.getBlockEntity(pos);
        if (inkedBlock.getSavedColor() != -1 && restoredBlockEntity instanceof InkColorBlockEntity colorBlock) {
            colorBlock.setColor(inkedBlock.getSavedColor());
            sendBlockUpdate(colorBlock);
        } else if (inkedBlock.getSavedColor() != -1
                && restoredBlockEntity instanceof ColoredBarrierBlockEntity barrier) {
            barrier.setColor(inkedBlock.getSavedColor());
            sendBlockUpdate(barrier);
        }
        return true;
    }

    private static void sendBlockUpdate(BlockEntity blockEntity) {
        if (blockEntity.getLevel() != null) {
            blockEntity.getLevel().sendBlockUpdated(
                    blockEntity.getBlockPos(),
                    blockEntity.getBlockState(),
                    blockEntity.getBlockState(),
                    2);
        }
    }

    private static void sendBlockUpdate(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 2);
    }

    public interface InkColorBlockVisitor {
        boolean visit(TeamInkColorBlock block);
    }

    public record TurfScanResult(TreeMap<Integer, Integer> scores, int scannedBlocks) {
        public int winner() {
            int winner = -1;
            int winnerScore = -1;
            for (Map.Entry<Integer, Integer> entry : scores.entrySet()) {
                if (entry.getValue() > winnerScore) {
                    winner = entry.getKey();
                    winnerScore = entry.getValue();
                }
            }
            return winner;
        }

        public float percent(int score) {
            return scannedBlocks == 0 ? 0.0F : score * 100.0F / scannedBlocks;
        }
    }

    public interface TeamInkColorBlock {
        BlockPos pos();

        int color();

        String team();

        void setColor(int color);

        void setTeam(String team);
    }

    private record InkColorBlock(InkColorBlockEntity blockEntity) implements TeamInkColorBlock {
        @Override
        public BlockPos pos() {
            return blockEntity.getBlockPos();
        }

        @Override
        public int color() {
            return blockEntity.getColor();
        }

        @Override
        public String team() {
            return blockEntity.getTeam();
        }

        @Override
        public void setColor(int color) {
            blockEntity.setColor(color);
        }

        @Override
        public void setTeam(String team) {
            blockEntity.setTeam(team);
        }
    }

    private record ColoredBarrierColorBlock(ColoredBarrierBlockEntity blockEntity) implements TeamInkColorBlock {
        @Override
        public BlockPos pos() {
            return blockEntity.getBlockPos();
        }

        @Override
        public int color() {
            return blockEntity.getColor();
        }

        @Override
        public String team() {
            return blockEntity.getTeam();
        }

        @Override
        public void setColor(int color) {
            blockEntity.setColor(color);
        }

        @Override
        public void setTeam(String team) {
            blockEntity.setTeam(team);
        }
    }

    private record RemotePedestalColorBlock(RemotePedestalBlockEntity blockEntity) implements TeamInkColorBlock {
        @Override
        public BlockPos pos() {
            return blockEntity.getBlockPos();
        }

        @Override
        public int color() {
            return blockEntity.remoteColor();
        }

        @Override
        public String team() {
            return blockEntity.getTeam();
        }

        @Override
        public void setColor(int color) {
            blockEntity.recolorRemote(color);
        }

        @Override
        public void setTeam(String team) {
            blockEntity.setTeam(team);
        }
    }

    private record WorldInkColorBlock(Level level, BlockPos pos, WorldInk worldInk) implements TeamInkColorBlock {
        @Override
        public BlockPos pos() {
            return pos;
        }

        @Override
        public int color() {
            WorldInk.Entry entry = worldInk.getInk(pos);
            return entry == null ? -1 : entry.color();
        }

        @Override
        public String team() {
            return "";
        }

        @Override
        public void setColor(int color) {
            worldInk.setColor(pos, color);
        }

        @Override
        public void setTeam(String team) {
        }
    }
}
