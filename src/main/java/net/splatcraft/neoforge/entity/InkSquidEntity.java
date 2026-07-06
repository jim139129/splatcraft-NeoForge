package net.splatcraft.neoforge.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.particle.SquidSoulParticleOptions;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;

public class InkSquidEntity extends PathfinderMob implements ColoredEntity {
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(InkSquidEntity.class, EntityDataSerializers.INT);

    public InkSquidEntity(EntityType<? extends InkSquidEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, InkColorData.DEFAULT_COLOR);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
    }

    @Override
    public void tick() {
        super.tick();

        BlockPos pos = getBlockPosBelowThatAffectsMyMovement();
        if (!level().isClientSide
                && level().getBlockState(pos).is(SplatcraftBlocks.INKWELL.get())
                && level().getBlockEntity(pos) instanceof InkColorBlockEntity inkColor
                && inkColor.getColor() != color()) {
            setColor(inkColor.getColor());
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        level().broadcastEntityEvent(this, (byte) 60);
        super.die(damageSource);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60) {
            level().addParticle(
                    new SquidSoulParticleOptions(color()),
                    getX(),
                    getY() + getBbHeight() * 0.5D,
                    getZ(),
                    0.0D,
                    1.0D,
                    0.0D);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.HONEY_BLOCK_FALL, 0.15F, 1.0F);
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Color", color());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setColor(compound.contains("Color", CompoundTag.TAG_INT) ? compound.getInt("Color") : InkColorData.DEFAULT_COLOR);
    }
}
