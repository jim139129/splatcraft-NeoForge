package net.splatcraft.neoforge.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.item.WeaponItem;

public record DodgeRollPayload(float leftImpulse, float forwardImpulse) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DodgeRollPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "dodge_roll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DodgeRollPayload> STREAM_CODEC = StreamCodec.of(
            DodgeRollPayload::encode,
            DodgeRollPayload::decode
    );

    public static void sendToServer(float leftImpulse, float forwardImpulse) {
        PacketDistributor.sendToServer(new DodgeRollPayload(leftImpulse, forwardImpulse));
    }

    public static void handle(DodgeRollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleServer(payload, context));
    }

    private static void handleServer(DodgeRollPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator() || !player.isUsingItem()) {
            return;
        }

        if (!Float.isFinite(payload.leftImpulse) || !Float.isFinite(payload.forwardImpulse)) {
            return;
        }

        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof WeaponItem weapon) || weapon.weaponClass() != WeaponItem.WeaponClass.DUALIE) {
            return;
        }

        weapon.tryPerformDualieDodge(player, stack, clampInput(payload.leftImpulse), clampInput(payload.forwardImpulse));
    }

    private static float clampInput(float impulse) {
        return Math.max(-1.0F, Math.min(1.0F, impulse));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static DodgeRollPayload decode(RegistryFriendlyByteBuf buffer) {
        return new DodgeRollPayload(buffer.readFloat(), buffer.readFloat());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, DodgeRollPayload payload) {
        buffer.writeFloat(payload.leftImpulse);
        buffer.writeFloat(payload.forwardImpulse);
    }
}
