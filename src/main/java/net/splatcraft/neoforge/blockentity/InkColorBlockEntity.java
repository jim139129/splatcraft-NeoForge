package net.splatcraft.neoforge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;

public class InkColorBlockEntity extends BlockEntity {
    protected int color = InkColorData.DEFAULT_COLOR;
    protected boolean inverted;
    protected String team = "";

    public InkColorBlockEntity(BlockPos pos, BlockState state) {
        this(SplatcraftBlockEntities.COLOR.get(), pos, state);
    }

    protected InkColorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Color", color);
        tag.putBoolean("Inverted", inverted);
        if (!team.isEmpty()) {
            tag.putString("Team", team);
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = readColor(tag, InkColorData.DEFAULT_COLOR);
        inverted = tag.getBoolean("Inverted");
        team = tag.getString("Team");
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
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (!tag.isEmpty()) {
            loadWithComponents(tag, registries);
            refreshClientRender();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadWithComponents(tag, registries);
        refreshClientRender();
    }

    protected static int readColor(CompoundTag tag, int fallback) {
        if (!tag.contains("Color")) {
            return fallback;
        }
        if (tag.contains("Color", CompoundTag.TAG_STRING)) {
            return InkColorData.resolve(tag.getString("Color")).orElse(fallback);
        }
        return tag.getInt("Color");
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

    public int effectiveColor() {
        return color >= 0 && inverted ? 0xFFFFFF - color : color;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        if (this.inverted == inverted) {
            return;
        }
        this.inverted = inverted;
        syncBlockEntity();
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        String normalizedTeam = team == null ? "" : team;
        if (this.team.equals(normalizedTeam)) {
            return;
        }
        this.team = normalizedTeam;
        syncBlockEntity();
    }

    protected void syncBlockEntity() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 2);
    }

    private void refreshClientRender() {
        if (level == null || !level.isClientSide) {
            return;
        }

        requestModelDataUpdate();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_IMMEDIATE);
    }
}
