package net.splatcraft.neoforge.blockentity;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.menu.InkVatMenu;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;
import net.splatcraft.neoforge.registry.SplatcraftItems;

public class InkVatBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private static final int[] INPUT_SLOTS = new int[]{0, 1, 2, 3};
    private static final int[] OUTPUT_SLOTS = new int[]{4};
    private static final int INK_SAC_SLOT = 0;
    private static final int POWER_EGG_SLOT = 1;
    private static final int EMPTY_INKWELL_SLOT = 2;
    private static final int OUTPUT_SLOT = 4;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
    private int color = -1;
    private int pointer = -1;
    private int recipeEntries;

    public InkVatBlockEntity(BlockPos pos, BlockState state) {
        super(SplatcraftBlockEntities.INK_VAT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InkVatBlockEntity inkVat) {
        InkVatMenu.updateOutput(inkVat);
        if (!level.isClientSide) {
            boolean active = inkVat.hasRecipe();
            if (state.getValue(BlockStateCompatBlocks.ACTIVE) != active) {
                level.setBlock(pos, state.setValue(BlockStateCompatBlocks.ACTIVE, active), 3);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.ink_vat");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < inventory.size(); i++) {
            inventory.set(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Color", color);
        tag.putInt("Pointer", pointer);
        tag.putInt("RecipeEntries", recipeEntries);
        ContainerHelper.saveAllItems(tag, inventory, registries);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = InkColorBlockEntity.readColor(tag, -1);
        pointer = tag.getInt("Pointer");
        recipeEntries = tag.getInt("RecipeEntries");
        inventory.clear();
        ContainerHelper.loadAllItems(tag, inventory, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory) {
        return new InkVatMenu(id, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int index) {
        return inventory.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index == OUTPUT_SLOT) {
            return removeOutput(count);
        }

        ItemStack stack = ContainerHelper.removeItem(inventory, index, count);
        if (!stack.isEmpty()) {
            syncBlockEntity();
        }
        return stack;
    }

    private ItemStack removeOutput(int count) {
        ItemStack output = inventory.get(OUTPUT_SLOT);
        int outputCount = Math.min(count, output.getCount());
        if (outputCount <= 0 || !consumeIngredients(outputCount)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = ContainerHelper.removeItem(inventory, OUTPUT_SLOT, outputCount);
        if (!stack.isEmpty()) {
            syncBlockEntity();
        }
        return stack;
    }

    public boolean consumeIngredients(int count) {
        if (count <= 0
                || inventory.get(INK_SAC_SLOT).getCount() < count
                || inventory.get(POWER_EGG_SLOT).getCount() < count
                || inventory.get(EMPTY_INKWELL_SLOT).getCount() < count) {
            return false;
        }

        ContainerHelper.removeItem(inventory, INK_SAC_SLOT, count);
        ContainerHelper.removeItem(inventory, POWER_EGG_SLOT, count);
        ContainerHelper.removeItem(inventory, EMPTY_INKWELL_SLOT, count);
        syncBlockEntity();
        return true;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(inventory, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        ItemStack previous = inventory.get(index).copy();
        inventory.set(index, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (previous.getCount() != stack.getCount() || !ItemStack.isSameItemSameComponents(previous, stack)) {
            syncBlockEntity();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        if (isEmpty()) {
            return;
        }
        inventory.clear();
        syncBlockEntity();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? INPUT_SLOTS : OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == 4;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return switch (index) {
            case 0 -> stack.is(Items.INK_SAC);
            case 1 -> stack.is(SplatcraftItems.POWER_EGG.get());
            case 2 -> stack.is(SplatcraftItems.EMPTY_INKWELL.get());
            case 3 -> stack.is(SplatcraftTags.Items.FILTERS);
            default -> false;
        };
    }

    public boolean hasRecipe() {
        return InkVatMenu.hasIngredients(this) && color != -1;
    }

    public int getCraftableOutputCount() {
        if (!hasRecipe()) {
            return 0;
        }

        int count = Math.min(Math.min(
                inventory.get(INK_SAC_SLOT).getCount(),
                inventory.get(POWER_EGG_SLOT).getCount()),
                inventory.get(EMPTY_INKWELL_SLOT).getCount());
        return Math.min(count, new ItemStack(SplatcraftItems.INKWELL.get()).getMaxStackSize());
    }

    public void onRedstonePulse() {
        if (level == null || !hasRecipe()) {
            return;
        }

        List<Integer> colors = InkVatMenu.availableRecipeColors(this);
        setRecipeEntries(colors.size());
        if (pointer == -1 || colors.isEmpty()) {
            sendBlockUpdate();
            return;
        }

        List<Integer> sortedIndexes = InkVatMenu.sortedRecipeIndexes(colors);
        int currentSortedIndex = sortedIndexes.indexOf(pointer);
        int nextSortedIndex = currentSortedIndex == -1 ? 0 : (currentSortedIndex + 1) % sortedIndexes.size();
        int nextPointer = sortedIndexes.get(nextSortedIndex);
        setPointer(nextPointer);
        setColor(colors.get(nextPointer));
        InkVatMenu.updateOutput(this);
        sendBlockUpdate();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        if (this.color == color) {
            return;
        }
        this.color = color;
        syncBlockEntity();
    }

    public int getPointer() {
        return pointer;
    }

    public void setPointer(int pointer) {
        if (this.pointer == pointer) {
            return;
        }
        this.pointer = pointer;
        syncBlockEntity();
    }

    public int getRecipeEntries() {
        return recipeEntries;
    }

    public void setRecipeEntries(int recipeEntries) {
        if (this.recipeEntries == recipeEntries) {
            return;
        }
        this.recipeEntries = recipeEntries;
        syncBlockEntity();
    }

    private void sendBlockUpdate() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void syncBlockEntity() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        sendBlockUpdate();
    }
}
