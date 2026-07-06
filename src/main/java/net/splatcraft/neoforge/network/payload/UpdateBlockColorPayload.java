package net.splatcraft.neoforge.network.payload;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.blockentity.InkVatBlockEntity;
import net.splatcraft.neoforge.menu.InkVatMenu;

public record UpdateBlockColorPayload(BlockPos pos, int color, int inkVatPointer) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateBlockColorPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "update_block_color"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBlockColorPayload> STREAM_CODEC = StreamCodec.of(
            UpdateBlockColorPayload::encode,
            UpdateBlockColorPayload::decode
    );

    public UpdateBlockColorPayload(BlockPos pos, int color) {
        this(pos, color, -1);
    }

    public static void sendToServer(BlockPos pos, int color, int pointer) {
        PacketDistributor.sendToServer(new UpdateBlockColorPayload(pos, color, pointer));
    }

    public static void handle(UpdateBlockColorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleServer(payload, context));
    }

    private static void handleServer(UpdateBlockColorPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos);
        if (!(blockEntity instanceof InkVatBlockEntity inkVat)
                || !(player.containerMenu instanceof InkVatMenu menu)
                || !menu.isFor(inkVat)
                || !inkVat.stillValid(player)) {
            return;
        }

        List<Integer> colors = InkVatMenu.availableRecipeColors(inkVat);
        int pointer = payload.inkVatPointer;
        if (pointer < 0 || pointer >= colors.size()) {
            return;
        }

        int color = colors.get(pointer);
        if (color != payload.color) {
            return;
        }

        inkVat.setPointer(pointer);
        inkVat.setColor(color);
        InkVatMenu.updateOutput(inkVat);
        sendBlockUpdate(inkVat);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static UpdateBlockColorPayload decode(RegistryFriendlyByteBuf buffer) {
        return new UpdateBlockColorPayload(buffer.readBlockPos(), buffer.readInt(), buffer.readInt());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, UpdateBlockColorPayload payload) {
        buffer.writeBlockPos(payload.pos);
        buffer.writeInt(payload.color);
        buffer.writeInt(payload.inkVatPointer);
    }

    private static void sendBlockUpdate(BlockEntity blockEntity) {
        if (blockEntity.getLevel() != null) {
            blockEntity.getLevel().sendBlockUpdated(
                    blockEntity.getBlockPos(),
                    blockEntity.getBlockState(),
                    blockEntity.getBlockState(),
                    2
            );
        }
    }
}
