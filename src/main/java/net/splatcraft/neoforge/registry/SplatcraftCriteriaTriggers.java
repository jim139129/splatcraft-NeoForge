package net.splatcraft.neoforge.registry;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.criteria.ChangeInkColorTrigger;
import net.splatcraft.neoforge.criteria.CraftWeaponTrigger;
import net.splatcraft.neoforge.criteria.FallIntoInkTrigger;
import net.splatcraft.neoforge.criteria.ScanTurfTrigger;

public final class SplatcraftCriteriaTriggers {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Splatcraft.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, CraftWeaponTrigger> CRAFT_WEAPON =
            TRIGGERS.register("craft_weapon", CraftWeaponTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, ChangeInkColorTrigger> CHANGE_INK_COLOR =
            TRIGGERS.register("change_ink_color", ChangeInkColorTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, ScanTurfTrigger> SCAN_TURF =
            TRIGGERS.register("scan_turf", ScanTurfTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, FallIntoInkTrigger> FALL_INTO_INK =
            TRIGGERS.register("fall_into_ink", FallIntoInkTrigger::new);

    private SplatcraftCriteriaTriggers() {
    }

    public static void register(IEventBus eventBus) {
        TRIGGERS.register(eventBus);
    }
}
