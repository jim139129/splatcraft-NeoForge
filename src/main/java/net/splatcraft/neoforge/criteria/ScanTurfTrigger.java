package net.splatcraft.neoforge.criteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class ScanTurfTrigger extends SimpleCriterionTrigger<ScanTurfTrigger.TriggerInstance> {
    @Override
    public Codec<ScanTurfTrigger.TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int blocksInked, boolean winner) {
        this.trigger(player, instance -> instance.matches(blocksInked, winner));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, int blocksInked, boolean winner)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ScanTurfTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                        .forGetter(ScanTurfTrigger.TriggerInstance::player),
                                Codec.INT.optionalFieldOf("blocks_inked", 0)
                                        .forGetter(ScanTurfTrigger.TriggerInstance::blocksInked),
                                Codec.BOOL.optionalFieldOf("winner", false)
                                        .forGetter(ScanTurfTrigger.TriggerInstance::winner))
                        .apply(instance, ScanTurfTrigger.TriggerInstance::new));

        public boolean matches(int blocksInked, boolean winner) {
            return blocksInked >= this.blocksInked && (winner || !this.winner);
        }
    }
}
