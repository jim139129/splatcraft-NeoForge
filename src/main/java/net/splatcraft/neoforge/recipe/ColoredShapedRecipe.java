package net.splatcraft.neoforge.recipe;

import com.mojang.serialization.MapCodec;
import java.util.OptionalInt;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;

public final class ColoredShapedRecipe extends ShapedRecipe {
    public static final MapCodec<ColoredShapedRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(
            recipe -> new ColoredShapedRecipe(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification()),
            recipe -> recipe
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ColoredShapedRecipe> STREAM_CODEC = ShapedRecipe.Serializer.STREAM_CODEC.map(
            recipe -> new ColoredShapedRecipe(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification()),
            recipe -> recipe
    );

    public ColoredShapedRecipe(String group, net.minecraft.world.item.crafting.CraftingBookCategory category,
                               net.minecraft.world.item.crafting.ShapedRecipePattern pattern, ItemStack result,
                               boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int colorSum = 0;
        int coloredInputs = 0;
        int currentColor = -1;
        boolean colorLocked = false;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.is(SplatcraftItems.INKWELL.get())) {
                continue;
            }

            OptionalInt color = InkColorComponent.color(stack);
            if (color.isEmpty()) {
                continue;
            }

            colorSum += color.getAsInt();
            coloredInputs++;
            if (InkColorComponent.isColorLocked(stack)) {
                colorLocked = true;
            } else {
                currentColor = color.getAsInt();
            }
        }

        int resultColor = coloredInputs == 0 ? -1 : colorLocked ? colorSum / coloredInputs : currentColor;
        return InkColorComponent.setColorAndLock(super.assemble(input, registries), resultColor, colorLocked);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SplatcraftRecipeSerializers.COLORED_CRAFTING_SHAPED.get();
    }

    public static final class Serializer implements RecipeSerializer<ColoredShapedRecipe> {
        @Override
        public MapCodec<ColoredShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ColoredShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
