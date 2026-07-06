package net.splatcraft.neoforge.worldink;

import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.splatcraft.neoforge.entity.SplatcraftEntityState;
import net.splatcraft.neoforge.particle.InkSplashParticleOptions;
import net.splatcraft.neoforge.player.SquidInkMovement;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public final class InkSurfaceEffects {
    private static final double MIN_SPRINT_DISTANCE_SQR = 1.0E-5D;

    private InkSurfaceEffects() {
    }

    public static void onPlayLevelSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        replaceInkSurfaceSound(event, SquidInkMovement.blockStandingOn(event.getEntity()), event.getEntity());
    }

    public static void onPlayLevelSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        replaceInkSurfaceSound(event, BlockPos.containing(event.getPosition()), null);
    }

    public static void onLivingFall(LivingFallEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        OptionalInt color = SquidInkMovement.inkSurfaceColor(level, SquidInkMovement.blockStandingOn(entity));
        if (color.isEmpty()) {
            return;
        }

        float scale = Mth.sqrt(Math.max(0.0F, event.getDistance())) * 0.3F;
        level.sendParticles(
                new InkSplashParticleOptions(color.getAsInt(), scale),
                entity.getX(),
                entity.getY() + level.getRandom().nextFloat() * entity.getBbHeight() * 0.3D,
                entity.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (!level.isClientSide || !entity.isSprinting() || !entity.onGround() || entity.tickCount % 2 != 0) {
            return;
        }
        if (entity.getDeltaMovement().horizontalDistanceSqr() < MIN_SPRINT_DISTANCE_SQR
                && horizontalMovementSqr(entity) < MIN_SPRINT_DISTANCE_SQR) {
            return;
        }

        OptionalInt color = SquidInkMovement.inkSurfaceColor(level, SquidInkMovement.blockStandingOn(entity));
        if (color.isEmpty()) {
            return;
        }

        double x = entity.getX() + (level.getRandom().nextFloat() - 0.5D) * entity.getBbWidth();
        double y = entity.getY() + level.getRandom().nextFloat() * entity.getBbHeight() * 0.3D;
        double z = entity.getZ() + (level.getRandom().nextFloat() - 0.5D) * entity.getBbWidth();
        float scale = 0.3F + level.getRandom().nextFloat() * 0.4F;
        level.addParticle(new InkSplashParticleOptions(color.getAsInt(), scale), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private static void replaceInkSurfaceSound(PlayLevelSoundEvent event, BlockPos pos, Entity entity) {
        Holder<SoundEvent> sound = event.getSound();
        if (sound == null || SquidInkMovement.inkSurfaceColor(event.getLevel(), pos).isEmpty()) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(pos);
        SoundType soundType = state.getSoundType();
        SoundEvent playedSound = sound.value();
        if (playedSound == soundType.getStepSound()) {
            event.setSound(inkStepSound(entity));
        } else if (playedSound == soundType.getFallSound()) {
            event.setSound(SplatcraftSounds.INKED_BLOCK_FALL);
        }
    }

    private static Holder<SoundEvent> inkStepSound(Entity entity) {
        if (entity instanceof LivingEntity livingEntity
                && SplatcraftEntityState.isSquid(livingEntity)
                && SquidInkMovement.canSquidSwim(livingEntity)) {
            return SplatcraftSounds.INKED_BLOCK_SWIM;
        }
        return SplatcraftSounds.INKED_BLOCK_STEP;
    }

    private static double horizontalMovementSqr(Entity entity) {
        double x = entity.getX() - entity.xo;
        double z = entity.getZ() - entity.zo;
        return x * x + z * z;
    }
}
