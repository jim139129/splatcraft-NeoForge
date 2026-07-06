package net.splatcraft.neoforge.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.blockentity.StageBarrierBlockEntity;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.WeaponSettings;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.particle.InkExplosionParticleOptions;
import net.splatcraft.neoforge.particle.InkSplashParticleOptions;
import net.splatcraft.neoforge.particle.SplatcraftParticleEffects;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.worldink.InkBlockUtils;
import net.splatcraft.neoforge.worldink.InkDamageUtils;
import net.splatcraft.neoforge.worldink.InkExplosion;

public class InkProjectileEntity extends ThrowableItemProjectile implements ColoredEntity {
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> CONTROLLED_VELOCITY =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STRAIGHT_SHOT_TICKS =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_STRAIGHT_SHOT_TICKS =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MAX_VELOCITY =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MIN_VELOCITY =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SHOT_X =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SHOT_Y =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SHOT_Z =
            SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);

    private ItemStack sourceWeapon = ItemStack.EMPTY;
    private ResourceLocation inkType = PlayerInfo.NORMAL_INK_TYPE;
    private int age;
    private boolean impacted;
    private boolean rollerAirborne;
    private float chargerCharge;
    private float splatlingCharge;
    private boolean dualieTurret;
    private final Set<UUID> piercedEntities = new HashSet<>();

    public InkProjectileEntity(EntityType<? extends InkProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static InkProjectileEntity create(Level level, LivingEntity shooter, ItemStack sourceWeapon) {
        InkProjectileEntity entity = new InkProjectileEntity(net.splatcraft.neoforge.registry.SplatcraftEntities.INK_PROJECTILE.get(), level);
        entity.setOwner(shooter);
        entity.setPos(shooter.getX(), shooter.getEyeY() - 0.1F, shooter.getZ());
        entity.setSourceWeapon(sourceWeapon);
        if (shooter instanceof Player player) {
            entity.inkType = SplatcraftPlayerInfoEvents.playerInfo(player).inkType();
        }
        return entity;
    }

    public void setSourceWeapon(ItemStack stack) {
        sourceWeapon = stack.copyWithCount(1);
        setItem(sourceWeapon);
        OptionalInt color = InkColorComponent.color(sourceWeapon);
        setColor(color.isPresent() ? color.getAsInt() : ownerColor(getOwner()));
        setSize(projectileFloat("size", 1.0F));
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

    public float projectileSize() {
        return entityData.get(SIZE);
    }

    public String projectileType() {
        ItemStack renderStack = sourceWeapon.isEmpty() ? getItem() : sourceWeapon;
        if (renderStack.getItem() instanceof WeaponItem weapon) {
            if (weapon.weaponClass() == WeaponItem.WeaponClass.SLOSHER && "explosher".equals(weapon.settingsId())) {
                return "blaster";
            }
            return switch (weapon.weaponClass()) {
                case BLASTER -> "blaster";
                case CHARGER -> "charger";
                case ROLLER -> "roller";
                case SHOOTER, DUALIE, SLOSHER, SPLATLING -> "shooter";
                default -> "default";
            };
        }
        return "default";
    }

    public void setSize(float size) {
        entityData.set(SIZE, Math.max(0.1F, size));
    }

    public void setRollerAirborne(boolean rollerAirborne) {
        this.rollerAirborne = rollerAirborne;
    }

    public void setChargerCharge(float chargerCharge) {
        this.chargerCharge = Math.max(0.0F, Math.min(1.0F, chargerCharge));
    }

    public void setSplatlingCharge(float splatlingCharge) {
        this.splatlingCharge = Math.max(0.0F, Math.min(2.0F, splatlingCharge));
    }

    public void setDualieTurret(boolean dualieTurret) {
        this.dualieTurret = dualieTurret;
    }

    public WeaponSettings settings() {
        return sourceWeapon.getItem() instanceof WeaponItem weapon ? weapon.settings() : WeaponSettings.EMPTY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, InkColorData.DEFAULT_COLOR);
        builder.define(SIZE, 1.0F);
        builder.define(CONTROLLED_VELOCITY, false);
        builder.define(STRAIGHT_SHOT_TICKS, 0);
        builder.define(MAX_STRAIGHT_SHOT_TICKS, 0);
        builder.define(MAX_VELOCITY, 0.0F);
        builder.define(MIN_VELOCITY, 0.0F);
        builder.define(SHOT_X, 0.0F);
        builder.define(SHOT_Y, 0.0F);
        builder.define(SHOT_Z, 0.0F);
    }

    @Override
    public void tick() {
        Vec3 controlledVelocity = controlledVelocity();
        if (!controlledVelocity.equals(Vec3.ZERO)) {
            setDeltaMovement(getDeltaMovement().add(controlledVelocity));
        }

        super.tick();

        if (!controlledVelocity.equals(Vec3.ZERO)) {
            setDeltaMovement(getDeltaMovement().subtract(controlledVelocity.scale(0.99D)));
        }

        if (usesControlledVelocity() && entityData.get(STRAIGHT_SHOT_TICKS) > 0) {
            entityData.set(STRAIGHT_SHOT_TICKS, entityData.get(STRAIGHT_SHOT_TICKS) - 1);
        }

        age++;

        if (isInWater()) {
            discard();
            return;
        }

        if (!level().isClientSide && shouldLeaveTrail()) {
            inkSplash(position(), trailRadius(), 0.0F, 0.0F);
            spawnTrailParticle();
        }

        if (!level().isClientSide && age >= lifetimeTicks()) {
            impact(null);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (result instanceof BlockHitResult blockResult && shouldIgnoreBlockHit(blockResult)) {
            return;
        }
        if (result instanceof EntityHitResult entityResult && canPierceEntity(entityResult.getEntity())) {
            if (!level().isClientSide) {
                pierceEntity(entityResult);
            }
            return;
        }
        super.onHit(result);
        if (!level().isClientSide) {
            impact(result);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().getBlockEntity(result.getBlockPos()) instanceof StageBarrierBlockEntity barrier) {
            barrier.resetActiveTime();
        }
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

    @Override
    protected double getDefaultGravity() {
        if (isChargerProjectile()) {
            return 0.0D;
        }
        return projectileFloat("gravity", 0.05F);
    }

    @Override
    public boolean isNoGravity() {
        return super.isNoGravity()
                || (usesControlledVelocity() && entityData.get(STRAIGHT_SHOT_TICKS) > 0)
                || getDefaultGravity() == 0.0D;
    }

    @Override
    protected Item getDefaultItem() {
        return SplatcraftItems.SPLATTERSHOT.get();
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        Vec3 shotVelocity = getDeltaMovement();
        if (shotVelocity.lengthSqr() > 1.0E-7D) {
            Vec3 shotDirection = shotVelocity.normalize();
            entityData.set(SHOT_X, (float) shotDirection.x);
            entityData.set(SHOT_Y, (float) shotDirection.y);
            entityData.set(SHOT_Z, (float) shotDirection.z);
        }
        configureControlledVelocity(Math.max(0.0F, velocity));
        if (usesControlledVelocity()) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Color", color());
        compound.putFloat("Size", projectileSize());
        compound.putInt("Age", age);
        compound.putString("InkType", inkType.toString());
        compound.put("SourceWeapon", sourceWeapon.save(registryAccess()));
        compound.putBoolean("ControlledVelocity", usesControlledVelocity());
        compound.putInt("StraightShotTicks", entityData.get(STRAIGHT_SHOT_TICKS));
        compound.putInt("MaxStraightShotTicks", entityData.get(MAX_STRAIGHT_SHOT_TICKS));
        compound.putFloat("MaxVelocity", entityData.get(MAX_VELOCITY));
        compound.putFloat("MinVelocity", entityData.get(MIN_VELOCITY));
        compound.putDouble("ShotX", shotDirection().x);
        compound.putDouble("ShotY", shotDirection().y);
        compound.putDouble("ShotZ", shotDirection().z);
        compound.putBoolean("RollerAirborne", rollerAirborne);
        compound.putFloat("ChargerCharge", chargerCharge);
        compound.putFloat("SplatlingCharge", splatlingCharge);
        compound.putBoolean("DualieTurret", dualieTurret);
        ListTag piercedEntityList = new ListTag();
        for (UUID uuid : piercedEntities) {
            piercedEntityList.add(NbtUtils.createUUID(uuid));
        }
        compound.put("PiercedEntities", piercedEntityList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setColor(compound.getInt("Color"));
        setSize(compound.contains("Size", CompoundTag.TAG_FLOAT) ? compound.getFloat("Size") : 1.0F);
        age = compound.getInt("Age");
        if (compound.contains("InkType", CompoundTag.TAG_STRING)) {
            ResourceLocation parsedInkType = ResourceLocation.tryParse(compound.getString("InkType"));
            inkType = parsedInkType == null ? PlayerInfo.NORMAL_INK_TYPE : parsedInkType;
        }
        sourceWeapon = ItemStack.parseOptional(registryAccess(), compound.getCompound("SourceWeapon"));
        entityData.set(CONTROLLED_VELOCITY, compound.getBoolean("ControlledVelocity"));
        entityData.set(STRAIGHT_SHOT_TICKS, compound.getInt("StraightShotTicks"));
        entityData.set(MAX_STRAIGHT_SHOT_TICKS, compound.getInt("MaxStraightShotTicks"));
        entityData.set(MAX_VELOCITY, compound.getFloat("MaxVelocity"));
        entityData.set(MIN_VELOCITY, compound.getFloat("MinVelocity"));
        entityData.set(SHOT_X, compound.contains("ShotX", CompoundTag.TAG_DOUBLE) ? (float) compound.getDouble("ShotX") : 0.0F);
        entityData.set(SHOT_Y, compound.contains("ShotY", CompoundTag.TAG_DOUBLE) ? (float) compound.getDouble("ShotY") : 0.0F);
        entityData.set(SHOT_Z, compound.contains("ShotZ", CompoundTag.TAG_DOUBLE) ? (float) compound.getDouble("ShotZ") : 0.0F);
        rollerAirborne = compound.getBoolean("RollerAirborne");
        chargerCharge = compound.contains("ChargerCharge", CompoundTag.TAG_FLOAT) ? compound.getFloat("ChargerCharge") : 0.0F;
        splatlingCharge = compound.contains("SplatlingCharge", CompoundTag.TAG_FLOAT) ? compound.getFloat("SplatlingCharge") : 0.0F;
        dualieTurret = compound.getBoolean("DualieTurret");
        piercedEntities.clear();
        ListTag piercedEntityList = compound.getList("PiercedEntities", Tag.TAG_INT_ARRAY);
        for (int index = 0; index < piercedEntityList.size(); index++) {
            piercedEntities.add(NbtUtils.loadUUID(piercedEntityList.get(index)));
        }
    }

    private void impact(@Nullable HitResult result) {
        if (impacted) {
            return;
        }

        impacted = true;
        float radius = impactRadius();
        float minDamage = minDamage();
        float maxDamage = maxDamage(minDamage);
        float damage = decayedDamage(minDamage, maxDamage);
        if (result instanceof EntityHitResult entityResult && !isBlasterProjectile()) {
            directEntityImpact(entityResult, radius, damage);
            discard();
            return;
        }
        inkSplash(result == null ? position() : result.getLocation(), radius, minDamage, damage);
        if (isStageBarrierHit(result) && !isBlasterProjectile()) {
            level().broadcastEntityEvent(this, (byte) -1);
            discard();
            return;
        }
        spawnImpactParticle(result == null ? position() : result.getLocation(), radius);
        playImpactSound();
        discard();
    }

    private void directEntityImpact(EntityHitResult result, float radius, float damage) {
        Entity target = result.getEntity();
        if (target instanceof LivingEntity livingTarget) {
            InkDamageUtils.doSplatDamage(level(), livingTarget, damage, color(), getOwner(), this, settings().stats().fullDamageToMobs());
        }
        spawnImpactSplashParticle(result.getLocation(), radius);
    }

    private void inkSplash(net.minecraft.world.phys.Vec3 center, float radius, float minDamage, float maxDamage) {
        InkExplosion.create(
                level(),
                getOwner(),
                this,
                center,
                radius,
                color(),
                inkType,
                maxDamage,
                minDamage,
                maxDamage,
                settings().stats().fullDamageToMobs());
    }

    private boolean shouldLeaveTrail() {
        int interval = Math.max(0, projectileInt("ink_trail_tick_interval", 0));
        if (isChargerProjectile() && interval == 0) {
            return true;
        }
        return interval > 0 && age % interval == 0;
    }

    private float trailRadius() {
        float fallback = isChargerProjectile() ? projectileSize() * 1.1F : projectileSize() * 0.45F;
        return projectileFloat("ink_trail_coverage", fallback);
    }

    private void spawnTrailParticle() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.world.phys.Vec3 motion = getDeltaMovement();
        double x = getX() - motion.x * 0.25D;
        double y = getY() + getBbHeight() * 0.5D - motion.y * 0.25D;
        double z = getZ() - motion.z * 0.25D;
        if ("charger".equals(projectileType())) {
            serverLevel.sendParticles(new InkSplashParticleOptions(color(), projectileSize()), x, y, z, 1, 0.0D, -0.1D, 0.0D, 0.0D);
        } else {
            serverLevel.sendParticles(new InkSplashParticleOptions(color(), projectileSize()), x, y, z, 1, motion.x, motion.y, motion.z, 0.0D);
        }
    }

    private void spawnImpactParticle(net.minecraft.world.phys.Vec3 center, float radius) {
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            spawnImpactSplashParticle(serverLevel, center, radius);
            SplatcraftParticleEffects.inkExplosion(serverLevel, center, Math.max(projectileSize(), radius), color());
        }
    }

    private void spawnImpactSplashParticle(net.minecraft.world.phys.Vec3 center, float radius) {
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            spawnImpactSplashParticle(serverLevel, center, radius);
        }
    }

    private void spawnImpactSplashParticle(net.minecraft.server.level.ServerLevel serverLevel, net.minecraft.world.phys.Vec3 center, float radius) {
        serverLevel.sendParticles(new InkSplashParticleOptions(color(), Math.max(projectileSize() * 2.0F, radius)), center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void playImpactSound() {
        if (isBlasterProjectile()) {
            level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.BLASTER_EXPLOSION.get(),
                    SoundSource.PLAYERS, 0.8F, 0.95F + level().getRandom().nextFloat() * 0.1F);
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_BLOCK_HIT, SoundSource.PLAYERS, 0.45F, 1.3F);
    }

    private int lifetimeTicks() {
        if (isChargerProjectile()) {
            JsonObject projectile = object(settings().raw(), "projectile");
            float speed = Math.max(0.1F, rawNumber(projectile, "speed").map(Number::floatValue).orElse(1.5F));
            float minRange = rawNumber(projectile, "min_charge_range").map(Number::floatValue).orElse(speed * 4.0F);
            float maxRange = rawNumber(projectile, "max_charge_range").map(Number::floatValue).orElse(minRange);
            float range = minRange + (maxRange - minRange) * chargerCharge;
            return Math.max(4, Math.round(range / speed));
        }

        float speed = Math.max(0.1F, projectileFloat("speed", 1.5F));
        float range = settings().stats().range()
                .orElse(projectileFloat("range", projectileFloat("straight_shot_distance", 8.0F)));
        float multiplier = isBlasterProjectile() ? 1.0F : 4.0F;
        return Math.max(4, Math.round(range / speed * multiplier));
    }

    private float impactRadius() {
        if (isRollerProjectile()) {
            return projectileSize() * 0.85F;
        }
        if (isChargerProjectile()) {
            return projectileFloat("ink_coverage_on_impact", projectileSize() * 0.85F);
        }
        return projectileFloat(
                "ink_explosion_radius",
                projectileFloat("ink_coverage_on_impact", Math.max(0.75F, projectileSize() * 0.85F))
        );
    }

    private float minDamage() {
        if (isRollerProjectile()) {
            JsonObject action = rollerAction();
            return rawNumber(action, "min_damage").map(Number::floatValue).orElse(0.0F);
        }
        if (isChargerProjectile()) {
            return 0.0F;
        }
        if (isDualieTurretProjectile()) {
            return projectileFloat("decayed_damage", settings().stats().minDamage().orElse(0.0F));
        }
        return settings().stats().minDamage().orElse(0.0F);
    }

    private float maxDamage(float minDamage) {
        if (isRollerProjectile()) {
            JsonObject action = rollerAction();
            return rawNumber(action, "base_damage").map(Number::floatValue).orElse(minDamage);
        }
        if (isChargerProjectile()) {
            JsonObject projectile = object(settings().raw(), "projectile");
            float minPartialDamage = rawNumber(projectile, "min_partial_charge_damage").map(Number::floatValue).orElse(minDamage);
            float maxPartialDamage = rawNumber(projectile, "max_partial_charge_damage").map(Number::floatValue).orElse(minPartialDamage);
            float fullDamage = rawNumber(projectile, "fully_charged_damage").map(Number::floatValue).orElse(maxPartialDamage);
            return chargerCharge >= 1.0F ? fullDamage : minPartialDamage + (maxPartialDamage - minPartialDamage) * chargerCharge;
        }
        if (isDualieTurretProjectile()) {
            return projectileFloat("base_damage", settings().stats().maxDamage().orElse(minDamage));
        }
        return settings().stats().maxDamage().orElse(minDamage);
    }

    private float decayedDamage(float minDamage, float maxDamage) {
        int decayStart = isRollerProjectile()
                ? rawNumber(rollerAction(), "damage_decay_start_tick").map(Number::intValue).orElse(Integer.MAX_VALUE)
                : projectileInt("damage_decay_start_tick", Integer.MAX_VALUE);
        if (age <= decayStart || minDamage >= maxDamage) {
            return maxDamage;
        }

        float decayPerTick = isRollerProjectile()
                ? rawNumber(rollerAction(), "damage_decay_per_tick").map(Number::floatValue).orElse(0.0F)
                : projectileFloat("damage_decay_per_tick", 0.0F);
        return Math.max(minDamage, maxDamage - (age - decayStart) * decayPerTick);
    }

    private boolean shouldIgnoreBlockHit(BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        if (InkBlockUtils.canInkPassthrough(level(), pos)) {
            return true;
        }
        return level().getBlockEntity(pos) instanceof ColoredBarrierBlockEntity barrier && barrier.canAllowThrough(this);
    }

    private boolean isStageBarrierHit(@Nullable HitResult result) {
        return result instanceof BlockHitResult blockResult
                && level().getBlockState(blockResult.getBlockPos()).getBlock()
                instanceof BlockStateCompatBlocks.StageBarrierBlockEntityBlock;
    }

    private boolean isBlasterProjectile() {
        return "blaster".equals(projectileType());
    }

    private boolean isRollerProjectile() {
        return "roller".equals(projectileType());
    }

    private boolean isChargerProjectile() {
        return "charger".equals(projectileType());
    }

    private boolean canPierceEntity(Entity entity) {
        return isChargerProjectile() && chargerCharge >= chargerPierceCharge() && piercedEntities.add(entity.getUUID());
    }

    private void pierceEntity(EntityHitResult result) {
        float radius = impactRadius();
        float minDamage = minDamage();
        float maxDamage = maxDamage(minDamage);
        inkSplash(result.getLocation(), radius, minDamage, maxDamage);
        spawnImpactParticle(result.getLocation(), radius);
    }

    private float chargerPierceCharge() {
        return rawNumber(object(settings().raw(), "projectile"), "pierces_at_charge").map(Number::floatValue).orElse(Float.MAX_VALUE);
    }

    private JsonObject rollerAction() {
        JsonObject raw = settings().raw();
        JsonObject swing = object(raw, "swing");
        return rollerAirborne && raw.get("fling") instanceof JsonObject fling ? fling : swing;
    }

    private void configureControlledVelocity(float launchVelocity) {
        if (!hasProjectileNumber("straight_shot_distance") && !hasProjectileNumber("decayed_speed")) {
            entityData.set(CONTROLLED_VELOCITY, false);
            return;
        }

        float maxVelocity = Math.max(0.0F, projectileFloat("speed", launchVelocity));
        float minVelocity = Math.max(0.0F, projectileFloat("decayed_speed", maxVelocity));
        Vec3 direction = shotDirection();
        if (maxVelocity <= 0.0F || direction.lengthSqr() <= 1.0E-7D) {
            entityData.set(CONTROLLED_VELOCITY, false);
            return;
        }

        float averageVelocity = (maxVelocity + minVelocity) * 0.5F;
        float straightShotDistance = Math.max(0.0F, projectileFloat("straight_shot_distance", 0.0F));
        int straightShotTicks = averageVelocity > 0.0F ? (int) (straightShotDistance / averageVelocity) : 0;
        entityData.set(CONTROLLED_VELOCITY, true);
        entityData.set(STRAIGHT_SHOT_TICKS, Math.max(0, straightShotTicks));
        entityData.set(MAX_STRAIGHT_SHOT_TICKS, Math.max(0, straightShotTicks));
        entityData.set(MAX_VELOCITY, maxVelocity);
        entityData.set(MIN_VELOCITY, minVelocity);
    }

    private Vec3 controlledVelocity() {
        if (!usesControlledVelocity()) {
            return Vec3.ZERO;
        }

        Vec3 direction = shotDirection();
        if (direction.lengthSqr() <= 1.0E-7D) {
            return Vec3.ZERO;
        }

        int straightShotTicks = entityData.get(STRAIGHT_SHOT_TICKS);
        int maxStraightShotTicks = entityData.get(MAX_STRAIGHT_SHOT_TICKS);
        double minVelocity = entityData.get(MIN_VELOCITY);
        double maxVelocity = entityData.get(MAX_VELOCITY);
        double velocity = straightShotTicks <= 0 || maxStraightShotTicks <= 0
                ? minVelocity
                : minVelocity + (maxVelocity - minVelocity) * ((double) straightShotTicks / maxStraightShotTicks);
        return direction.normalize().scale(velocity);
    }

    private Vec3 shotDirection() {
        return new Vec3(entityData.get(SHOT_X), entityData.get(SHOT_Y), entityData.get(SHOT_Z));
    }

    private boolean usesControlledVelocity() {
        return entityData.get(CONTROLLED_VELOCITY);
    }

    private boolean hasProjectileNumber(String key) {
        return rawProjectileNumber(key).isPresent();
    }

    private float projectileFloat(String key, float fallback) {
        return rawProjectileNumber(key).map(Number::floatValue).orElse(fallback);
    }

    private int projectileInt(String key, int fallback) {
        return rawProjectileNumber(key).map(Number::intValue).orElse(fallback);
    }

    private Optional<Number> rawProjectileNumber(String key) {
        return rawNumber(projectileObject(), key);
    }

    private JsonObject projectileObject() {
        JsonObject raw = settings().raw();
        JsonObject projectile = object(raw, "projectile").deepCopy();
        if (isSplatlingProjectile()) {
            for (var entry : object(raw, "second_charge_projectile").entrySet()) {
                projectile.add(entry.getKey(), entry.getValue());
            }
        }
        if (isDualieTurretProjectile()) {
            for (var entry : object(raw, "turret_projectile").entrySet()) {
                projectile.add(entry.getKey(), entry.getValue());
            }
        }
        return projectile;
    }

    private static Optional<Number> rawNumber(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? Optional.of(value.getAsNumber())
                : Optional.empty();
    }

    private boolean isSplatlingProjectile() {
        return splatlingCharge > 1.0F
                && sourceWeapon.getItem() instanceof WeaponItem weapon
                && weapon.weaponClass() == WeaponItem.WeaponClass.SPLATLING;
    }

    private boolean isDualieTurretProjectile() {
        return dualieTurret
                && sourceWeapon.getItem() instanceof WeaponItem weapon
                && weapon.weaponClass() == WeaponItem.WeaponClass.DUALIE;
    }

    private static JsonObject object(JsonObject parent, String name) {
        JsonElement element = parent.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static int ownerColor(@Nullable Entity owner) {
        return owner instanceof Player player ? SplatcraftPlayerInfoEvents.color(player) : InkColorData.DEFAULT_COLOR;
    }
}
