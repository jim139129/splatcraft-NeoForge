package net.splatcraft.neoforge.entity.sub;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public class SuctionBombEntity extends AbstractSubWeaponEntity {
    private static final int FLASH_DURATION = 20;

    private boolean activated;
    private boolean playedDetonatingSound;
    private int fuseTime;
    private int previousFuseTime;
    private Direction stickFacing = Direction.UP;

    public SuctionBombEntity(EntityType<? extends AbstractSubWeaponEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        if (activated) {
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
        }

        super.tick();
        previousFuseTime = fuseTime;
        if (activated) {
            setDeltaMovement(Vec3.ZERO);
            fuseTime++;
            playDetonatingSoundIfNeeded();
            if (!level().isClientSide && fuseTime >= subInt("fuse_time", 40)) {
                expire();
            }
        }
    }

    @Override
    public float flashIntensity(float partialTick) {
        int fuse = Math.max(1, subInt("fuse_time", 40));
        float fuseProgress = Mth.lerp(partialTick, previousFuseTime, fuseTime);
        return Mth.clamp((fuseProgress - (fuse - FLASH_DURATION)) / FLASH_DURATION, 0.0F, 1.0F);
    }

    @Override
    protected int lifetimeTicks() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (activated) {
            return;
        }

        activated = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.05D);
        Vec3 location = result.getLocation().subtract(normal);
        setPosRaw(location.x, location.y, location.z);

        stickFacing = result.getDirection();
        applyStickRotation();
    }

    @Override
    protected boolean expiresOnHit(HitResult result) {
        return false;
    }

    @Override
    protected Item getDefaultItem() {
        return SplatcraftItems.SUCTION_BOMB.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Activated", activated);
        compound.putBoolean("PlayedDetonatingSound", playedDetonatingSound);
        compound.putInt("FuseTime", fuseTime);
        compound.putString("StickFacing", stickFacing.getName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        activated = compound.getBoolean("Activated");
        playedDetonatingSound = compound.getBoolean("PlayedDetonatingSound");
        fuseTime = compound.getInt("FuseTime");
        previousFuseTime = fuseTime;
        if (compound.contains("StickFacing", CompoundTag.TAG_STRING)) {
            Direction parsed = Direction.byName(compound.getString("StickFacing"));
            stickFacing = parsed == null ? Direction.UP : parsed;
        }
        setNoGravity(activated);
        applyStickRotation();
    }

    private void playDetonatingSoundIfNeeded() {
        int fuse = subInt("fuse_time", 40);
        if (!level().isClientSide && !playedDetonatingSound && fuseTime >= fuse - FLASH_DURATION) {
            level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.SUB_DETONATING.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            playedDetonatingSound = true;
        }
    }

    private void applyStickRotation() {
        if (stickFacing.getAxis() == Direction.Axis.Y) {
            setXRot(stickFacing == Direction.UP ? -90.0F : 90.0F);
        } else {
            setYRot(180.0F - stickFacing.toYRot());
            setXRot(0.0F);
        }
        yRotO = getYRot();
        xRotO = getXRot();
    }
}
