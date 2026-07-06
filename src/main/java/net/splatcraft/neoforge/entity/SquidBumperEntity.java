package net.splatcraft.neoforge.entity;

import java.util.Collections;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.particle.InkExplosionParticleOptions;
import net.splatcraft.neoforge.particle.InkSplashParticleOptions;
import net.splatcraft.neoforge.registry.SplatcraftDamageTypes;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public class SquidBumperEntity extends LivingEntity implements ColoredEntity, ItemSupplier {
    public static final float MAX_INK_HEALTH = 20.0F;
    public static final int MAX_RESPAWN_TIME = 60;

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(SquidBumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> INK_HEALTH =
            SynchedEntityData.defineId(SquidBumperEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RESPAWN_TIME =
            SynchedEntityData.defineId(SquidBumperEntity.class, EntityDataSerializers.INT);

    private long punchCooldown;

    public SquidBumperEntity(EntityType<? extends SquidBumperEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, InkColorData.DEFAULT_COLOR);
        builder.define(INK_HEALTH, MAX_INK_HEALTH);
        builder.define(RESPAWN_TIME, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);

        if (!level().isClientSide && respawnTime() > 0) {
            int nextTime = respawnTime() - 1;
            setRespawnTime(nextTime);
            if (nextTime == 20) {
                level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.SQUID_BUMPER_RESPAWNING.get(), getSoundSource(), 1.0F, 1.0F);
            } else if (nextTime <= 0) {
                respawn();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !isAlive()) {
            return false;
        }

        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            discard();
            return true;
        }

        if (source.is(SplatcraftDamageTypes.SPLAT) || source.is(SplatcraftDamageTypes.ROLL)) {
            return hurtByInk(amount);
        }

        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            breakAndDrop();
            return true;
        }

        boolean arrow = source.getDirectEntity() instanceof AbstractArrow;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player) && !arrow) {
            return false;
        }

        if (playerCannotBuild(attacker)) {
            return false;
        }

        if (source.isCreativePlayer()) {
            playBreakEffects();
            discard();
            return true;
        }

        long gameTime = level().getGameTime();
        if (!arrow && gameTime - punchCooldown > 5L) {
            punchCooldown = gameTime;
            level().broadcastEntityEvent(this, (byte) 32);
            return true;
        }

        breakAndDrop();
        return true;
    }

    public boolean hurtByInk(float amount) {
        if (amount <= 0.0F || inkHealth() <= 0.0F) {
            return false;
        }

        setInkHealth(Math.max(0.0F, inkHealth() - amount));
        setRespawnTime(MAX_RESPAWN_TIME);
        level().broadcastEntityEvent(this, (byte) 31);
        if (inkHealth() <= 0.0F) {
            level().broadcastEntityEvent(this, (byte) 34);
            level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.SQUID_BUMPER_POP.get(), getSoundSource(), 0.6F, 1.6F);
        }
        return true;
    }

    public void respawn() {
        setInkHealth(MAX_INK_HEALTH);
        setRespawnTime(0);
        level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.SQUID_BUMPER_READY.get(), getSoundSource(), 1.0F, 1.0F);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public void setColor(int color) {
        entityData.set(COLOR, color & 0xFFFFFF);
    }

    @Override
    public int splatcraftColor() {
        return color();
    }

    public float inkHealth() {
        return entityData.get(INK_HEALTH);
    }

    public void setInkHealth(float inkHealth) {
        entityData.set(INK_HEALTH, Mth.clamp(inkHealth, 0.0F, MAX_INK_HEALTH));
    }

    public int respawnTime() {
        return entityData.get(RESPAWN_TIME);
    }

    public void setRespawnTime(int respawnTime) {
        entityData.set(RESPAWN_TIME, Math.max(0, respawnTime));
    }

    public float bumperScale(float partialTick) {
        if (inkHealth() > 0.0F) {
            return 1.0F;
        }
        return Mth.clamp((MAX_RESPAWN_TIME - respawnTime() + partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public boolean isPickable() {
        return inkHealth() > 0.0F;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return true;
    }

    @Override
    public void doPush(Entity entity) {
        if (inkHealth() <= 0.0F) {
            return;
        }

        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        double strongestAxis = Mth.absMax(dx, dz);
        if (strongestAxis < 0.01D) {
            return;
        }

        double distance = Math.sqrt(strongestAxis);
        dx = dx / distance * 0.15D;
        dz = dz / distance * 0.15D;
        if (!entity.isVehicle()) {
            entity.push(dx, 0.0D, dz);
        }
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = new ItemStack(SplatcraftItems.SQUID_BUMPER.get());
        InkColorComponent.setColorAndLock(stack, color(), true);
        return stack;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 31) {
            level().playLocalSound(getX(), getY(), getZ(), SplatcraftSounds.SQUID_BUMPER_INK.get(), getSoundSource(), 0.35F, 1.0F, false);
            playInkHitParticles();
        } else if (id == 32) {
            level().playLocalSound(getX(), getY(), getZ(), SplatcraftSounds.SQUID_BUMPER_HIT.get(), getSoundSource(), 0.35F, 1.0F, false);
        } else if (id == 34) {
            playPopParticles();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Color", color());
        compound.putFloat("InkHealth", inkHealth());
        compound.putInt("RespawnTime", respawnTime());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setColor(compound.contains("Color", CompoundTag.TAG_INT) ? compound.getInt("Color") : InkColorData.DEFAULT_COLOR);
        setInkHealth(compound.contains("InkHealth", CompoundTag.TAG_FLOAT) ? compound.getFloat("InkHealth") : MAX_INK_HEALTH);
        setRespawnTime(compound.contains("RespawnTime", CompoundTag.TAG_INT) ? compound.getInt("RespawnTime") : 0);
    }

    private void breakAndDrop() {
        spawnAtLocation(getItem(), 0.0F);
        playBreakEffects();
        discard();
    }

    private void playBreakEffects() {
        level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.SQUID_BUMPER_BREAK.get(), getSoundSource(), 0.8F, 1.0F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.defaultBlockState()),
                    getX(),
                    getY() + getBbHeight() * 0.5D,
                    getZ(),
                    10,
                    getBbWidth() * 0.25D,
                    getBbHeight() * 0.25D,
                    getBbWidth() * 0.25D,
                    0.05D);
        }
    }

    private void playPopParticles() {
        for (int i = 0; i < 10; i++) {
            level().addParticle(
                    new InkSplashParticleOptions(color(), 2.0F),
                    getX(),
                    getY() + getBbHeight() * 0.5D,
                    getZ(),
                    random.nextDouble() * 0.5D - 0.25D,
                    random.nextDouble() * 0.5D - 0.25D,
                    random.nextDouble() * 0.5D - 0.25D);
        }
        level().addParticle(
                new InkExplosionParticleOptions(color(), 2.0F),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                0.0D,
                0.0D,
                0.0D);
    }

    private void playInkHitParticles() {
        level().addParticle(
                new InkSplashParticleOptions(color(), 2.0F),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                0.0D,
                0.0D,
                0.0D);
    }

    private static boolean playerCannotBuild(Entity attacker) {
        return attacker instanceof Player player && !player.getAbilities().mayBuild;
    }
}
