package net.splatcraft.neoforge.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.splatcraft.neoforge.Splatcraft;

public record WeaponSettings(ResourceLocation type, WeaponStats stats, JsonObject raw) {
    public static final WeaponSettings EMPTY =
            new WeaponSettings(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "unknown"), WeaponStats.EMPTY, new JsonObject());

    public static final Codec<WeaponSettings> CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        JsonElement element = dynamic.convert(JsonOps.INSTANCE).getValue();
        if (!element.isJsonObject()) {
            return DataResult.error(() -> "Expected weapon settings object");
        }

        return fromJson(element.getAsJsonObject());
    }, settings -> new Dynamic<>(JsonOps.INSTANCE, settings.raw.deepCopy()));

    public WeaponSettings {
        raw = raw.deepCopy();
    }

    public static DataResult<WeaponSettings> fromJson(JsonObject json) {
        if (!json.has("type")) {
            return DataResult.error(() -> "Missing weapon settings type");
        }

        ResourceLocation type = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type"));
        if (type == null) {
            return DataResult.error(() -> "Invalid weapon settings type");
        }

        return DataResult.success(new WeaponSettings(type, WeaponStats.fromJson(type, json), json));
    }

    public boolean isType(String path) {
        return type.equals(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, path));
    }

    public record WeaponStats(
            float mobility,
            boolean secret,
            boolean fullDamageToMobs,
            Optional<Float> maxDamage,
            Optional<Float> minDamage,
            Optional<Float> range,
            Optional<Float> inkConsumption,
            Optional<Integer> inkRecoveryCooldown,
            Optional<Integer> firingSpeed,
            Optional<Integer> startupTicks,
            Optional<Integer> endlagTicks
    ) {
        public static final WeaponStats EMPTY = new WeaponStats(
                1.0F,
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        private static WeaponStats fromJson(ResourceLocation type, JsonObject json) {
            JsonObject projectile = object(json, "projectile");
            JsonObject shot = object(json, "shot");
            JsonObject roll = object(json, "roll");
            JsonObject swing = object(json, "swing");
            JsonObject fling = object(json, "fling");
            JsonObject charge = object(json, "charge");

            Optional<Float> maxDamage = Optional.empty();
            Optional<Float> minDamage = Optional.empty();
            Optional<Float> range = Optional.empty();
            Optional<Float> inkConsumption = Optional.empty();
            Optional<Integer> inkRecoveryCooldown = Optional.empty();
            Optional<Integer> firingSpeed = Optional.empty();
            Optional<Integer> startupTicks = Optional.empty();
            Optional<Integer> endlagTicks = Optional.empty();

            String path = type.getPath();
            switch (path) {
                case "shooter", "dualie" -> {
                    maxDamage = number(projectile, "base_damage");
                    minDamage = fallback(number(projectile, "decayed_damage"), maxDamage);
                    range = number(projectile, "straight_shot_distance");
                    inkConsumption = number(shot, "ink_consumption");
                    inkRecoveryCooldown = integer(shot, "ink_recovery_cooldown");
                    firingSpeed = integer(shot, "firing_speed");
                    startupTicks = integer(shot, "startup_ticks");
                }
                case "blaster" -> {
                    maxDamage = number(projectile, "direct_damage");
                    minDamage = fallback(number(projectile, "splash_damage"), maxDamage);
                    range = number(projectile, "range");
                    inkConsumption = number(shot, "ink_consumption");
                    inkRecoveryCooldown = integer(shot, "ink_recovery_cooldown");
                    startupTicks = integer(shot, "startup_ticks");
                    endlagTicks = integer(shot, "endlag_ticks");
                }
                case "charger" -> {
                    Optional<Float> maxPartialDamage = number(projectile, "max_partial_charge_damage");
                    maxDamage = number(projectile, "fully_charged_damage").or(() -> maxPartialDamage);
                    minDamage = fallback(number(projectile, "min_partial_charge_damage"), maxDamage);
                    range = number(projectile, "max_charge_range").or(() -> number(projectile, "min_charge_range"));
                    inkConsumption = number(shot, "full_charge_ink_consumption").or(() -> number(shot, "min_charge_ink_consumption"));
                    inkRecoveryCooldown = integer(shot, "ink_recovery_cooldown");
                    startupTicks = integer(charge, "charge_time_ticks");
                    endlagTicks = integer(shot, "endlag_ticks");
                }
                case "roller" -> {
                    Optional<Float> swingDamage = number(swing, "base_damage");
                    Optional<Float> flingDamage = number(fling, "base_damage");
                    maxDamage = max(swingDamage, flingDamage).or(() -> number(roll, "damage"));
                    minDamage = fallback(min(number(swing, "min_damage"), number(fling, "min_damage")), maxDamage);
                    range = number(swing, "projectile_speed").or(() -> number(fling, "projectile_speed"));
                    inkConsumption = number(swing, "ink_consumption").or(() -> number(roll, "ink_consumption"));
                    inkRecoveryCooldown = integer(swing, "ink_recovery_cooldown").or(() -> integer(roll, "ink_recovery_cooldown"));
                    startupTicks = integer(swing, "startup_time");
                }
                case "slosher" -> {
                    maxDamage = number(projectile, "direct_damage");
                    minDamage = fallback(number(projectile, "splash_damage"), maxDamage);
                    range = number(projectile, "speed");
                    inkConsumption = number(shot, "ink_consumption");
                    inkRecoveryCooldown = integer(shot, "ink_recovery_cooldown");
                    startupTicks = integer(shot, "startup_ticks");
                    endlagTicks = integer(shot, "endlag_ticks");
                }
                case "splatling" -> {
                    maxDamage = number(projectile, "base_damage");
                    minDamage = fallback(number(projectile, "decayed_damage"), maxDamage);
                    range = number(projectile, "straight_shot_distance");
                    inkConsumption = number(json, "max_ink_consumption");
                    inkRecoveryCooldown = integer(json, "ink_recovery_cooldown");
                    firingSpeed = integer(shot, "firing_speed");
                    startupTicks = integer(shot, "startup_ticks");
                    endlagTicks = integer(charge, "total_firing_duration");
                }
                case "sub_weapon" -> {
                    maxDamage = number(json, "direct_damage").or(() -> number(json, "prop_damage"));
                    minDamage = fallback(number(json, "indirect_damage"), maxDamage);
                    range = number(json, "explosion_size");
                    inkConsumption = number(json, "ink_consumption");
                    inkRecoveryCooldown = integer(json, "ink_recovery_cooldown");
                    startupTicks = integer(json, "fuse_time");
                }
                default -> {
                }
            }

            return new WeaponStats(
                    number(json, "mobility").orElse(1.0F),
                    bool(json, "is_secret").or(() -> bool(json, "isSecret")).orElse(false),
                    bool(json, "full_damage_to_mobs").orElse(false),
                    maxDamage,
                    minDamage,
                    range,
                    inkConsumption,
                    inkRecoveryCooldown,
                    firingSpeed,
                    startupTicks,
                    endlagTicks
            );
        }

        public Map<String, Optional<?>> asTooltipStats() {
            return Map.of(
                    "damage", maxDamage,
                    "range", range,
                    "ink_consumption", inkConsumption,
                    "firing_speed", firingSpeed
            );
        }

        private static JsonObject object(JsonObject parent, String name) {
            JsonElement element = parent.get(name);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }

        private static Optional<Float> number(JsonObject object, String name) {
            JsonElement element = object.get(name);
            return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                    ? Optional.of(element.getAsFloat())
                    : Optional.empty();
        }

        private static Optional<Integer> integer(JsonObject object, String name) {
            JsonElement element = object.get(name);
            return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                    ? Optional.of(element.getAsNumber().intValue())
                    : Optional.empty();
        }

        private static Optional<Boolean> bool(JsonObject object, String name) {
            JsonElement element = object.get(name);
            return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                    ? Optional.of(element.getAsBoolean())
                    : Optional.empty();
        }

        private static Optional<Float> max(Optional<Float> first, Optional<Float> second) {
            if (first.isEmpty()) {
                return second;
            }
            if (second.isEmpty()) {
                return first;
            }
            return Optional.of(Math.max(first.get(), second.get()));
        }

        private static Optional<Float> min(Optional<Float> first, Optional<Float> second) {
            if (first.isEmpty()) {
                return second;
            }
            if (second.isEmpty()) {
                return first;
            }
            return Optional.of(Math.min(first.get(), second.get()));
        }

        private static Optional<Float> fallback(Optional<Float> value, Optional<Float> fallback) {
            return value.isPresent() ? value : fallback;
        }
    }
}
