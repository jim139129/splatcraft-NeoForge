package net.splatcraft.neoforge.entity.sub;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.particle.InkSplashParticleOptions;
import net.splatcraft.neoforge.worldink.InkBlockUtils;
import net.splatcraft.neoforge.worldink.InkDamageUtils;

public class CurlingBombEntity extends AbstractSubWeaponEntity {
    private static final String ENTITY_DATA_KEY = "EntityData";
    private static final String COOK_TIME_KEY = "CookTime";
    private static final int FLASH_DURATION = 20;

    private int cookTime;
    private boolean playedDetonatingSound;

    public CurlingBombEntity(EntityType<? extends AbstractSubWeaponEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setSourceWeapon(ItemStack stack) {
        super.setSourceWeapon(stack);
        cookTime = readCookTime(stack);
        age = Math.max(age, cookTime);
    }

    @Override
    protected int lifetimeTicks() {
        return subInt("fuse_time", 80);
    }

    @Override
    protected double getDefaultGravity() {
        return onGround() ? 0.0D : super.getDefaultGravity();
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) {
            return;
        }
        if (!level().isClientSide) {
            inkGroundTrail();
            playDetonatingSoundIfNeeded();
            spawnSlidingSplashIfNeeded();
        }
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.92D, 1.0D, 0.92D));
        }
    }

    @Override
    protected float explosionRadius() {
        return super.explosionRadius() + cookScale();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            InkDamageUtils.doSplatDamage(
                    level(),
                    target,
                    nestedSubFloat("curling", "contact_damage", 0.0F),
                    color(),
                    getOwner(),
                    this,
                    settings().stats().fullDamageToMobs());
        }
        bounce(getDeltaMovement());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        Vec3 velocity = getDeltaMovement();
        Direction face = result.getDirection();
        if (face.getAxis() == Direction.Axis.X) {
            setDeltaMovement(-velocity.x, velocity.y, velocity.z);
        } else if (face.getAxis() == Direction.Axis.Y && Math.abs(velocity.y) >= 0.05D) {
            setDeltaMovement(velocity.x, -velocity.y * 0.5D, velocity.z);
        } else if (face.getAxis() == Direction.Axis.Z) {
            setDeltaMovement(velocity.x, velocity.y, -velocity.z);
        }
    }

    @Override
    protected boolean expiresOnHit(HitResult result) {
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 2) {
            level().addParticle(new InkSplashParticleOptions(color(), explosionRadius() * 1.15F), getX(), getY() + 0.4D, getZ(), 0.0D, 0.0D, 0.0D);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return SplatcraftItems.CURLING_BOMB.get();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        cookTime = readCookTime(sourceWeapon());
        playedDetonatingSound = compound.getBoolean("PlayedDetonatingSound");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("PlayedDetonatingSound", playedDetonatingSound);
    }

    private float cookScale() {
        int maxCookTime = nestedSubInt("curling", "cook_time", 0);
        if (maxCookTime <= 0) {
            return 0.0F;
        }
        return Math.min(4.0F, cookTime / (float) maxCookTime);
    }

    private static int readCookTime(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getCompound(ENTITY_DATA_KEY).getInt(COOK_TIME_KEY);
    }

    private void inkGroundTrail() {
        float contactDamage = nestedSubFloat("curling", "contact_damage", 0.0F);
        for (int offset = 0; offset <= 2; offset++) {
            BlockPos pos = blockPosition().below(offset);
            if (!InkBlockUtils.canInkFromFace(level(), pos, Direction.UP)) {
                continue;
            }

            Entity owner = getOwner();
            if (owner instanceof Player player) {
                InkBlockUtils.playerInkBlock(player, level(), pos, color(), contactDamage, inkType);
            } else {
                InkBlockUtils.inkBlock(level(), pos, color(), contactDamage, inkType);
            }
            break;
        }
    }

    private void playDetonatingSoundIfNeeded() {
        int fuse = lifetimeTicks();
        if (!playedDetonatingSound && age >= fuse - FLASH_DURATION) {
            level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.SUB_DETONATING.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            playedDetonatingSound = true;
        }
    }

    private void spawnSlidingSplashIfNeeded() {
        double speed = getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).length();
        if (speed > 0.01D && age % Math.max(1, (int) Math.max(1.0D, (1.0D - speed) * 10.0D)) == 0) {
            level().broadcastEntityEvent(this, (byte) 2);
        }
    }

    private void bounce(Vec3 velocity) {
        double absX = Math.abs(velocity.x);
        double absY = Math.abs(velocity.y);
        double absZ = Math.abs(velocity.z);

        if (absX >= absY && absX >= absZ) {
            setDeltaMovement(-velocity.x, velocity.y, velocity.z);
        } else if (absY >= 0.05D && absY >= absX && absY >= absZ) {
            setDeltaMovement(velocity.x, -velocity.y * 0.5D, velocity.z);
        } else if (absZ >= absY && absZ >= absX) {
            setDeltaMovement(velocity.x, velocity.y, -velocity.z);
        }
    }
}
