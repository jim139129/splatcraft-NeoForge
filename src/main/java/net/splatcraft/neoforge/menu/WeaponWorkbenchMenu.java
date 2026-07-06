package net.splatcraft.neoforge.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.recipe.WeaponWorkbenchRecipe;
import net.splatcraft.neoforge.recipe.WeaponWorkbenchTabRecipe;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftCriteriaTriggers;
import net.splatcraft.neoforge.registry.SplatcraftMenuTypes;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;
import net.splatcraft.neoforge.registry.SplatcraftStats;

public class WeaponWorkbenchMenu extends AbstractContainerMenu {
    public static final int RECIPES_PER_PAGE = 8;
    public static final int INGREDIENTS_PER_PAGE = 8;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_SIZE = PLAYER_INVENTORY_ROWS * PLAYER_INVENTORY_COLUMNS;
    private static final int HOTBAR_START = PLAYER_INVENTORY_SIZE;
    private static final int BUTTON_RECIPE_STRIDE = 256;

    private final ContainerLevelAccess access;

    public WeaponWorkbenchMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), buffer.readBlockPos()));
    }

    public WeaponWorkbenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public WeaponWorkbenchMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(SplatcraftMenuTypes.WEAPON_WORKBENCH.get(), containerId);
        this.access = access;
        addPlayerInventorySlots(playerInventory, 8, 144);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SplatcraftBlocks.AMMO_KNIGHTS_WORKBENCH.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int recipeIndex = Math.floorDiv(id, BUTTON_RECIPE_STRIDE);
        int subtypeIndex = Math.floorMod(id, BUTTON_RECIPE_STRIDE);
        Optional<RecipeHolder<WeaponWorkbenchRecipe>> recipe = recipeAt(player.level(), recipeIndex);
        if (recipe.isEmpty()) {
            return false;
        }

        if (subtypeIndex < 0 || subtypeIndex >= recipe.get().value().recipes().size()) {
            return false;
        }

        WeaponWorkbenchRecipe.Subtype subtype = recipe.get().value().recipes().get(subtypeIndex);
        if (!subtype.isAvailable(player, recipe.get().value().recipes())) {
            return false;
        }

        ItemStack output = subtype.assemble();
        if (output.isEmpty() || !takeIngredients(player, subtype.ingredients())) {
            return false;
        }

        player.getInventory().setChanged();
        if (player instanceof ServerPlayer serverPlayer) {
            SplatcraftCriteriaTriggers.CRAFT_WEAPON.get().trigger(serverPlayer, output.copy());
        }
        player.awardStat(Stats.ITEM_CRAFTED.get(output.getItem()));
        player.awardStat(Stats.CUSTOM.get(SplatcraftStats.WEAPONS_CRAFTED));
        if (!player.addItem(output)) {
            ItemEntity item = player.drop(output, false);
            if (item != null) {
                item.setNoPickUpDelay();
            }
        } else {
            broadcastChanges();
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return copy;
        }

        ItemStack stack = slot.getItem();
        copy = stack.copy();
        if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    public List<RecipeHolder<WeaponWorkbenchTabRecipe>> visibleTabs(Player player) {
        return sortedTabs(player.level()).stream()
                .filter(tab -> !tab.value().hidden() || !recipesForTab(player, tab.id()).isEmpty())
                .toList();
    }

    public List<RecipeHolder<WeaponWorkbenchRecipe>> recipesForTab(Player player, net.minecraft.resources.ResourceLocation tabId) {
        return sortedRecipes(player.level()).stream()
                .filter(recipe -> recipe.value().tab().equals(tabId))
                .filter(recipe -> !recipe.value().availableRecipes(player).isEmpty())
                .toList();
    }

    public Optional<RecipeHolder<WeaponWorkbenchRecipe>> recipeAt(Level level, int recipeIndex) {
        List<RecipeHolder<WeaponWorkbenchRecipe>> recipes = sortedRecipes(level);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return Optional.empty();
        }
        return Optional.of(recipes.get(recipeIndex));
    }

    public int recipeIndex(Level level, RecipeHolder<WeaponWorkbenchRecipe> recipe) {
        List<RecipeHolder<WeaponWorkbenchRecipe>> recipes = sortedRecipes(level);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipe.id())) {
                return index;
            }
        }
        return -1;
    }

    public int craftButtonId(Level level, RecipeHolder<WeaponWorkbenchRecipe> recipe, int subtypeIndex) {
        int recipeIndex = recipeIndex(level, recipe);
        if (recipeIndex < 0 || subtypeIndex < 0 || subtypeIndex >= BUTTON_RECIPE_STRIDE) {
            return -1;
        }
        return recipeIndex * BUTTON_RECIPE_STRIDE + subtypeIndex;
    }

    public boolean hasIngredients(Player player, List<WeaponWorkbenchRecipe.StackedIngredient> ingredients) {
        return consumeIngredients(player, ingredients, false);
    }

    private static List<RecipeHolder<WeaponWorkbenchTabRecipe>> sortedTabs(Level level) {
        return level.getRecipeManager().getAllRecipesFor(SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TAB_TYPE.get()).stream()
                .sorted(Comparator.comparingInt((RecipeHolder<WeaponWorkbenchTabRecipe> recipe) -> recipe.value().pos())
                        .thenComparing(recipe -> recipe.id().toString()))
                .toList();
    }

    private static List<RecipeHolder<WeaponWorkbenchRecipe>> sortedRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TYPE.get()).stream()
                .sorted(Comparator.comparingInt((RecipeHolder<WeaponWorkbenchRecipe> recipe) -> recipe.value().pos())
                        .thenComparing(recipe -> recipe.id().toString()))
                .toList();
    }

    private static boolean takeIngredients(Player player, List<WeaponWorkbenchRecipe.StackedIngredient> ingredients) {
        return consumeIngredients(player, ingredients, true);
    }

    private static boolean consumeIngredients(Player player, List<WeaponWorkbenchRecipe.StackedIngredient> ingredients, boolean takeItems) {
        Inventory inventory = player.getInventory();
        List<ItemStack> simulated = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            simulated.add(inventory.getItem(slot).copy());
        }

        for (WeaponWorkbenchRecipe.StackedIngredient ingredient : ingredients) {
            if (!consumeIngredient(simulated, ingredient.ingredient(), ingredient.count())) {
                return false;
            }
        }

        if (takeItems) {
            for (int slot = 0; slot < simulated.size(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                int remaining = simulated.get(slot).getCount();
                if (stack.getCount() != remaining) {
                    stack.setCount(remaining);
                    if (stack.isEmpty()) {
                        inventory.setItem(slot, ItemStack.EMPTY);
                    }
                }
            }
        }
        return true;
    }

    private static boolean consumeIngredient(List<ItemStack> stacks, Ingredient ingredient, int count) {
        int remaining = count;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addPlayerInventorySlots(Inventory inventory, int left, int top) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
                addSlot(new Slot(inventory, column + row * PLAYER_INVENTORY_COLUMNS + 9, left + column * 18, top + row * 18));
            }
        }

        for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }
}
