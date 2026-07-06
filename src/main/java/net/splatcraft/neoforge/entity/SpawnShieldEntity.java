package net.splatcraft.neoforge.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.blockentity.SpawnPadBlockEntity;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.entity.sub.AbstractSubWeaponEntity;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.worldink.InkDamageUtils;

public class SpawnShieldEntity extends Entity implements ColoredEntity {
    public static final int MAX_ACTIVE_TIME = 20;
    public static final float DEFAULT_SIZE = 4.0F;

    private static final EntityDataAccessor<Integer> ACTIVE_TIME =
            SynchedEntityData.defineId(SpawnShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(SpawnShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(SpawnShieldEntity.class, EntityDataSerializers.FLOAT);

    private BlockPos spawnPadPos;

    public SpawnShieldEntity(EntityType<? extends SpawnShieldEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ACTIVE_TIME, 0);
        builder.define(COLOR, InkColorData.DEFAULT_COLOR);
        builder.define(SIZE, DEFAULT_SIZE);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);

        if (level().isClientSide) {
            return;
        }

        if (!hasValidSpawnPad()) {
            discard();
            return;
        }

        SpawnPadBlockEntity spawnPad = (SpawnPadBlockEntity) level().getBlockEntity(spawnPadPos);
        int spawnPadColor = spawnPad.effectiveColor();
        if (spawnPadColor != color()) {
            setColor(spawnPadColor);
        }

        if (activeTime() > 0) {
            setActiveTime(activeTime() - 1);
        }

        repelEnemyEntities();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(shieldSize());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void kill() {
        if (spawnPadPos == null) {
            super.kill();
        }
    }

    @Override
    public int splatcraftColor() {
        return color();
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public void setColor(int color) {
        entityData.set(COLOR, color & 0xFFFFFF);
    }

    public int activeTime() {
        return entityData.get(ACTIVE_TIME);
    }

    public void setActiveTime(int activeTime) {
        entityData.set(ACTIVE_TIME, Mth.clamp(activeTime, 0, MAX_ACTIVE_TIME));
    }

    public float shieldSize() {
        return entityData.get(SIZE);
    }

    public void setShieldSize(float size) {
        entityData.set(SIZE, Math.max(0.1F, size));
        refreshDimensions();
    }

    public BlockPos spawnPadPos() {
        return spawnPadPos;
    }

    public void setSpawnPadPos(BlockPos spawnPadPos) {
        this.spawnPadPos = spawnPadPos;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        setColor(compound.contains("Color", CompoundTag.TAG_INT) ? compound.getInt("Color") : InkColorData.DEFAULT_COLOR);
        setActiveTime(compound.contains("ActiveTime", CompoundTag.TAG_INT) ? compound.getInt("ActiveTime") : 0);
        setShieldSize(compound.contains("Size", CompoundTag.TAG_FLOAT) ? compound.getFloat("Size") : DEFAULT_SIZE);
        spawnPadPos = compound.contains("SpawnPadPos") ? NbtUtils.readBlockPos(compound, "SpawnPadPos").orElse(null) : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("Color", color());
        compound.putInt("ActiveTime", activeTime());
        compound.putFloat("Size", shieldSize());
        if (spawnPadPos != null) {
            compound.put("SpawnPadPos", NbtUtils.writeBlockPos(spawnPadPos));
        }
    }

    private boolean hasValidSpawnPad() {
        return spawnPadPos != null
                && level().getBlockEntity(spawnPadPos) instanceof SpawnPadBlockEntity spawnPad
                && spawnPad.isSpawnShield(this);
    }

    private void repelEnemyEntities() {
        for (Entity entity : level().getEntitiesOfClass(Entity.class, getBoundingBox())) {
            if (entity == this || entity.isRemoved() || canPassThrough(entity)) {
                continue;
            }

            setActiveTime(MAX_ACTIVE_TIME);
            if (entity instanceof InkProjectileEntity || entity instanceof AbstractSubWeaponEntity) {
                level().broadcastEntityEvent(entity, (byte) -1);
                entity.discard();
            } else {
                repel(entity);
            }
        }
    }

    private boolean canPassThrough(Entity entity) {
        if (entity.getType().is(SplatcraftTags.EntityTypes.BYPASSES_SPAWN_SHIELD)) {
            return true;
        }

        int entityColor = entityColor(entity);
        BlockPos rulePos = spawnPadPos == null ? blockPosition() : spawnPadPos;
        return entityColor >= 0 && InkDamageUtils.sameColor(level(), rulePos, entityColor, color());
    }

    private void repel(Entity entity) {
        if (entity instanceof Player player && player.isPassenger()) {
            player.stopRiding();
        }

        Vec3 offset = entity.position().subtract(position());
        if (offset.lengthSqr() < 1.0E-4D) {
            offset = new Vec3(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
        }

        Vec3 movement = offset.normalize().scale(0.5D);
        entity.setDeltaMovement(movement);
        entity.hurtMarked = true;
    }

    private static int entityColor(Entity entity) {
        if (entity instanceof Player player) {
            return SplatcraftPlayerInfoEvents.color(player);
        }
        return entity instanceof ColoredEntity colored ? colored.splatcraftColor() : -1;
    }
}
