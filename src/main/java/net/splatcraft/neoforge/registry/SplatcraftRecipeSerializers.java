package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.recipe.ColoredShapedRecipe;
import net.splatcraft.neoforge.recipe.InkVatColorRecipe;
import net.splatcraft.neoforge.recipe.SingleUseSubRecipe;
import net.splatcraft.neoforge.recipe.WeaponWorkbenchRecipe;
import net.splatcraft.neoforge.recipe.WeaponWorkbenchTabRecipe;

public final class SplatcraftRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Splatcraft.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Splatcraft.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InkVatColorRecipe>> INK_VAT_COLOR =
            SERIALIZERS.register("ink_vat_color", InkVatColorRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WeaponWorkbenchTabRecipe>> WEAPON_WORKBENCH_TAB =
            SERIALIZERS.register("weapon_workbench_tab", WeaponWorkbenchTabRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WeaponWorkbenchRecipe>> WEAPON_WORKBENCH =
            SERIALIZERS.register("weapon_workbench", WeaponWorkbenchRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColoredShapedRecipe>> COLORED_CRAFTING_SHAPED =
            SERIALIZERS.register("colored_crafting_shaped", ColoredShapedRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SingleUseSubRecipe>> SINGLE_USE_SUB =
            SERIALIZERS.register("single_use_sub", SingleUseSubRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<InkVatColorRecipe>> INK_VAT_COLOR_TYPE =
            TYPES.register("ink_vat_color", () -> recipeType("ink_vat_color"));
    public static final DeferredHolder<RecipeType<?>, RecipeType<WeaponWorkbenchTabRecipe>> WEAPON_WORKBENCH_TAB_TYPE =
            TYPES.register("weapon_workbench_tab", () -> recipeType("weapon_workbench_tab"));
    public static final DeferredHolder<RecipeType<?>, RecipeType<WeaponWorkbenchRecipe>> WEAPON_WORKBENCH_TYPE =
            TYPES.register("weapon_workbench", () -> recipeType("weapon_workbench"));

    private SplatcraftRecipeSerializers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }

    private static <T extends Recipe<?>> RecipeType<T> recipeType(String name) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return Splatcraft.MOD_ID + ":" + name;
            }
        };
    }
}
