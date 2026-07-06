package net.splatcraft.neoforge.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.splatcraft.neoforge.Splatcraft;

public final class SplatcraftData {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation STARTER_COLORS = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "starter_colors");

    private static Map<ResourceLocation, WeaponSettings> weaponSettings = Map.of();
    private static Map<ResourceLocation, List<Integer>> inkColorTags = Map.of();

    private SplatcraftData() {
    }

    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WeaponSettingsReloadListener());
        event.addListener(new InkColorTagsReloadListener());
    }

    public static Map<ResourceLocation, WeaponSettings> weaponSettings() {
        return weaponSettings;
    }

    public static void replaceWeaponSettings(Map<ResourceLocation, WeaponSettings> settings) {
        weaponSettings = Map.copyOf(settings);
    }

    public static WeaponSettings weaponSettings(String path) {
        return weaponSettings(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, path));
    }

    public static WeaponSettings weaponSettings(ResourceLocation id) {
        return weaponSettings.getOrDefault(id, WeaponSettings.EMPTY);
    }

    public static List<Integer> inkColorTag(ResourceLocation id) {
        return inkColorTags.getOrDefault(id, List.of());
    }

    public static List<Integer> starterColors() {
        List<Integer> colors = inkColorTag(STARTER_COLORS);
        return colors.isEmpty() ? List.of(0xDF641A, 0x26229F, 0x409D3B, 0xC83D79) : colors;
    }

    private static final class WeaponSettingsReloadListener extends SimpleJsonResourceReloadListener {
        private WeaponSettingsReloadListener() {
            super(GSON, "weapon_settings");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, WeaponSettings> loaded = new HashMap<>();

            entries.forEach((id, element) -> {
                WeaponSettings.CODEC.parse(JsonOps.INSTANCE, element)
                        .resultOrPartial(message -> Splatcraft.LOGGER.warn("Failed to load weapon settings {}: {}", id, message))
                        .ifPresent(settings -> loaded.put(id, settings));
            });

            weaponSettings = Map.copyOf(loaded);
            Splatcraft.LOGGER.info("Loaded {} Splatcraft weapon settings", weaponSettings.size());
        }
    }

    private static final class InkColorTagsReloadListener extends SimpleJsonResourceReloadListener {
        private InkColorTagsReloadListener() {
            super(GSON, "tags/ink_colors");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, List<Integer>> loaded = new HashMap<>();

            entries.forEach((id, element) -> {
                JsonObject json = element.getAsJsonObject();
                List<Integer> values = GsonHelper.getAsBoolean(json, "replace", false)
                        ? new ArrayList<>()
                        : new ArrayList<>(loaded.getOrDefault(id, List.of()));

                for (JsonElement value : GsonHelper.getAsJsonArray(json, "values")) {
                    parseColorValue(value).ifPresent(values::add);
                }

                loaded.put(id, List.copyOf(values));
            });

            inkColorTags = Map.copyOf(loaded);
            Splatcraft.LOGGER.info("Loaded {} Splatcraft ink color tags", inkColorTags.size());
        }

        private static OptionalInt parseColorValue(JsonElement element) {
            if (GsonHelper.isNumberValue(element)) {
                int color = element.getAsInt();
                return color >= 0 && color <= 0xFFFFFF ? OptionalInt.of(color) : OptionalInt.empty();
            }

            if (GsonHelper.isStringValue(element)) {
                return InkColorData.resolve(element.getAsString());
            }

            return OptionalInt.empty();
        }
    }
}
