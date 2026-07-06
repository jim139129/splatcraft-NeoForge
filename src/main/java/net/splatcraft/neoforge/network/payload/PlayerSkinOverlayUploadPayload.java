package net.splatcraft.neoforge.network.payload;

import java.util.Arrays;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;

public record PlayerSkinOverlayUploadPayload(UUID playerId, byte[] imageBytes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerSkinOverlayUploadPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "player_skin_overlay_upload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSkinOverlayUploadPayload> STREAM_CODEC = StreamCodec.of(
            PlayerSkinOverlayUploadPayload::encode,
            PlayerSkinOverlayUploadPayload::decode
    );

    public PlayerSkinOverlayUploadPayload {
        imageBytes = imageBytes == null ? new byte[0] : Arrays.copyOf(imageBytes, imageBytes.length);
    }

    public static void handle(PlayerSkinOverlayUploadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerSkinOverlayPayload.receiveUpload(payload.playerId, payload.imageBytes, serverPlayer);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static PlayerSkinOverlayUploadPayload decode(RegistryFriendlyByteBuf buffer) {
        return new PlayerSkinOverlayUploadPayload(
                buffer.readUUID(),
                buffer.readByteArray(PlayerSkinOverlayPayload.MAX_IMAGE_BYTES));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerSkinOverlayUploadPayload payload) {
        buffer.writeUUID(payload.playerId);
        buffer.writeByteArray(payload.imageBytes);
    }
}
