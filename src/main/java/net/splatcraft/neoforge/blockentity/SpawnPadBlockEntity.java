package net.splatcraft.neoforge.blockentity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;
import net.splatcraft.neoforge.registry.SplatcraftEntities;

public class SpawnPadBlockEntity extends InkColorBlockEntity {
    private UUID spawnShieldUuid;

    public SpawnPadBlockEntity(BlockPos pos, BlockState state) {
        super(SplatcraftBlockEntities.SPAWN_PAD.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SpawnPadBlockEntity spawnPad) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int shieldColor = spawnPad.effectiveColor();
        SpawnShieldEntity shield = spawnPad.getSpawnShield(serverLevel);
        if (shield == null || shield.isRemoved()) {
            shield = new SpawnShieldEntity(SplatcraftEntities.SPAWN_SHIELD.get(), serverLevel);
            shield.setSpawnPadPos(pos);
            shield.setColor(shieldColor);
            shield.setShieldSize(SpawnShieldEntity.DEFAULT_SIZE);
            shield.setPos(pos.getX() + 0.5D, pos.getY() - 1.0D, pos.getZ() + 0.5D);
            serverLevel.addFreshEntity(shield);
            spawnPad.setSpawnShield(shield);
        } else {
            shield.setSpawnPadPos(pos);
            shield.setColor(shieldColor);
            shield.setPos(pos.getX() + 0.5D, pos.getY() - 1.0D, pos.getZ() + 0.5D);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (spawnShieldUuid != null) {
            tag.putUUID("SpawnShield", spawnShieldUuid);
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spawnShieldUuid = tag.hasUUID("SpawnShield") ? tag.getUUID("SpawnShield") : null;
    }

    public UUID getSpawnShieldUuid() {
        return spawnShieldUuid;
    }

    public void setSpawnShieldUuid(UUID spawnShieldUuid) {
        this.spawnShieldUuid = spawnShieldUuid;
        setChanged();
    }

    public boolean isSpawnShield(SpawnShieldEntity otherShield) {
        return otherShield != null && spawnShieldUuid != null && spawnShieldUuid.equals(otherShield.getUUID());
    }

    public SpawnShieldEntity getSpawnShield(ServerLevel level) {
        if (spawnShieldUuid == null) {
            return null;
        }

        Entity entity = level.getEntity(spawnShieldUuid);
        return entity instanceof SpawnShieldEntity shield ? shield : null;
    }

    public void setSpawnShield(SpawnShieldEntity shield) {
        setSpawnShieldUuid(shield == null ? null : shield.getUUID());
    }

}
