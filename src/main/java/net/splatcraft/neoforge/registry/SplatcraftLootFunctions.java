package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.loot.BlueprintLootFunction;

public final class SplatcraftLootFunctions {
    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Splatcraft.MOD_ID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<BlueprintLootFunction>> BLUEPRINT_POOL =
            LOOT_FUNCTIONS.register("blueprint_pool", () -> new LootItemFunctionType<>(BlueprintLootFunction.CODEC));

    private SplatcraftLootFunctions() {
    }

    public static void register(IEventBus eventBus) {
        LOOT_FUNCTIONS.register(eventBus);
    }
}
