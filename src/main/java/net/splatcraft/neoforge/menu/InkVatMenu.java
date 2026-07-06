package net.splatcraft.neoforge.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.splatcraft.neoforge.blockentity.InkVatBlockEntity;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.item.FilterItem;
import net.splatcraft.neoforge.network.payload.UpdateBlockColorPayload;
import net.splatcraft.neoforge.recipe.InkVatColorRecipe;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftMenuTypes;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;
import net.splatcraft.neoforge.registry.SplatcraftStats;

public class InkVatMenu extends AbstractContainerMenu {
    private static final int VAT_SLOT_COUNT = 5;
    private static final int PLAYER_SLOT_START = VAT_SLOT_COUNT;

    private final Container container;
    private final ContainerLevelAccess access;
    private final InkVatBlockEntity inkVat;
    private List<Integer> recipes = new ArrayList<>();
    private List<Integer> sortedRecipeIndexes = new ArrayList<>();

    public InkVatMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolveContainer(playerInventory, buffer.readBlockPos()));
    }

    public InkVatMenu(int containerId, Inventory playerInventory, InkVatBlockEntity inkVat) {
        super(SplatcraftMenuTypes.INK_VAT.get(), containerId);
        checkContainerSize(inkVat, VAT_SLOT_COUNT);
        this.container = inkVat;
        this.inkVat = inkVat;
        this.access = ContainerLevelAccess.create(inkVat.getLevel(), inkVat.getBlockPos());
        addVatSlots(playerInventory);
        addPlayerInventorySlots(playerInventory, 8, 126);
        refreshRecipes(false);
    }

    private InkVatMenu(int containerId, Inventory playerInventory, Container fallbackContainer) {
        super(SplatcraftMenuTypes.INK_VAT.get(), containerId);
        checkContainerSize(fallbackContainer, VAT_SLOT_COUNT);
        this.container = fallbackContainer;
        this.inkVat = fallbackContainer instanceof InkVatBlockEntity blockEntity ? blockEntity : null;
        this.access = inkVat == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(inkVat.getLevel(), inkVat.getBlockPos());
        addVatSlots(playerInventory);
        addPlayerInventorySlots(playerInventory, 8, 126);
        refreshRecipes(false);
    }

    private void addVatSlots(Inventory playerInventory) {
        addSlot(new RefreshingItemSlot(container, 0, 26, 70, new ItemStack(Items.INK_SAC)));
        addSlot(new RefreshingItemSlot(container, 1, 46, 70, new ItemStack(SplatcraftItems.POWER_EGG.get())));
        addSlot(new RefreshingItemSlot(container, 2, 92, 82, new ItemStack(SplatcraftItems.EMPTY_INKWELL.get())));
        addSlot(new FilterSlot(container, 3, 36, 89));
        addSlot(new OutputSlot(container, 4, 112, 82));
    }

    public static List<Integer> availableRecipeColors(InkVatBlockEntity inkVat) {
        if (!hasIngredients(inkVat) || inkVat.getLevel() == null) {
            return Collections.emptyList();
        }

        List<Integer> colors = new ArrayList<>();
        RecipeInput input = new ContainerRecipeInput(inkVat);
        boolean hasOmniFilter = inkVat.getItem(3).getItem() instanceof FilterItem filter && filter.isOmni();
        for (var holder : inkVat.getLevel().getRecipeManager().getAllRecipesFor(SplatcraftRecipeSerializers.INK_VAT_COLOR_TYPE.get())) {
            InkVatColorRecipe recipe = holder.value();
            if (hasOmniFilter) {
                if (!recipe.notOnOmniFilter()) {
                    addColorIfAbsent(colors, recipe.color().resolve());
                }
            } else if (recipe.matches(input, inkVat.getLevel())) {
                addColorIfAbsent(colors, recipe.color().resolve());
            }
        }
        if (hasOmniFilter) {
            InkColorData.builtInColors().values().forEach(color -> addColorIfAbsent(colors, OptionalInt.of(color)));
        }
        return colors;
    }

    private static void addColorIfAbsent(List<Integer> colors, OptionalInt color) {
        if (color.isPresent() && !colors.contains(color.getAsInt())) {
            colors.add(color.getAsInt());
        }
    }

    public static boolean hasIngredients(Container container) {
        return !container.getItem(0).isEmpty() && !container.getItem(1).isEmpty() && !container.getItem(2).isEmpty();
    }

    public static void updateOutput(InkVatBlockEntity inkVat) {
        if (inkVat.getLevel() == null) {
            return;
        }

        if (hasIngredients(inkVat) && inkVat.getColor() != -1) {
            int count = inkVat.getCraftableOutputCount();
            ItemStack output = new ItemStack(SplatcraftItems.INKWELL.get(), Math.max(0, count));
            InkColorComponent.setColorAndLock(output, inkVat.getColor(), true);
            setOutputIfChanged(inkVat, output);
        } else {
            setOutputIfChanged(inkVat, ItemStack.EMPTY);
        }
    }

    private static void setOutputIfChanged(InkVatBlockEntity inkVat, ItemStack output) {
        ItemStack current = inkVat.getItem(4);
        if (current.getCount() != output.getCount() || !ItemStack.isSameItemSameComponents(current, output)) {
            inkVat.setItem(4, output);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (inkVat == null || !isIndexInBounds(id)) {
            return false;
        }
        int color = recipes.get(id);
        if (inkVat.getLevel() != null && inkVat.getLevel().isClientSide()) {
            UpdateBlockColorPayload.sendToServer(inkVat.getBlockPos(), color, id);
        }
        inkVat.setPointer(id);
        inkVat.setColor(color);
        updateOutput(inkVat);
        broadcastChanges();
        return true;
    }

    public List<Integer> getSortedRecipeIndexes() {
        return sortedRecipeIndexes;
    }

    public List<Integer> getRecipeColors() {
        return recipes;
    }

    public int getSelectedRecipeIndex() {
        return inkVat == null ? -1 : inkVat.getPointer();
    }

    public boolean hasColorOptions() {
        return !recipes.isEmpty();
    }

    public boolean isFor(InkVatBlockEntity inkVat) {
        return this.inkVat == inkVat;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SplatcraftBlocks.INK_VAT.get());
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
        if (index == 4) {
            if (inkVat == null) {
                return ItemStack.EMPTY;
            }
            int craftable = Math.min(stack.getCount(), inkVat.getCraftableOutputCount());
            if (craftable <= 0) {
                return ItemStack.EMPTY;
            }

            stack.setCount(craftable);
            copy = stack.copy();
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            int crafted = copy.getCount() - stack.getCount();
            if (crafted <= 0) {
                return ItemStack.EMPTY;
            }
            if (inkVat != null && !inkVat.consumeIngredients(crafted)) {
                return ItemStack.EMPTY;
            }

            awardInkwellsCrafted(player, crafted);
            refreshRecipes(false);
            return copy;
        } else if (index < VAT_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, VAT_SLOT_COUNT - 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return copy;
    }

    private static void awardInkwellsCrafted(Player player, int crafted) {
        if (crafted > 0) {
            player.awardStat(Stats.CUSTOM.get(SplatcraftStats.INKWELLS_CRAFTED), crafted);
        }
    }

    public void refreshRecipes() {
        refreshRecipes(true);
    }

    private void refreshRecipes(boolean resetSelection) {
        if (inkVat == null) {
            recipes = Collections.emptyList();
            sortedRecipeIndexes = Collections.emptyList();
            return;
        }
        recipes = availableRecipeColors(inkVat);
        sortedRecipeIndexes = sortedRecipeIndexes(recipes);
        inkVat.setRecipeEntries(recipes.size());
        if (resetSelection || !isIndexInBounds(inkVat.getPointer())) {
            inkVat.setPointer(-1);
            inkVat.setColor(-1);
        }
        updateOutput(inkVat);
    }

    private boolean isIndexInBounds(int index) {
        return index >= 0 && index < recipes.size();
    }

    public static List<Integer> sortedRecipeIndexes(List<Integer> colors) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < colors.size(); index++) {
            indexes.add(index);
        }
        indexes.sort((left, right) -> compareColors(colors.get(left), colors.get(right)));
        return indexes;
    }

    private static int compareColors(int left, int right) {
        int leftOrder = InkColorData.builtInOrder(left);
        int rightOrder = InkColorData.builtInOrder(right);
        if (leftOrder != rightOrder) {
            return Integer.compare(leftOrder, rightOrder);
        }
        return Integer.compare(left, right);
    }

    private static Container resolveContainer(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof InkVatBlockEntity inkVat) {
            return inkVat;
        }
        return new SimpleContainer(VAT_SLOT_COUNT);
    }

    private void addPlayerInventorySlots(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private record ContainerRecipeInput(Container container) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return container.getItem(index);
        }

        @Override
        public int size() {
            return container.getContainerSize();
        }
    }

    private final class FilterSlot extends Slot {
        private FilterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(SplatcraftTags.Items.FILTERS);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            refreshRecipes();
        }
    }

    private final class RefreshingItemSlot extends ItemSlot {
        private RefreshingItemSlot(Container container, int slot, int x, int y, ItemStack validItem) {
            super(container, slot, x, y, validItem);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            refreshRecipes();
        }
    }

    private static class ItemSlot extends Slot {
        private final ItemStack validItem;

        private ItemSlot(Container container, int slot, int x, int y, ItemStack validItem) {
            super(container, slot, x, y);
            this.validItem = validItem;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ItemStack.isSameItem(stack, validItem);
        }
    }

    private final class OutputSlot extends Slot {
        private int removeCount;

        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack removed = super.remove(amount);
            if (!removed.isEmpty()) {
                removeCount += removed.getCount();
            }
            return removed;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            int crafted = removeCount;
            removeCount = 0;
            if (crafted <= 0) {
                crafted = stack.getCount();
            }
            if (crafted > 0) {
                awardInkwellsCrafted(player, crafted);
                refreshRecipes(false);
            }
        }
    }
}
