package net.splatcraft.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;

public record InkVatColorRecipe(Ingredient filter, ColorValue color, boolean notOnOmniFilter) implements Recipe<RecipeInput> {
    public static final MapCodec<InkVatColorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.optionalFieldOf("filter", Ingredient.EMPTY).forGetter(InkVatColorRecipe::filter),
            ColorValue.CODEC.fieldOf("color").forGetter(InkVatColorRecipe::color),
            Codec.BOOL.optionalFieldOf("not_on_omni_filter", false).forGetter(InkVatColorRecipe::notOnOmniFilter)
    ).apply(instance, InkVatColorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InkVatColorRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.filter);
                buffer.writeUtf(recipe.color.rawValue());
                buffer.writeBoolean(recipe.notOnOmniFilter);
            },
            buffer -> new InkVatColorRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), new ColorValue(buffer.readUtf()), buffer.readBoolean())
    );

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return input.size() > 3 && filter.test(input.getItem(3));
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(SplatcraftBlocks.INKWELL.get());
        int resolvedColor = InkColorData.resolve(color.rawValue()).orElse(InkColorData.DEFAULT_COLOR);
        InkColorComponent.setColor(result, resolvedColor);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(SplatcraftBlocks.INKWELL.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SplatcraftRecipeSerializers.INK_VAT_COLOR.get();
    }

    @Override
    public RecipeType<?> getType() {
        return SplatcraftRecipeSerializers.INK_VAT_COLOR_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(SplatcraftBlocks.INK_VAT.get());
    }

    public record ColorValue(String rawValue) {
        public static final Codec<ColorValue> CODEC = Codec.either(Codec.INT, Codec.STRING)
                .xmap(value -> value.map(integer -> new ColorValue(Integer.toString(integer)), ColorValue::new),
                        color -> {
                            try {
                                return com.mojang.datafixers.util.Either.left(Integer.parseInt(color.rawValue));
                            } catch (NumberFormatException ignored) {
                                return com.mojang.datafixers.util.Either.right(color.rawValue);
                            }
                        });

        public OptionalInt resolve() {
            return InkColorData.resolve(rawValue);
        }
    }

    public static final class Serializer implements RecipeSerializer<InkVatColorRecipe> {
        @Override
        public MapCodec<InkVatColorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InkVatColorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
