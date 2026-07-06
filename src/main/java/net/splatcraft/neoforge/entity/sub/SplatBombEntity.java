package net.splatcraft.neoforge.entity.sub;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.registry.SplatcraftItems;

public class SplatBombEntity extends AbstractSubWeaponEntity {
    private static final int FLASH_DURATION = 10;

    private int fuseTime;
    private int previousFuseTime;

    public SplatBombEntity(EntityType<? extends AbstractSubWeaponEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();
        previousFuseTime = fuseTime;
        if (onGround()) {
            fuseTime++;
            if (!level().isClientSide && fuseTime >= subInt("fuse_time", 20)) {
                expire();
            }
        }
    }

    @Override
    public float flashIntensity(float partialTick) {
        int fuse = Math.max(1, subInt("fuse_time", 20));
        float fuseProgress = Mth.lerp(partialTick, previousFuseTime, fuseTime);
        return Mth.clamp((fuseProgress - (fuse - FLASH_DURATION)) / FLASH_DURATION, 0.0F, 1.0F);
    }

    @Override
    protected int lifetimeTicks() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Vec3 velocity = getDeltaMovement();
        double absX = Math.abs(velocity.x);
        double absY = Math.abs(velocity.y);
        double absZ = Math.abs(velocity.z);

        if (absX >= absY && absX >= absZ) {
            setDeltaMovement(-velocity.x * 0.3D, velocity.y, velocity.z * 0.3D);
        } else if (absY >= 0.05D && absY >= absX && absY >= absZ) {
            setDeltaMovement(velocity.x * 0.3D, -velocity.y * 0.5D, velocity.z * 0.3D);
        } else if (absZ >= absY && absZ >= absX) {
            setDeltaMovement(velocity.x * 0.3D, velocity.y, -velocity.z * 0.3D);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        Direction face = result.getDirection();
        Vec3 velocity = getDeltaMovement();
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
    protected Item getDefaultItem() {
        return SplatcraftItems.SPLAT_BOMB.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("FuseTime", fuseTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        fuseTime = compound.getInt("FuseTime");
        previousFuseTime = fuseTime;
    }
}
