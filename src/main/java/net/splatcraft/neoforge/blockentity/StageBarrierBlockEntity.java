package net.splatcraft.neoforge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;
import net.splatcraft.neoforge.registry.SplatcraftDamageTypes;

public class StageBarrierBlockEntity extends BlockEntity {
    public static final int MAX_ACTIVE_TIME = 20;
    protected int activeTime = MAX_ACTIVE_TIME;

    public StageBarrierBlockEntity(BlockPos pos, BlockState state) {
        this(SplatcraftBlockEntities.STAGE_BARRIER.get(), pos, state);
    }

    protected StageBarrierBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StageBarrierBlockEntity barrier) {
        barrier.tickActiveTime();

        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(0.05D))) {
            if (entity instanceof SpawnShieldEntity) {
                continue;
            }

            barrier.resetActiveTime();
            if (!level.isClientSide && barrier.damagesPlayer() && entity instanceof Player player) {
                player.hurt(player.damageSources().source(SplatcraftDamageTypes.OUT_OF_STAGE), Float.MAX_VALUE);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("ActiveTime", activeTime);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        activeTime = tag.contains("ActiveTime") ? tag.getInt("ActiveTime") : MAX_ACTIVE_TIME;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public int getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(int activeTime) {
        if (this.activeTime == activeTime) {
            return;
        }
        this.activeTime = activeTime;
        setChanged();
    }

    public void resetActiveTime() {
        setActiveTime(MAX_ACTIVE_TIME);
    }

    public void tickActiveTime() {
        if (activeTime > 0) {
            activeTime--;
        }
    }

    protected boolean damagesPlayer() {
        return level != null && level.getBlockState(worldPosition).is(net.splatcraft.neoforge.registry.SplatcraftBlocks.STAGE_VOID.get());
    }

    protected void syncBlockEntity() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 2);
    }
}
