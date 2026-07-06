package net.splatcraft.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;

public record WeaponWorkbenchTabRecipe(ResourceLocation icon, int pos, Component name, boolean hidden) implements Recipe<RecipeInput> {
    public static final MapCodec<WeaponWorkbenchTabRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("icon").forGetter(WeaponWorkbenchTabRecipe::icon),
            Codec.INT.optionalFieldOf("pos", Integer.MAX_VALUE).forGetter(WeaponWorkbenchTabRecipe::pos),
            ComponentSerialization.CODEC.optionalFieldOf("name", Component.empty()).forGetter(WeaponWorkbenchTabRecipe::name),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(WeaponWorkbenchTabRecipe::hidden)
    ).apply(instance, WeaponWorkbenchTabRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchTabRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                buffer.writeResourceLocation(recipe.icon);
                buffer.writeInt(recipe.pos);
                ComponentSerialization.STREAM_CODEC.encode(buffer, recipe.name);
                buffer.writeBoolean(recipe.hidden);
            },
            buffer -> new WeaponWorkbenchTabRecipe(
                    buffer.readResourceLocation(),
                    buffer.readInt(),
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    buffer.readBoolean())
    );

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return true;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TAB.get();
    }

    @Override
    public RecipeType<?> getType() {
        return SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TAB_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<WeaponWorkbenchTabRecipe> {
        @Override
        public MapCodec<WeaponWorkbenchTabRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchTabRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
