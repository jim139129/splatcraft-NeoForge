package net.splatcraft.neoforge.registry;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.loot.ChestLootModifier;
import net.splatcraft.neoforge.loot.FishingLootModifier;

public final class SplatcraftLootModifiers {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Splatcraft.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ChestLootModifier>> CHEST_LOOT =
            SERIALIZERS.register("chest_loot", () -> ChestLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<FishingLootModifier>> FISHING =
            SERIALIZERS.register("fishing", () -> FishingLootModifier.CODEC);

    private SplatcraftLootModifiers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
