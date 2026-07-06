package net.splatcraft.neoforge.mixin;

import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientRecipeBook.class)
public class RecipeCategoryMixin {
    @Inject(method = "getCategory", at = @At("HEAD"), cancellable = true)
    private static void splatcraft$getCategory(
            RecipeHolder<?> recipeHolder,
            CallbackInfoReturnable<RecipeBookCategories> callback
    ) {
        RecipeType<?> type = recipeHolder.value().getType();
        if (type == SplatcraftRecipeSerializers.INK_VAT_COLOR_TYPE.get()
                || type == SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TAB_TYPE.get()
                || type == SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TYPE.get()) {
            callback.setReturnValue(RecipeBookCategories.UNKNOWN);
        }
    }
}
