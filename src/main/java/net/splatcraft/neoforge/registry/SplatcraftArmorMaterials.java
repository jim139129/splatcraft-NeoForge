package net.splatcraft.neoforge.registry;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;

public final class SplatcraftArmorMaterials {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Splatcraft.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> INK_CLOTH =
            register("ink_cloth", 0, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.EMPTY);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> INK_TANK =
            register("ink_tank", 0, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, () -> Ingredient.EMPTY);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CLASSIC_INK_TANK =
            register("classic_ink_tank", 0, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, () -> Ingredient.EMPTY);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> INK_TANK_JR =
            register("ink_tank_jr", 0, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, () -> Ingredient.EMPTY);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORED_INK_TANK =
            register("armored_ink_tank", 3, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.05F, () -> Ingredient.EMPTY);

    private SplatcraftArmorMaterials() {
    }

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(
            String name,
            int defense,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngredient
    ) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                defense(defense),
                0,
                equipSound,
                repairIngredient,
                List.of(
                        new ArmorMaterial.Layer(id(name), "", true),
                        new ArmorMaterial.Layer(id(name), "_overlay", false)
                ),
                toughness,
                knockbackResistance
        ));
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int value) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, value);
        }
        return defense;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, path);
    }
}
