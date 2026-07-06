package net.splatcraft.neoforge.worldink;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;

public final class WorldInkStorage {
    private WorldInkStorage() {
    }

    public static WorldInk get(Level level, BlockPos pos) {
        return get(level.getChunkAt(pos));
    }

    public static WorldInk get(LevelChunk chunk) {
        return chunk.getData(SplatcraftAttachments.WORLD_INK.get());
    }

    public static Optional<WorldInk> getLoaded(Level level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()),
                ChunkStatus.FULL,
                false);
        return chunk instanceof LevelChunk levelChunk
                ? levelChunk.getExistingData(SplatcraftAttachments.WORLD_INK.get())
                : Optional.empty();
    }

    public static Optional<WorldInk> getLoaded(Level level, ChunkPos pos) {
        ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        return chunk instanceof LevelChunk levelChunk
                ? levelChunk.getExistingData(SplatcraftAttachments.WORLD_INK.get())
                : Optional.empty();
    }

    public static Optional<WorldInk> getOrCreateLoaded(Level level, ChunkPos pos) {
        ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        return chunk instanceof LevelChunk levelChunk
                ? Optional.of(get(levelChunk))
                : Optional.empty();
    }

    public static Optional<WorldInk.Entry> getInk(Level level, BlockPos pos) {
        return getLoaded(level, pos).map(worldInk -> worldInk.getInk(pos));
    }

    public static boolean isInked(Level level, BlockPos pos) {
        return getInk(level, pos).isPresent();
    }

    public static boolean ink(Level level, BlockPos pos, int color, ResourceLocation type) {
        return get(level, pos).ink(pos, color, type);
    }

    public static boolean clearInk(Level level, BlockPos pos) {
        return clearInk(level, pos, false);
    }

    public static boolean clearInk(Level level, BlockPos pos, boolean removePermanent) {
        return getLoaded(level, pos)
                .map(worldInk -> worldInk.clearInk(pos, removePermanent))
                .orElse(false);
    }

    public static Optional<WorldInk.Entry> getPermanentInk(Level level, BlockPos pos) {
        return getLoaded(level, pos).map(worldInk -> worldInk.getPermanentInk(pos));
    }

    public static boolean setPermanentInk(Level level, BlockPos pos, int color, ResourceLocation type) {
        return get(level, pos).setPermanentInk(pos, color, type);
    }

    public static boolean removePermanentInk(Level level, BlockPos pos) {
        return getLoaded(level, pos)
                .map(worldInk -> worldInk.removePermanentInk(pos))
                .orElse(false);
    }
}
