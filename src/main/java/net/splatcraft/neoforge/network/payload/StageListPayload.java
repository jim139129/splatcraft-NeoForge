package net.splatcraft.neoforge.network.payload;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.ClientStageCache;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.data.Stage;

public record StageListPayload(CompoundTag stages) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StageListPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "stage_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StageListPayload> STREAM_CODEC = StreamCodec.of(
            StageListPayload::encode,
            StageListPayload::decode
    );

    public StageListPayload {
        stages = stages == null ? new CompoundTag() : stages;
    }

    public static StageListPayload fromServer(MinecraftServer server) {
        return new StageListPayload(serializeStages(SplatcraftSaveData.get(server).stages()));
    }

    public static void sendToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, fromServer(player.getServer()));
    }

    public static void sendToAll(MinecraftServer server) {
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            PacketDistributor.sendToAllPlayers(fromServer(server));
        }
    }

    public static void handle(StageListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStageCache.replace(payload.stages));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static StageListPayload decode(RegistryFriendlyByteBuf buffer) {
        return new StageListPayload(buffer.readNbt());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, StageListPayload payload) {
        buffer.writeNbt(payload.stages);
    }

    private static CompoundTag serializeStages(Map<String, Stage> stages) {
        CompoundTag tag = new CompoundTag();
        stages.forEach((stageId, stage) -> tag.put(stageId, stage.save()));
        return tag;
    }
}
