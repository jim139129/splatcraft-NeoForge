package net.splatcraft.neoforge.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.item.SubWeaponItem;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;

public final class SingleUseSubRecipe extends CustomRecipe {
    public static final MapCodec<SingleUseSubRecipe> CODEC = CraftingBookCategory.CODEC
            .fieldOf("category")
            .orElse(CraftingBookCategory.MISC)
            .xmap(SingleUseSubRecipe::new, SingleUseSubRecipe::category);

    public static final StreamCodec<RegistryFriendlyByteBuf, SingleUseSubRecipe> STREAM_CODEC = StreamCodec.composite(
            CraftingBookCategory.STREAM_CODEC,
            SingleUseSubRecipe::category,
            SingleUseSubRecipe::new
    );

    public SingleUseSubRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int subWeapons = 0;
        int inkwells = 0;
        int sardinium = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(SplatcraftTags.Items.SUB_WEAPONS)) {
                subWeapons++;
            } else if (stack.is(SplatcraftItems.INKWELL.get())) {
                inkwells++;
            } else if (stack.is(SplatcraftItems.SARDINIUM.get())) {
                sardinium++;
            } else {
                return false;
            }

            if (subWeapons > 1 || inkwells > 1 || sardinium > 1) {
                return false;
            }
        }

        return subWeapons == 1 && inkwells == 1 && sardinium == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack subWeapon = ItemStack.EMPTY;
        int color = 0xFFFFFF;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(SplatcraftTags.Items.SUB_WEAPONS)) {
                subWeapon = stack;
            } else if (stack.is(SplatcraftItems.INKWELL.get())) {
                color = InkColorComponent.color(stack).orElse(color);
            }
        }

        if (subWeapon.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = subWeapon.copyWithCount(1);
        SubWeaponItem.setSingleUse(result, color, true);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(SplatcraftItems.INKWELL.get())) {
                remainingItems.set(slot, new ItemStack(SplatcraftItems.EMPTY_INKWELL.get()));
            } else if (stack.is(SplatcraftTags.Items.SUB_WEAPONS)) {
                remainingItems.set(slot, stack.copyWithCount(1));
            }
        }

        return remainingItems;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SplatcraftRecipeSerializers.SINGLE_USE_SUB.get();
    }

    public static final class Serializer implements RecipeSerializer<SingleUseSubRecipe> {
        @Override
        public MapCodec<SingleUseSubRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SingleUseSubRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
