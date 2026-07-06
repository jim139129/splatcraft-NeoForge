package net.splatcraft.neoforge.player;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.network.payload.GameRulesPayload;
import net.splatcraft.neoforge.network.payload.PlayerColorsPayload;
import net.splatcraft.neoforge.network.payload.PlayerSkinOverlayPayload;
import net.splatcraft.neoforge.network.payload.StageListPayload;
import net.splatcraft.neoforge.network.payload.WeaponSettingsPayload;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;
import net.splatcraft.neoforge.registry.SplatcraftCriteriaTriggers;
import net.splatcraft.neoforge.scoreboard.SplatcraftScoreboardHandler;

public final class SplatcraftPlayerInfoEvents {
    private SplatcraftPlayerInfoEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerInfo info = playerInfo(serverPlayer);
        boolean changed = ensureInitialized(serverPlayer, info);
        ItemStack inkBand = findInkBand(serverPlayer);
        if (!info.sameInkBand(inkBand)) {
            info.setInkBand(inkBand);
            changed = true;
        }

        if (changed) {
            sync(serverPlayer);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureInitialized(player, playerInfo(player));
            sync(player);
            GameRulesPayload.sendToPlayer(player);
            StageListPayload.sendToPlayer(player);
            WeaponSettingsPayload.sendToPlayer(player);
            PlayerColorsPayload.sendSnapshotTo(player);
            PlayerColorsPayload.sendUpdateToAll(player);
            PlayerSkinOverlayPayload.sendCachedTo(player);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer trackingPlayer)) {
            return;
        }

        Entity target = event.getTarget();
        if (target instanceof ServerPlayer trackedPlayer) {
            PlayerInfo info = trackedPlayer.getData(SplatcraftAttachments.PLAYER_INFO.get());
            ensureInitialized(trackedPlayer, info);
            sync(trackedPlayer);
        }
    }

    public static PlayerInfo playerInfo(Player player) {
        return player.getData(SplatcraftAttachments.PLAYER_INFO.get());
    }

    public static boolean isSquid(Player player) {
        return player.getExistingData(SplatcraftAttachments.PLAYER_INFO.get())
                .map(PlayerInfo::isSquid)
                .orElse(false);
    }

    public static void setSquid(Player player, boolean squid) {
        PlayerInfo info = existingOrCreate(player, squid);
        if (info == null) {
            return;
        }
        if (info.isSquid() == squid) {
            return;
        }

        info.setSquid(squid);
        player.refreshDimensions();
        if (player instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    public static int color(Player player) {
        return player.getExistingData(SplatcraftAttachments.PLAYER_INFO.get())
                .map(PlayerInfo::color)
                .orElse(InkColorData.DEFAULT_COLOR);
    }

    public static void setColor(Player player, int color) {
        PlayerInfo info = playerInfo(player);
        if (info.color() == color) {
            return;
        }

        info.setColor(color);
        SplatcraftScoreboardHandler.updatePlayerScore(SplatcraftScoreboardHandler.COLOR, player, color);
        if (player instanceof ServerPlayer serverPlayer) {
            triggerChangeInkColor(serverPlayer, color);
            sync(serverPlayer);
            PlayerColorsPayload.sendUpdateToAll(serverPlayer);
        }
    }

    private static void triggerChangeInkColor(ServerPlayer player, int color) {
        InkColorData.builtInId(color)
                .map(ResourceLocation::toString)
                .ifPresent(colorId -> SplatcraftCriteriaTriggers.CHANGE_INK_COLOR.get().trigger(player, colorId));
        SplatcraftCriteriaTriggers.CHANGE_INK_COLOR.get().trigger(player, color);
    }

    public static void sync(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }
        player.syncData(SplatcraftAttachments.PLAYER_INFO.get());
    }

    private static PlayerInfo existingOrCreate(Player player, boolean requestedSquid) {
        return player.getExistingData(SplatcraftAttachments.PLAYER_INFO.get())
                .orElseGet(() -> requestedSquid ? playerInfo(player) : null);
    }

    private static boolean ensureInitialized(ServerPlayer player, PlayerInfo info) {
        if (info.initialized()) {
            return false;
        }

        PlayerInfo starter = PlayerInfo.randomStarter(player.getRandom());
        info.setColor(starter.color());
        info.setInitialized(true);
        return true;
    }

    private static ItemStack findInkBand(Player player) {
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (stack.is(SplatcraftTags.Items.INK_BANDS)) {
                return stack.copyWithCount(1);
            }
        }
        for (ItemStack stack : inventory.armor) {
            if (stack.is(SplatcraftTags.Items.INK_BANDS)) {
                return stack.copyWithCount(1);
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack.is(SplatcraftTags.Items.INK_BANDS)) {
                return stack.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }
}
