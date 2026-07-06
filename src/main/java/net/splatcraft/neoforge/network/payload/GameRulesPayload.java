package net.splatcraft.neoforge.network.payload;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;

public record GameRulesPayload(Map<String, Boolean> booleanRules, Map<String, Integer> integerRules) implements CustomPacketPayload {
    private static final int MAX_RULES = 64;
    private static final int MAX_RULE_ID_CHARS = 128;

    public static final CustomPacketPayload.Type<GameRulesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "game_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GameRulesPayload> STREAM_CODEC = StreamCodec.of(
            GameRulesPayload::encode,
            GameRulesPayload::decode
    );

    public GameRulesPayload {
        booleanRules = Map.copyOf(booleanRules);
        integerRules = Map.copyOf(integerRules);
    }

    public static void sendToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, current(player.getServer()));
    }

    public static void sendToAll(MinecraftServer server) {
        GameRulesPayload payload = current(server);
        server.getPlayerList().getPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    public static void handle(GameRulesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SplatcraftGameRules.replaceClientValues(payload.booleanRules, payload.integerRules));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static GameRulesPayload current(MinecraftServer server) {
        return new GameRulesPayload(
                SplatcraftGameRules.booleanValues(server),
                SplatcraftGameRules.integerValues(server));
    }

    private static GameRulesPayload decode(RegistryFriendlyByteBuf buffer) {
        Map<String, Boolean> booleanRules = new LinkedHashMap<>();
        int booleanRuleCount = buffer.readVarInt();
        if (booleanRuleCount < 0 || booleanRuleCount > MAX_RULES) {
            throw new IllegalArgumentException("Invalid Splatcraft boolean gamerule payload size: " + booleanRuleCount);
        }
        for (int index = 0; index < booleanRuleCount; index++) {
            booleanRules.put(buffer.readUtf(MAX_RULE_ID_CHARS), buffer.readBoolean());
        }

        Map<String, Integer> integerRules = new LinkedHashMap<>();
        int integerRuleCount = buffer.readVarInt();
        if (integerRuleCount < 0 || integerRuleCount > MAX_RULES) {
            throw new IllegalArgumentException("Invalid Splatcraft integer gamerule payload size: " + integerRuleCount);
        }
        for (int index = 0; index < integerRuleCount; index++) {
            integerRules.put(buffer.readUtf(MAX_RULE_ID_CHARS), buffer.readInt());
        }
        return new GameRulesPayload(booleanRules, integerRules);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, GameRulesPayload payload) {
        buffer.writeVarInt(payload.booleanRules.size());
        payload.booleanRules.forEach((rule, value) -> {
            buffer.writeUtf(rule, MAX_RULE_ID_CHARS);
            buffer.writeBoolean(value);
        });

        buffer.writeVarInt(payload.integerRules.size());
        payload.integerRules.forEach((rule, value) -> {
            buffer.writeUtf(rule, MAX_RULE_ID_CHARS);
            buffer.writeInt(value);
        });
    }
}
