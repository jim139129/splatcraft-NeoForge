package net.splatcraft.neoforge.blockentity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.item.ColorChangerItem;
import net.splatcraft.neoforge.item.RemoteItem;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;

public class RemotePedestalBlockEntity extends InkColorBlockEntity implements WorldlyContainer {
    private ItemStack remote = ItemStack.EMPTY;
    private int signal;
    private int remoteResult;

    public RemotePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(SplatcraftBlockEntities.REMOTE_PEDESTAL.get(), pos, state);
    }

    public void onPowered() {
        if (level == null || !(remote.getItem() instanceof RemoteItem remoteItem)) {
            if (signal == 0 && remoteResult == 0) {
                return;
            }
            signal = 0;
            remoteResult = 0;
            syncBlockEntity();
            return;
        }

        RemoteItem.RemoteResult result = remoteItem.onRemoteUse(level, remote, getColor(), getBlockPos().getCenter(), null);
        if (signal == result.comparatorResult() && remoteResult == result.commandResult()) {
            return;
        }
        signal = result.comparatorResult();
        remoteResult = result.commandResult();
        syncBlockEntity();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Signal", signal);
        if (!remote.isEmpty()) {
            tag.put("Remote", remote.save(registries));
        }
        if (remoteResult != 0) {
            tag.putInt("RemoteResult", remoteResult);
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signal = tag.getInt("Signal");
        remote = tag.contains("Remote", CompoundTag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound("Remote"))
                : ItemStack.EMPTY;
        remoteResult = tag.getInt("RemoteResult");
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
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == 0 && stack.is(SplatcraftTags.Items.REMOTES);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == 0;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return remote.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? remote : ItemStack.EMPTY;
    }

    public int remoteColor() {
        return InkColorComponent.color(remote).orElse(-1);
    }

    public boolean hasRecolorableRemote() {
        return remote.getItem() instanceof ColorChangerItem;
    }

    public boolean canRecolorRemote(int color) {
        return hasRecolorableRemote()
                && InkColorComponent.color(remote).orElse(-1) != color;
    }

    public boolean recolorRemote(int color) {
        if (!canRecolorRemote(color)) {
            return false;
        }

        InkColorComponent.setColorAndLock(remote, color, true);
        syncBlockEntity();
        return true;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = remote.split(count);
        if (!stack.isEmpty()) {
            syncBlockEntity();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = remote;
        remote = ItemStack.EMPTY;
        syncBlockEntity();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0) {
            ItemStack previous = remote.copy();
            remote = stack;
            remote.limitSize(getMaxStackSize(remote));
            if (previous.getCount() != remote.getCount() || !ItemStack.isSameItemSameComponents(previous, remote)) {
                syncBlockEntity();
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        if (remote.isEmpty()) {
            return;
        }
        remote = ItemStack.EMPTY;
        syncBlockEntity();
    }

    public int getSignal() {
        return signal;
    }

    public void setSignal(int signal) {
        if (this.signal == signal) {
            return;
        }
        this.signal = signal;
        syncBlockEntity();
    }

    public int getRemoteResult() {
        return remoteResult;
    }

    public void setRemoteResult(int remoteResult) {
        if (this.remoteResult == remoteResult) {
            return;
        }
        this.remoteResult = remoteResult;
        syncBlockEntity();
    }
}
