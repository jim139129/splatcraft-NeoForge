package net.splatcraft.neoforge.criteria;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public class ChangeInkColorTrigger extends SimpleCriterionTrigger<ChangeInkColorTrigger.TriggerInstance> {
    @Override
    public Codec<ChangeInkColorTrigger.TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> instance.color().isEmpty());
    }

    public void trigger(ServerPlayer player, String colorId) {
        this.trigger(player, instance -> instance.matches(colorId));
    }

    public void trigger(ServerPlayer player, int color) {
        int clampedColor = Mth.clamp(color, 0, 0xFFFFFF);
        this.trigger(player, instance -> instance.matches(clampedColor));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ColorValue> color)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ChangeInkColorTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                        .forGetter(ChangeInkColorTrigger.TriggerInstance::player),
                                ColorValue.CODEC.optionalFieldOf("color")
                                        .forGetter(ChangeInkColorTrigger.TriggerInstance::color))
                        .apply(instance, ChangeInkColorTrigger.TriggerInstance::new));

        public boolean matches(String colorId) {
            return this.color.isEmpty() || this.color.get().matches(colorId);
        }

        public boolean matches(int color) {
            return this.color.isEmpty() || this.color.get().matches(color);
        }
    }

    public record ColorValue(Optional<String> id, Optional<Integer> rgb) {
        public static final Codec<ColorValue> CODEC = Codec.either(Codec.STRING, Codec.INT).xmap(
                either -> either.map(ColorValue::fromString, ColorValue::fromInt),
                value -> value.id()
                        .<Either<String, Integer>>map(Either::left)
                        .orElseGet(() -> Either.right(value.rgb().orElse(0))));

        private static ColorValue fromString(String value) {
            Optional<Integer> rgb = parseHexColor(value);
            return new ColorValue(Optional.of(value), rgb);
        }

        private static ColorValue fromInt(int value) {
            return new ColorValue(Optional.empty(), Optional.of(Mth.clamp(value, 0, 0xFFFFFF)));
        }

        public boolean matches(String colorId) {
            if (this.id.isPresent() && this.id.get().equals(colorId)) {
                return true;
            }

            Optional<Integer> otherColor = parseHexColor(colorId);
            return otherColor.isPresent() && this.matches(otherColor.get());
        }

        public boolean matches(int color) {
            return this.rgb.isPresent() && this.rgb.get() == Mth.clamp(color, 0, 0xFFFFFF);
        }

        private static Optional<Integer> parseHexColor(String value) {
            String trimmed = value.trim();
            String hex = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
            if (hex.length() != 6) {
                return Optional.empty();
            }

            for (int i = 0; i < hex.length(); i++) {
                char c = hex.charAt(i);
                if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                    return Optional.empty();
                }
            }

            return Optional.of(Integer.parseInt(hex.toLowerCase(Locale.ROOT), 16));
        }
    }
}
