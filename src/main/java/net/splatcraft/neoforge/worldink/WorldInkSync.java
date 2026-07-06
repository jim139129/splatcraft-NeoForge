package net.splatcraft.neoforge.worldink;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.splatcraft.neoforge.network.payload.WorldInkPayload;

public final class WorldInkSync {
    private WorldInkSync() {
    }

    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        LevelChunk chunk = event.getChunk();
        WorldInkStorage.getLoaded(event.getLevel(), event.getPos())
                .filter(worldInk -> !worldInk.inkInChunk().isEmpty())
                .ifPresent(worldInk -> sendChunk(event.getPlayer(), chunk.getPos(), worldInk));
    }

    public static void sendUpdate(ServerLevel level, ChunkPos chunkPos, BlockPos localPos, WorldInk.Entry entry) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, WorldInkPayload.update(chunkPos, localPos, entry));
    }

    private static void sendChunk(ServerPlayer player, ChunkPos chunkPos, WorldInk worldInk) {
        PacketDistributor.sendToPlayer(player, WorldInkPayload.chunk(chunkPos, worldInk.inkInChunk()));
    }
}
