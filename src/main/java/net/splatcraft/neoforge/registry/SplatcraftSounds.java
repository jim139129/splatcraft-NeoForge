package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;

public final class SplatcraftSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Splatcraft.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> INKED_BLOCK_BREAK = register("block.inked_block.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> INKED_BLOCK_STEP = register("block.inked_block.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> INKED_BLOCK_SWIM = register("block.inked_block.swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> INKED_BLOCK_PLACE = register("block.inked_block.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> INKED_BLOCK_HIT = register("block.inked_block.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> INKED_BLOCK_FALL = register("block.inked_block.fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_TRANSFORM = register("squid_transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_REVERT = register("squid_revert");
    public static final DeferredHolder<SoundEvent, SoundEvent> INK_SUBMERGE = register("ink_submerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> INK_SURFACE = register("ink_surface");
    public static final DeferredHolder<SoundEvent, SoundEvent> NO_INK = register("no_ink");
    public static final DeferredHolder<SoundEvent, SoundEvent> NO_INK_SUB = register("no_ink_sub");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOOTER_FIRING = register("shooter_firing");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLASTER_FIRING = register("blaster_firing");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLASTER_EXPLOSION = register("blaster_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROLLER_FLING = register("roller_fling");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROLLER_ROLL = register("roller_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> BRUSH_FLING = register("brush_fling");
    public static final DeferredHolder<SoundEvent, SoundEvent> BRUSH_ROLL = register("brush_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGER_CHARGE = register("charger_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGER_READY = register("charger_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGER_SHOT = register("charger_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUALIE_FIRING = register("dualie_firing");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUALIE_DODGE = register("dualie_dodge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLOSHER_SHOT = register("slosher_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPLATLING_CHARGE = register("splatling_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPLATLING_CHARGE_SECOND_LEVEL = register("splatling_charge_second_level");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPLATLING_READY = register("splatling_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPLATLING_FIRING = register("splatling_firing");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUB_THROW = register("sub_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUB_DETONATING = register("sub_detonating");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUB_DETONATE = register("sub_detonate");
    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_EGG_CAN_OPEN = register("power_egg_can_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_BUMPER_PLACE = register("squid_bumper_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_BUMPER_POP = register("squid_bumper_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_BUMPER_RESPAWNING = register("squid_bumper_respawning");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_BUMPER_READY = register("squid_bumper_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_BUMPER_HIT = register("squid_bumper_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_BUMPER_INK = register("squid_bumper_ink");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUID_BUMPER_BREAK = register("squid_bumper_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPLAT_SWITCH_POWERED_ON = register("splat_switch_powered_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPLAT_SWITCH_POWERED_OFF = register("splat_switch_powered_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> REMOTE_USE = register("remote_use");

    public static final SoundType SOUND_TYPE_INK = new DeferredSoundType(
            1.0F,
            1.0F,
            INKED_BLOCK_BREAK,
            INKED_BLOCK_STEP,
            INKED_BLOCK_PLACE,
            INKED_BLOCK_HIT,
            INKED_BLOCK_FALL);
    public static final SoundType SOUND_TYPE_SWIMMING = new DeferredSoundType(
            1.0F,
            1.0F,
            INKED_BLOCK_BREAK,
            INKED_BLOCK_SWIM,
            INKED_BLOCK_PLACE,
            INKED_BLOCK_HIT,
            INKED_BLOCK_FALL);

    private SplatcraftSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name)
        ));
    }
}
