package net.splatcraft.neoforge.network.payload;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.SplatcraftData;
import net.splatcraft.neoforge.data.WeaponSettings;

public record WeaponSettingsPayload(Map<ResourceLocation, String> settings) implements CustomPacketPayload {
    private static final int MAX_SETTINGS = 1024;
    private static final int MAX_SETTING_JSON_CHARS = 262_144;

    public static final CustomPacketPayload.Type<WeaponSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "weapon_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponSettingsPayload> STREAM_CODEC = StreamCodec.of(
            WeaponSettingsPayload::encode,
            WeaponSettingsPayload::decode
    );

    public WeaponSettingsPayload {
        settings = Map.copyOf(settings);
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        WeaponSettingsPayload payload = current();
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    public static void sendToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, current());
    }

    public static void handle(WeaponSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Map<ResourceLocation, WeaponSettings> decoded = new LinkedHashMap<>();
            payload.settings.forEach((id, json) -> parseSetting(id, json, decoded));
            SplatcraftData.replaceWeaponSettings(decoded);
            Splatcraft.LOGGER.info("Synced {} Splatcraft weapon settings from server", decoded.size());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static WeaponSettingsPayload current() {
        Map<ResourceLocation, String> encoded = new LinkedHashMap<>();
        SplatcraftData.weaponSettings().forEach((id, settings) ->
                WeaponSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings)
                        .resultOrPartial(message -> Splatcraft.LOGGER.warn("Failed to encode weapon settings {}: {}", id, message))
                        .ifPresent(json -> encoded.put(id, json.toString())));
        return new WeaponSettingsPayload(encoded);
    }

    private static void parseSetting(ResourceLocation id, String json, Map<ResourceLocation, WeaponSettings> decoded) {
        try {
            JsonElement element = JsonParser.parseString(json);
            WeaponSettings.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(message -> Splatcraft.LOGGER.warn("Failed to decode weapon settings {}: {}", id, message))
                    .ifPresent(settings -> decoded.put(id, settings));
        } catch (JsonSyntaxException exception) {
            Splatcraft.LOGGER.warn("Failed to parse weapon settings {} from server", id, exception);
        }
    }

    private static WeaponSettingsPayload decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_SETTINGS) {
            throw new IllegalArgumentException("Invalid Splatcraft weapon settings payload size: " + size);
        }

        Map<ResourceLocation, String> settings = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            settings.put(buffer.readResourceLocation(), buffer.readUtf(MAX_SETTING_JSON_CHARS));
        }
        return new WeaponSettingsPayload(settings);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, WeaponSettingsPayload payload) {
        buffer.writeVarInt(payload.settings.size());
        payload.settings.forEach((id, json) -> {
            buffer.writeResourceLocation(id);
            buffer.writeUtf(json, MAX_SETTING_JSON_CHARS);
        });
    }
}
