package net.splatcraft.neoforge.mixin;

import java.util.Map;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ObjectiveCriteria.class)
public interface ObjectiveCriteriaAccessor {
    @Accessor("CRITERIA_CACHE")
    static Map<String, ObjectiveCriteria> splatcraft$getCriteriaCache() {
        throw new AssertionError();
    }
}
