package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;

public final class SplatcraftAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, Splatcraft.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> INK_SWIM_SPEED =
            ATTRIBUTES.register("ink_swim_speed", () -> new RangedAttribute(
                    "attribute.splatcraft.ink_swim_speed",
                    0.7D,
                    0.0D,
                    1024.0D
            ).setSyncable(true));

    private SplatcraftAttributes() {
    }

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
        eventBus.addListener(SplatcraftAttributes::modifyEntityAttributes);
    }

    private static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, INK_SWIM_SPEED, 0.075D);
    }
}
