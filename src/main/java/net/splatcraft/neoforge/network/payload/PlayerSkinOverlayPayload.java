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

    // Skin overlays are stretched onto the 64x64 player skin layout; anything larger only
    // wastes memory on every client, and a crafted high-compression PNG could decompress
    // into a huge texture. Reject before broadcasting.
    public static final int MAX_IMAGE_DIMENSION = 256;
    private static final long UPLOAD_COOLDOWN_MS = 5_000L;

    private static final Map<UUID, byte[]> SERVER_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_UPLOAD_MS = new ConcurrentHashMap<>();
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
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_UPLOAD_MS.remove(player.getUUID());
            if (SERVER_CACHE.remove(player.getUUID()) != null) {
                PacketDistributor.sendToAllPlayers(new PlayerSkinOverlayPayload(player.getUUID(), new byte[0]));
            }
        }
    }

    public static void handle(PlayerSkinOverlayPayload payload, IPayloadContext context) {
        clientHandler.handle(payload, context);
    }

    static void receiveUpload(UUID playerId, byte[] imageBytes, ServerPlayer sender) {
        if (!sender.getUUID().equals(playerId) || imageBytes.length > MAX_IMAGE_BYTES) {
            return;
        }
        if (imageBytes.length > 0 && !isAcceptablePng(imageBytes)) {
            return;
        }
        // The server relays each upload to every online player; without dedup and a cooldown a
        // client could re-send 256KB every tick and amplify it by the whole player list.
        if (Arrays.equals(imageBytes, SERVER_CACHE.getOrDefault(playerId, new byte[0]))) {
            return;
        }
        long now = System.currentTimeMillis();
        Long lastUpload = LAST_UPLOAD_MS.put(playerId, now);
        if (lastUpload != null && now - lastUpload < UPLOAD_COOLDOWN_MS) {
            return;
        }

        if (imageBytes.length == 0) {
            SERVER_CACHE.remove(playerId);
        } else {
            SERVER_CACHE.put(playerId, imageBytes);
        }
        PacketDistributor.sendToAllPlayers(new PlayerSkinOverlayPayload(playerId, imageBytes));
    }

    /**
     * Validates the PNG signature and reads the IHDR dimensions without decoding, so oversized
     * or non-PNG payloads are rejected before reaching any client. Runs on the dedicated server,
     * which has no image decoding classes available.
     */
    private static boolean isAcceptablePng(byte[] imageBytes) {
        if (imageBytes.length < 24
                || imageBytes[0] != (byte) 0x89 || imageBytes[1] != 'P'
                || imageBytes[2] != 'N' || imageBytes[3] != 'G') {
            return false;
        }
        int width = readBigEndianInt(imageBytes, 16);
        int height = readBigEndianInt(imageBytes, 20);
        return width > 0 && height > 0 && width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION;
    }

    private static int readBigEndianInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
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
