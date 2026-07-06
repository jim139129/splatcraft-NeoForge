package net.splatcraft.neoforge.player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.blockentity.SpawnPadBlockEntity;
import net.splatcraft.neoforge.data.InkOverlayEvents;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftCriteriaTriggers;
import net.splatcraft.neoforge.registry.SplatcraftDamageTypes;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.registry.SplatcraftStats;
import net.splatcraft.neoforge.worldink.InkDamageUtils;

public final class SquidFormEvents {
    private static final EntityDimensions SQUID_ENTRY_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.6F);
    private static final EntityDimensions SQUID_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.5F).withEyeHeight(0.4F);
    private static final EntityDimensions SUBMERGED_SQUID_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.5F).withEyeHeight(0.3F);
    private static final Set<UUID> SUBMERGED_PLAYERS = new HashSet<>();

    private SquidFormEvents() {
    }

    public static boolean canEnterSquid(Player player) {
        return player.isAlive()
                && !player.isSleeping()
                && player.getVehicle() == null
                && player.level().noCollision(player, SQUID_ENTRY_DIMENSIONS.makeBoundingBox(player.position()).deflate(1.0E-7D));
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        applyEnemyInkDamage(player);
        applyWaterDamage(player);
        if (!SplatcraftPlayerInfoEvents.isSquid(player)) {
            updateSubmergedState(player, false);
            return;
        }

        boolean canHide = SquidInkMovement.canSquidHide(player);
        updateSubmergedState(player, canHide);
        if (!player.getAbilities().flying) {
            player.setSprinting(canHide);
            player.walkDist = player.walkDistO;
        }
        if (canHide) {
            player.fallDistance = 0.0F;
            applyInkHealing(player);
        }

        player.setPose(Pose.SWIMMING);
        player.stopUsingItem();

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(SplatcraftStats.SQUID_TIME);
            applyStandingColorEffects(serverPlayer);
        }
    }

    private static void updateSubmergedState(Player player, boolean submerged) {
        if (player.level().isClientSide) {
            return;
        }

        UUID uuid = player.getUUID();
        boolean wasSubmerged = SUBMERGED_PLAYERS.contains(uuid);
        if (submerged == wasSubmerged) {
            return;
        }

        if (submerged) {
            SUBMERGED_PLAYERS.add(uuid);
            playInkTransitionSound(player, SplatcraftSounds.INK_SUBMERGE.get());
        } else {
            SUBMERGED_PLAYERS.remove(uuid);
            playInkTransitionSound(player, SplatcraftSounds.INK_SURFACE.get());
        }
    }

    private static void playInkTransitionSound(Player player, SoundEvent sound) {
        float pitch = ((player.level().getRandom().nextFloat() - player.level().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.5F, pitch);
    }

    private static void applyEnemyInkDamage(Player player) {
        if (player.level().isClientSide
                || player.level().getDifficulty() == Difficulty.PEACEFUL
                || player.tickCount % 20 != 0
                || player.getHealth() <= 4.0F
                || !SquidInkMovement.onEnemyInk(player)) {
            return;
        }

        player.hurt(player.damageSources().source(SplatcraftDamageTypes.ENEMY_INK), 2.0F);
    }

    private static void applyWaterDamage(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (!SplatcraftGameRules.localizedBoolean(player.level(), player.blockPosition(), SplatcraftGameRules.WATER_DAMAGE)) {
            return;
        }
        if (!player.isInWater() || player.tickCount % 10 != 0 || player.hasEffect(MobEffects.WATER_BREATHING)) {
            return;
        }

        player.hurt(player.damageSources().source(SplatcraftDamageTypes.WATER), 8.0F);
    }

    private static void applyInkHealing(Player player) {
        if (!SplatcraftGameRules.localizedBoolean(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING)) {
            return;
        }
        if (player.tickCount % 5 != 0 || player.getHealth() >= player.getMaxHealth() || player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER)) {
            return;
        }

        player.heal(0.5F);
        if (SplatcraftGameRules.localizedBoolean(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER)) {
            player.causeFoodExhaustion(0.25F);
        }
        InkOverlayEvents.reduceAmount(player, 0.49F);
    }

    public static void onEntitySize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && SplatcraftPlayerInfoEvents.isSquid(player)) {
            event.setNewSize(SquidInkMovement.canSquidHide(player) ? SUBMERGED_SQUID_DIMENSIONS : SQUID_DIMENSIONS);
        }
    }

    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (SplatcraftPlayerInfoEvents.isSquid(player) && SquidInkMovement.canSquidHide(player)) {
            event.modifyVisibility(isMovingWhileSubmerged(player) ? 0.7D : 0.0D);
        }
    }

    private static boolean isMovingWhileSubmerged(Player player) {
        return Math.abs(player.getX() - player.xo) > 0.14D
                || Math.abs(player.getY() - player.yo) > 0.07D
                || Math.abs(player.getZ() - player.zo) > 0.14D;
    }

    private static void applyStandingColorEffects(ServerPlayer player) {
        BlockPos pos = SquidInkMovement.blockStandingOn(player);
        BlockState state = player.level().getBlockState(pos);

        if (state.is(SplatcraftBlocks.INKWELL.get()) && player.level().getBlockEntity(pos) instanceof InkColorBlockEntity inkwell) {
            setPlayerColorFromBlock(player, inkwell);
            return;
        }

        BlockPos spawnPadPos = resolveSpawnPadPos(player, pos, state);
        if (spawnPadPos == null || !(player.level().getBlockEntity(spawnPadPos) instanceof SpawnPadBlockEntity spawnPad)) {
            return;
        }

        int spawnPadColor = spawnPad.effectiveColor();
        if (spawnPadColor < 0) {
            return;
        }

        if (SplatcraftGameRules.localizedBoolean(player.level(), spawnPadPos, SplatcraftGameRules.UNIVERSAL_INK)) {
            SplatcraftPlayerInfoEvents.setColor(player, spawnPadColor);
        }

        if (InkDamageUtils.sameColor(player.level(), spawnPadPos, SplatcraftPlayerInfoEvents.color(player), spawnPadColor)) {
            player.setRespawnPosition(player.level().dimension(), spawnPadPos, spawnPadYaw(spawnPad.getBlockState()), false, true);
        }
    }

    private static void setPlayerColorFromBlock(ServerPlayer player, InkColorBlockEntity colorBlock) {
        int color = effectiveBlockColor(colorBlock);
        if (color >= 0) {
            SplatcraftPlayerInfoEvents.setColor(player, color);
        }
    }

    private static int effectiveBlockColor(InkColorBlockEntity colorBlock) {
        int color = colorBlock.getColor();
        return color >= 0 && colorBlock.isInverted() ? 0xFFFFFF - color : color;
    }

    private static BlockPos resolveSpawnPadPos(ServerPlayer player, BlockPos pos, BlockState state) {
        if (state.is(SplatcraftBlocks.SPAWN_PAD.get())) {
            return pos;
        }
        if (!state.is(SplatcraftBlocks.SPAWN_PAD_EDGE.get())) {
            return null;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos parentPos = pos.relative(direction);
            if (player.level().getBlockState(parentPos).is(SplatcraftBlocks.SPAWN_PAD.get())) {
                return parentPos;
            }
            parentPos = pos.relative(direction).relative(direction.getClockWise());
            if (player.level().getBlockState(parentPos).is(SplatcraftBlocks.SPAWN_PAD.get())) {
                return parentPos;
            }
        }
        return null;
    }

    private static float spawnPadYaw(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING).toYRot()
                : 0.0F;
    }

    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (SplatcraftPlayerInfoEvents.isSquid(player) && SquidInkMovement.canSquidHide(player)) {
            SplatcraftCriteriaTriggers.FALL_INTO_INK.get().trigger(player, event.getDistance());
            event.setCanceled(true);
        }
    }

    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (SquidInkMovement.onEnemyInk(player)) {
            player.setDeltaMovement(
                    player.getDeltaMovement().x(),
                    Math.min(player.getDeltaMovement().y(), 0.1D),
                    player.getDeltaMovement().z());
        } else if (SplatcraftPlayerInfoEvents.isSquid(player) && SquidInkMovement.canSquidHide(player)) {
            player.setDeltaMovement(
                    player.getDeltaMovement().x(),
                    player.getDeltaMovement().y() * 1.1D,
                    player.getDeltaMovement().z());
        }
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (SplatcraftPlayerInfoEvents.isSquid(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getNewGameMode() == GameType.SPECTATOR) {
            SplatcraftPlayerInfoEvents.setSquid(event.getEntity(), false);
            updateSubmergedState(event.getEntity(), false);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SUBMERGED_PLAYERS.remove(event.getEntity().getUUID());
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        updateSubmergedState(event.getEntity(), false);
    }

    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (SplatcraftPlayerInfoEvents.isSquid(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (SplatcraftPlayerInfoEvents.isSquid(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (SplatcraftPlayerInfoEvents.isSquid(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (SplatcraftPlayerInfoEvents.isSquid(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (SplatcraftPlayerInfoEvents.isSquid(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (SplatcraftPlayerInfoEvents.isSquid(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
