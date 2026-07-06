package net.splatcraft.neoforge.command;

import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.data.Stage;
import net.splatcraft.neoforge.worldink.InkAreaActions;

final class InkAreaOperations {
    private InkAreaOperations() {
    }

    static ServerLevel stageLevel(Level fallbackLevel, Stage stage) {
        if (fallbackLevel == null || fallbackLevel.getServer() == null) {
            return null;
        }

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, stage.dimension());
        return fallbackLevel.getServer().getLevel(dimension);
    }

    static BlockPos min(BlockPos left, BlockPos right) {
        return InkAreaActions.min(left, right);
    }

    static BlockPos max(BlockPos left, BlockPos right) {
        return InkAreaActions.max(left, right);
    }

    static int visitColorBlocks(Level level, BlockPos from, BlockPos to, InkColorBlockVisitor visitor) {
        return InkAreaActions.visitColorBlocks(
                level,
                from,
                to,
                block -> visitor.visit(new InkAreaActionColorBlock(block)));
    }

    static int clearInk(Level level, BlockPos from, BlockPos to) {
        return InkAreaActions.clearInk(level, from, to);
    }

    static OptionalInt inkColorAt(Level level, BlockPos pos) {
        return InkAreaActions.inkColorAt(level, pos);
    }

    static boolean inWorldBounds(Level level, BlockPos from, BlockPos to) {
        return InkAreaActions.inWorldBounds(level, from, to);
    }

    interface InkColorBlockVisitor {
        boolean visit(TeamInkColorBlock block);
    }

    interface TeamInkColorBlock {
        int color();

        String team();

        void setColor(int color);

        void setTeam(String team);
    }

    private record InkAreaActionColorBlock(InkAreaActions.TeamInkColorBlock block) implements TeamInkColorBlock {
        @Override
        public int color() {
            return block.color();
        }

        @Override
        public String team() {
            return block.team();
        }

        @Override
        public void setColor(int color) {
            block.setColor(color);
        }

        @Override
        public void setTeam(String team) {
            block.setTeam(team);
        }
    }
}
