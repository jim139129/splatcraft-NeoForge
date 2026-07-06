package net.splatcraft.neoforge.network.payload;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;

public record PlayerSkinOverlayPayload(UUID playerId, byte[] imageBytes) implements CustomPacketPayload {
    public static final int MAX_IMAGE_BYTES = 262_144;
    public static final CustomPacketPayload.Type<PlayerSkinOverlayPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "player_skin_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSkinOverlayPayload> STREAM_CODEC = StreamCodec.of(
            PlayerSkinOverlayPayload::encode,
            PlayerSkinOverlayPayload::decode
    );

    private static final Map<UUID, byte[]> SERVER_CACHE = new ConcurrentHashMap<>();
    private static IPayloadHandler<PlayerSkinOverlayPayload> clientHandler = (payload, context) -> {};

    public PlayerSkinOverlayPayload {
        imageBytes = imageBytes == null ? new byte[0] : Arrays.copyOf(imageBytes, imageBytes.length);
    }

    public static void setClientHandler(IPayloadHandler<PlayerSkinOverlayPayload> handler) {
        clientHandler = Objects.requireNonNull(handler);
    }

    public static void sendToServer(UUID playerId, byte[] imageBytes) {
        if (imageBytes.length <= MAX_IMAGE_BYTES) {
            PacketDistributor.sendToServer(new PlayerSkinOverlayUploadPayload(playerId, imageBytes));
        }
    }

    public static void sendCachedTo(ServerPlayer player) {
        SERVER_CACHE.forEach((playerId, imageBytes) ->
                PacketDistributor.sendToPlayer(player, new PlayerSkinOverlayPayload(playerId, imageBytes)));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SERVER_CACHE.remove(player.getUUID()) != null) {
            PacketDistributor.sendToAllPlayers(new PlayerSkinOverlayPayload(player.getUUID(), new byte[0]));
        }
    }

    public static void handle(PlayerSkinOverlayPayload payload, IPayloadContext context) {
        clientHandler.handle(payload, context);
    }

    static void receiveUpload(UUID playerId, byte[] imageBytes, ServerPlayer sender) {
        if (!sender.getUUID().equals(playerId) || imageBytes.length > MAX_IMAGE_BYTES) {
            return;
        }

        if (imageBytes.length == 0) {
            SERVER_CACHE.remove(playerId);
        } else {
            SERVER_CACHE.put(playerId, imageBytes);
        }
        PacketDistributor.sendToAllPlayers(new PlayerSkinOverlayPayload(playerId, imageBytes));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static PlayerSkinOverlayPayload decode(RegistryFriendlyByteBuf buffer) {
        return new PlayerSkinOverlayPayload(buffer.readUUID(), buffer.readByteArray(MAX_IMAGE_BYTES));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerSkinOverlayPayload payload) {
        buffer.writeUUID(payload.playerId);
        buffer.writeByteArray(payload.imageBytes);
    }
}
