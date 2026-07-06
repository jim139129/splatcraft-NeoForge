package net.splatcraft.neoforge.criteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class FallIntoInkTrigger extends SimpleCriterionTrigger<FallIntoInkTrigger.TriggerInstance> {
    @Override
    public Codec<FallIntoInkTrigger.TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, float distance) {
        this.trigger(player, instance -> instance.matches(distance));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, float distance)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<FallIntoInkTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                        .forGetter(FallIntoInkTrigger.TriggerInstance::player),
                                Codec.FLOAT.optionalFieldOf("distance", 0.0F)
                                        .forGetter(FallIntoInkTrigger.TriggerInstance::distance))
                        .apply(instance, FallIntoInkTrigger.TriggerInstance::new));

        public boolean matches(float distance) {
            return distance >= this.distance;
        }
    }
}
