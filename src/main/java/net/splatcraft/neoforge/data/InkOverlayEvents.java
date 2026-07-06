package net.splatcraft.neoforge.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;

public final class InkOverlayEvents {
    private static final float PASSIVE_DECAY_PER_TICK = 0.01F;

    private InkOverlayEvents() {
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        target.getExistingData(SplatcraftAttachments.INK_OVERLAY.get())
                .ifPresent(info -> decayOverlay(target, info));
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer && event.getTarget() instanceof LivingEntity target) {
            target.getExistingData(SplatcraftAttachments.INK_OVERLAY.get())
                    .filter(info -> !info.isDefault())
                    .ifPresent(info -> target.syncData(SplatcraftAttachments.INK_OVERLAY.get()));
        }
    }

    public static void reduceAmount(LivingEntity target, float amount) {
        target.getExistingData(SplatcraftAttachments.INK_OVERLAY.get()).ifPresent(info -> {
            if (info.addAmount(-amount)) {
                sync(target);
            }
        });
    }

    private static void decayOverlay(LivingEntity target, InkOverlayInfo info) {
        boolean changed = target.isInWater() ? info.setAmount(0.0F) : info.addAmount(-PASSIVE_DECAY_PER_TICK);
        if (changed && target.isInWater()) {
            sync(target);
        }
    }

    private static void sync(LivingEntity target) {
        if (!target.level().isClientSide && canSync(target)) {
            target.syncData(SplatcraftAttachments.INK_OVERLAY.get());
        }
    }

    private static boolean canSync(LivingEntity target) {
        return !(target instanceof ServerPlayer player) || player.connection != null;
    }
}
