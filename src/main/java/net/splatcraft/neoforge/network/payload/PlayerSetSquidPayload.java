package net.splatcraft.neoforge.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.player.SquidFormEvents;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public record PlayerSetSquidPayload(boolean squid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerSetSquidPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "player_set_squid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSetSquidPayload> STREAM_CODEC = StreamCodec.of(
            PlayerSetSquidPayload::encode,
            PlayerSetSquidPayload::decode
    );

    public static void sendToServer(boolean squid) {
        PacketDistributor.sendToServer(new PlayerSetSquidPayload(squid));
    }

    public static void handle(PlayerSetSquidPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleServer(payload, context));
    }

    private static void handleServer(PlayerSetSquidPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }

        if (SplatcraftPlayerInfoEvents.isSquid(player) == payload.squid) {
            return;
        }

        if (payload.squid && !canEnterSquid(player)) {
            SplatcraftPlayerInfoEvents.sync(player);
            return;
        }

        SplatcraftPlayerInfoEvents.setSquid(player, payload.squid);
        playTransformSound(player, payload.squid);
    }

    private static boolean canEnterSquid(ServerPlayer player) {
        return player.containerMenu == player.inventoryMenu
                && SquidFormEvents.canEnterSquid(player);
    }

    private static void playTransformSound(ServerPlayer player, boolean squid) {
        SoundEvent sound = squid ? SplatcraftSounds.SQUID_TRANSFORM.get() : SplatcraftSounds.SQUID_REVERT.get();
        float pitch = ((player.level().getRandom().nextFloat() - player.level().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.75F, pitch);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerSetSquidPayload payload) {
        buffer.writeBoolean(payload.squid);
    }

    private static PlayerSetSquidPayload decode(RegistryFriendlyByteBuf buffer) {
        return new PlayerSetSquidPayload(buffer.readBoolean());
    }
}
