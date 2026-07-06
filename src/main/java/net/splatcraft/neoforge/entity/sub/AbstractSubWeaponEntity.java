package net.splatcraft.neoforge.entity.sub;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.WeaponSettings;
import net.splatcraft.neoforge.entity.ColoredEntity;
import net.splatcraft.neoforge.item.SubWeaponItem;
import net.splatcraft.neoforge.particle.InkExplosionParticleOptions;
import net.splatcraft.neoforge.particle.SplatcraftParticleEffects;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.worldink.InkExplosion;

public abstract class AbstractSubWeaponEntity extends ThrowableItemProjectile implements ColoredEntity {
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(AbstractSubWeaponEntity.class, EntityDataSerializers.INT);

    protected int age;
    protected ItemStack sourceWeapon = ItemStack.EMPTY;
    protected ResourceLocation inkType = PlayerInfo.NORMAL_INK_TYPE;
    public boolean isItem = false;
    private boolean exploded;

    protected AbstractSubWeaponEntity(EntityType<? extends AbstractSubWeaponEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static <T extends AbstractSubWeaponEntity> T create(
            EntityType<T> entityType,
            Level level,
            LivingEntity thrower,
            ItemStack sourceWeapon
    ) {
        T entity = entityType.create(level);
        if (entity == null) {
            throw new IllegalStateException("Failed to create Splatcraft sub weapon entity " + entityType);
        }

        entity.setOwner(thrower);
        entity.setPos(thrower.getX(), thrower.getEyeY() - 0.1F, thrower.getZ());
        entity.setSourceWeapon(sourceWeapon);
        if (thrower instanceof Player player) {
            entity.inkType = SplatcraftPlayerInfoEvents.playerInfo(player).inkType();
        }
        return entity;
    }

    public static <T extends AbstractSubWeaponEntity> T create(
            EntityType<T> entityType,
            Level level,
            double x,
            double y,
            double z,
            ItemStack sourceWeapon
    ) {
        T entity = entityType.create(level);
        if (entity == null) {
            throw new IllegalStateException("Failed to create Splatcraft sub weapon entity " + entityType);
        }

        entity.setPos(x, y, z);
        entity.setSourceWeapon(sourceWeapon);
        return entity;
    }

    public void setSourceWeapon(ItemStack stack) {
        this.sourceWeapon = stack.copyWithCount(1);
        this.setItem(this.sourceWeapon);
        this.setColor(InkColorComponent.color(this.sourceWeapon).orElse(InkColorData.DEFAULT_COLOR));
    }

    public ItemStack sourceWeapon() {
        return sourceWeapon;
    }

    public int color() {
        return entityData.get(COLOR);
    }

    @Override
    public int splatcraftColor() {
        return color();
    }

    public void setColor(int color) {
        entityData.set(COLOR, color);
    }

    public WeaponSettings settings() {
        return sourceWeapon.getItem() instanceof SubWeaponItem subWeapon ? subWeapon.settings() : WeaponSettings.EMPTY;
    }

    public float renderAge(float partialTick) {
        return age + partialTick;
    }

    public float flashIntensity(float partialTick) {
        int lifetime = Math.max(1, lifetimeTicks());
        float fuseWindow = Math.min(20.0F, lifetime);
        float remaining = lifetime - renderAge(partialTick);
        return Mth.clamp((fuseWindow - remaining) / fuseWindow, 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, InkColorData.DEFAULT_COLOR);
    }

    @Override
    public void tick() {
        super.tick();
        if (isInWater()) {
            if (!level().isClientSide) {
                level().broadcastEntityEvent(this, (byte) -1);
                discard();
            }
            return;
        }

        age++;
        if (!level().isClientSide && age >= lifetimeTicks()) {
            expire();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && expiresOnHit(result)) {
            expire();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == -1) {
            level().addParticle(new InkExplosionParticleOptions(color(), 0.5F), getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else {
            super.handleEntityEvent(id);
        }
    }

    protected boolean expiresOnHit(HitResult result) {
        return true;
    }

    protected void expire() {
        explode();
        discard();
    }

    protected void explode() {
        if (exploded || level().isClientSide) {
            return;
        }

        exploded = true;
        WeaponSettings.WeaponStats stats = settings().stats();
        float minDamage = stats.minDamage().orElse(subFloat("indirect_damage", 0.0F));
        float maxDamage = stats.maxDamage().orElse(subFloat("direct_damage", subFloat("prop_damage", 0.0F)));
        float blockDamage = subFloat("prop_damage", maxDamage);
        InkExplosion.create(
                level(),
                getOwner(),
                this,
                position(),
                explosionRadius(),
                color(),
                inkType,
                blockDamage,
                minDamage,
                maxDamage,
                stats.fullDamageToMobs());
        level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.SUB_DETONATE.get(), SoundSource.PLAYERS, 0.8F, randomPitch(0.95F));
        spawnExplosionParticle();
    }

    private float randomPitch(float base) {
        return ((level().getRandom().nextFloat() - level().getRandom().nextFloat()) * 0.1F + 1.0F) * base;
    }

    private void spawnExplosionParticle() {
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SplatcraftParticleEffects.inkExplosion(serverLevel, position(), explosionRadius(), color());
        }
    }

    protected float explosionRadius() {
        return settings().stats().range().orElse(subFloat("explosion_size", 2.0F));
    }

    protected int lifetimeTicks() {
        return subInt("fuse_time", 60);
    }

    protected int subInt(String key, int fallback) {
        return rawNumber(key, null).map(Number::intValue).orElse(fallback);
    }

    protected float subFloat(String key, float fallback) {
        return rawNumber(key, null).map(Number::floatValue).orElse(fallback);
    }

    protected int nestedSubInt(String objectKey, String key, int fallback) {
        return rawNumber(key, objectKey).map(Number::intValue).orElse(fallback);
    }

    protected float nestedSubFloat(String objectKey, String key, float fallback) {
        return rawNumber(key, objectKey).map(Number::floatValue).orElse(fallback);
    }

    private java.util.Optional<Number> rawNumber(String key, @Nullable String objectKey) {
        com.google.gson.JsonObject raw = settings().raw();
        if (objectKey != null) {
            com.google.gson.JsonElement object = raw.get(objectKey);
            if (object == null || !object.isJsonObject()) {
                return java.util.Optional.empty();
            }
            raw = object.getAsJsonObject();
        }

        com.google.gson.JsonElement value = raw.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? java.util.Optional.of(value.getAsNumber())
                : java.util.Optional.empty();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Color", color());
        compound.putInt("Age", age);
        compound.putString("InkType", inkType.toString());
        compound.put("SourceWeapon", sourceWeapon.save(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setColor(compound.getInt("Color"));
        age = compound.getInt("Age");
        if (compound.contains("InkType", CompoundTag.TAG_STRING)) {
            ResourceLocation parsedInkType = ResourceLocation.tryParse(compound.getString("InkType"));
            inkType = parsedInkType == null ? PlayerInfo.NORMAL_INK_TYPE : parsedInkType;
        }
        sourceWeapon = ItemStack.parseOptional(registryAccess(), compound.getCompound("SourceWeapon"));
    }
}
