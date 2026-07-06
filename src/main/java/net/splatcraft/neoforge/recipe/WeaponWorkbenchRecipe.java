package net.splatcraft.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;

public record WeaponWorkbenchRecipe(ResourceLocation tab, int pos, List<Subtype> recipes) implements Recipe<RecipeInput> {
    public static final MapCodec<WeaponWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("tab").forGetter(WeaponWorkbenchRecipe::tab),
            Codec.INT.optionalFieldOf("pos", Integer.MAX_VALUE).forGetter(WeaponWorkbenchRecipe::pos),
            Subtype.CODEC.listOf().fieldOf("recipes").forGetter(WeaponWorkbenchRecipe::recipes)
    ).apply(instance, WeaponWorkbenchRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                buffer.writeResourceLocation(recipe.tab);
                buffer.writeInt(recipe.pos);
                buffer.writeVarInt(recipe.recipes.size());
                recipe.recipes.forEach(subtype -> Subtype.STREAM_CODEC.encode(buffer, subtype));
            },
            buffer -> new WeaponWorkbenchRecipe(
                    buffer.readResourceLocation(),
                    buffer.readInt(),
                    ByteBufCodecs.collection(java.util.ArrayList::new, Subtype.STREAM_CODEC).decode(buffer))
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
        return recipes.isEmpty() ? ItemStack.EMPTY : recipes.getFirst().result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SplatcraftRecipeSerializers.WEAPON_WORKBENCH.get();
    }

    @Override
    public RecipeType<?> getType() {
        return SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record StackedIngredient(Ingredient ingredient, int count) {
        private static final Codec<StackedIngredient> FLAT_CODEC = SizedIngredient.FLAT_CODEC
                .xmap(ingredient -> new StackedIngredient(ingredient.ingredient(), ingredient.count()),
                        ingredient -> new SizedIngredient(ingredient.ingredient(), ingredient.count()));
        private static final Codec<StackedIngredient> NESTED_CODEC = SizedIngredient.NESTED_CODEC
                .xmap(ingredient -> new StackedIngredient(ingredient.ingredient(), ingredient.count()),
                        ingredient -> new SizedIngredient(ingredient.ingredient(), ingredient.count()));
        public static final Codec<StackedIngredient> CODEC = NeoForgeExtraCodecs.withAlternative(FLAT_CODEC, NESTED_CODEC);

        public static final StreamCodec<RegistryFriendlyByteBuf, StackedIngredient> STREAM_CODEC = StreamCodec.of(
                (buffer, ingredient) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.ingredient);
                    buffer.writeVarInt(ingredient.count);
                },
                buffer -> new StackedIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt())
        );

        public boolean matches(ItemStack stack) {
            return stack.getCount() >= count && ingredient.test(stack);
        }

        public ItemStack displayStack(int tickTime) {
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length == 0) {
                return ItemStack.EMPTY;
            }
            return stacks[Math.floorDiv(tickTime, 20) % stacks.length].copyWithCount(count);
        }
    }

    public record Subtype(ItemStack result, List<StackedIngredient> ingredients, ComponentSerialization.ComponentHolder name,
                          Optional<ResourceLocation> advancement, boolean requiresOther) {
        public static final Codec<Subtype> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Subtype::result),
                StackedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(Subtype::ingredients),
                ComponentSerialization.HOLDER_CODEC.optionalFieldOf("name", ComponentSerialization.ComponentHolder.empty()).forGetter(Subtype::name),
                ResourceLocation.CODEC.optionalFieldOf("advancement").forGetter(Subtype::advancement),
                Codec.BOOL.optionalFieldOf("requires_other", false).forGetter(Subtype::requiresOther)
        ).apply(instance, Subtype::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Subtype> STREAM_CODEC = StreamCodec.of(
                (buffer, subtype) -> {
                    ItemStack.STREAM_CODEC.encode(buffer, subtype.result);
                    buffer.writeVarInt(subtype.ingredients.size());
                    subtype.ingredients.forEach(ingredient -> StackedIngredient.STREAM_CODEC.encode(buffer, ingredient));
                    ComponentSerialization.HOLDER_STREAM_CODEC.encode(buffer, subtype.name);
                    buffer.writeBoolean(subtype.advancement.isPresent());
                    subtype.advancement.ifPresent(buffer::writeResourceLocation);
                    buffer.writeBoolean(subtype.requiresOther);
                },
                buffer -> {
                    ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                    int count = buffer.readVarInt();
                    java.util.ArrayList<StackedIngredient> ingredients = new java.util.ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        ingredients.add(StackedIngredient.STREAM_CODEC.decode(buffer));
                    }
                    ComponentSerialization.ComponentHolder name = ComponentSerialization.HOLDER_STREAM_CODEC.decode(buffer);
                    Optional<ResourceLocation> advancement = buffer.readBoolean() ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
                    return new Subtype(result, List.copyOf(ingredients), name, advancement, buffer.readBoolean());
                }
        );

        public boolean matches(RecipeInput input) {
            List<ItemStack> remaining = new ArrayList<>();
            for (int slot = 0; slot < input.size(); slot++) {
                ItemStack stack = input.getItem(slot);
                if (!stack.isEmpty()) {
                    remaining.add(stack);
                }
            }

            for (StackedIngredient ingredient : ingredients) {
                boolean matched = false;
                for (int i = 0; i < remaining.size(); i++) {
                    ItemStack stack = remaining.get(i);
                    if (ingredient.matches(stack)) {
                        remaining.remove(i);
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    return false;
                }
            }

            return remaining.isEmpty();
        }

        public boolean isAvailable(Player player, List<Subtype> siblings) {
            if (requiresOther) {
                for (Subtype sibling : siblings) {
                    if (sibling != this && !sibling.isAvailable(player, List.of())) {
                        return false;
                    }
                }
            }

            if (advancement.isEmpty()) {
                return true;
            }

            if (player instanceof ServerPlayer serverPlayer) {
                AdvancementHolder holder = serverPlayer.getServer().getAdvancements().get(advancement.get());
                return holder == null || serverPlayer.getAdvancements().getOrStartProgress(holder).isDone();
            }

            return true;
        }

        public ItemStack assemble() {
            return result.copy();
        }
    }

    public List<Subtype> availableRecipes(Player player) {
        return recipes.stream()
                .filter(recipe -> recipe.isAvailable(player, recipes))
                .toList();
    }

    public static final class Serializer implements RecipeSerializer<WeaponWorkbenchRecipe> {
        @Override
        public MapCodec<WeaponWorkbenchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
