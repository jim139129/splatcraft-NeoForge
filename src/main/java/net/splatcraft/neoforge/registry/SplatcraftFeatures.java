package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.worldgen.feature.CrateFeature;
import net.splatcraft.neoforge.worldgen.feature.SardiniumDepositFeature;

public final class SplatcraftFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Splatcraft.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<CountConfiguration>> CRATE =
            FEATURES.register("crate", () -> new CrateFeature(CountConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SARDINIUM_DEPOSIT =
            FEATURES.register("sardinium_deposit", () -> new SardiniumDepositFeature(NoneFeatureConfiguration.CODEC));

    private SplatcraftFeatures() {
    }

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
