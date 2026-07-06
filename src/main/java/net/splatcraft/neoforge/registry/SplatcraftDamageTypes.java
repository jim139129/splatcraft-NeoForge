package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.splatcraft.neoforge.Splatcraft;

public final class SplatcraftDamageTypes {
    public static final ResourceKey<DamageType> ENEMY_INK = create("enemy_ink");
    public static final ResourceKey<DamageType> SPLAT = create("splat");
    public static final ResourceKey<DamageType> ROLL = create("roll");
    public static final ResourceKey<DamageType> WATER = create("water");
    public static final ResourceKey<DamageType> OUT_OF_STAGE = create("out_of_stage");

    private SplatcraftDamageTypes() {
    }

    private static ResourceKey<DamageType> create(String name) {
        return ResourceKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name));
    }
}
