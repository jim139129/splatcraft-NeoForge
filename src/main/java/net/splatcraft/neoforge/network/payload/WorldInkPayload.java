package net.splatcraft.neoforge.network.payload;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.worldink.WorldInk;
import net.splatcraft.neoforge.worldink.WorldInkStorage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorldInkPayload(ChunkPos chunkPos, Map<BlockPos, WorldInk.Entry> ink, boolean replace) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WorldInkPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "world_ink"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldInkPayload> STREAM_CODEC = StreamCodec.of(
            WorldInkPayload::encode,
            WorldInkPayload::decode
    );

    public static WorldInkPayload chunk(ChunkPos chunkPos, Map<BlockPos, WorldInk.Entry> ink) {
        return new WorldInkPayload(chunkPos, new LinkedHashMap<>(ink), true);
    }

    public static WorldInkPayload update(ChunkPos chunkPos, BlockPos localPos, WorldInk.Entry entry) {
        Map<BlockPos, WorldInk.Entry> ink = new LinkedHashMap<>();
        ink.put(localPos, entry);
        return new WorldInkPayload(chunkPos, ink, false);
    }

    public static void handle(WorldInkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            WorldInkStorage.getOrCreateLoaded(level, payload.chunkPos).ifPresent(worldInk -> {
                Set<BlockPos> positionsToUpdate = new HashSet<>(worldInk.inkInChunk().keySet());
                positionsToUpdate.addAll(payload.ink.keySet());

                if (payload.replace) {
                    worldInk.replaceSyncedInk(payload.ink);
                } else {
                    payload.ink.forEach(worldInk::setSyncedInk);
                }

                for (BlockPos localPos : positionsToUpdate) {
                    BlockPos pos = new BlockPos(
                            payload.chunkPos.x * 16 + localPos.getX(),
                            localPos.getY(),
                            payload.chunkPos.z * 16 + localPos.getZ());
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 2);
                }
            });
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static WorldInkPayload decode(RegistryFriendlyByteBuf buffer) {
        ChunkPos chunkPos = new ChunkPos(buffer.readInt(), buffer.readInt());
        boolean replace = buffer.readBoolean();
        int size = buffer.readVarInt();
        Map<BlockPos, WorldInk.Entry> ink = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            BlockPos pos = buffer.readBlockPos();
            if (buffer.readBoolean()) {
                ink.put(pos, new WorldInk.Entry(buffer.readInt(), buffer.readResourceLocation()));
            } else {
                ink.put(pos, null);
            }
        }
        return new WorldInkPayload(chunkPos, ink, replace);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, WorldInkPayload payload) {
        buffer.writeInt(payload.chunkPos.x);
        buffer.writeInt(payload.chunkPos.z);
        buffer.writeBoolean(payload.replace);
        buffer.writeVarInt(payload.ink.size());
        payload.ink.forEach((pos, entry) -> {
            buffer.writeBlockPos(pos);
            buffer.writeBoolean(entry != null);
            if (entry != null) {
                buffer.writeInt(entry.color());
                buffer.writeResourceLocation(entry.type());
            }
        });
    }
}
