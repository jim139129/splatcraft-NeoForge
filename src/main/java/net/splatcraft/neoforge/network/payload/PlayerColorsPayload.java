package net.splatcraft.neoforge.network.payload;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.ClientPlayerColors;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;

public record PlayerColorsPayload(boolean reset, List<Entry> colors) implements CustomPacketPayload {
    private static final int MAX_COLORS = 1024;
    private static final int MAX_NAME_CHARS = 64;

    public static final CustomPacketPayload.Type<PlayerColorsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "player_colors"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerColorsPayload> STREAM_CODEC = StreamCodec.of(
            PlayerColorsPayload::encode,
            PlayerColorsPayload::decode
    );

    public PlayerColorsPayload {
        colors = List.copyOf(colors);
    }

    public static void sendSnapshotTo(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        List<Entry> entries = server == null
                ? List.of(entry(player))
                : server.getPlayerList().getPlayers().stream().map(PlayerColorsPayload::entry).toList();
        PacketDistributor.sendToPlayer(player, new PlayerColorsPayload(true, entries));
    }

    public static void sendUpdateToAll(ServerPlayer player) {
        PacketDistributor.sendToAllPlayers(new PlayerColorsPayload(false, List.of(entry(player))));
    }

    public static void handle(PlayerColorsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.reset) {
                ClientPlayerColors.clear();
            }
            payload.colors.forEach(entry -> ClientPlayerColors.put(entry.playerId(), entry.playerName(), entry.color()));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static Entry entry(ServerPlayer player) {
        return new Entry(player.getUUID(), player.getGameProfile().getName(), playerColor(player));
    }

    private static int playerColor(ServerPlayer player) {
        return player.getExistingData(SplatcraftAttachments.PLAYER_INFO.get())
                .map(PlayerInfo::color)
                .orElse(InkColorData.DEFAULT_COLOR);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerColorsPayload payload) {
        buffer.writeBoolean(payload.reset);
        buffer.writeVarInt(payload.colors.size());
        payload.colors.forEach(entry -> {
            buffer.writeUUID(entry.playerId());
            buffer.writeUtf(entry.playerName(), MAX_NAME_CHARS);
            buffer.writeInt(entry.color());
        });
    }

    private static PlayerColorsPayload decode(RegistryFriendlyByteBuf buffer) {
        boolean reset = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_COLORS) {
            throw new IllegalArgumentException("Invalid Splatcraft player colors payload size: " + size);
        }

        Entry[] entries = new Entry[size];
        for (int index = 0; index < size; index++) {
            UUID playerId = buffer.readUUID();
            String playerName = buffer.readUtf(MAX_NAME_CHARS);
            int color = buffer.readInt();
            entries[index] = new Entry(playerId, playerName, color);
        }
        return new PlayerColorsPayload(reset, List.of(entries));
    }

    public record Entry(UUID playerId, String playerName, int color) {
    }
}
