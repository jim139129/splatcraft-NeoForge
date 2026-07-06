package net.splatcraft.neoforge.player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.splatcraft.neoforge.blockentity.SpawnPadBlockEntity;
import net.splatcraft.neoforge.worldink.InkDamageUtils;

public final class SplatcraftRespawnEvents {
    private SplatcraftRespawnEvents() {
    }

    public static void onPlayerRespawnPosition(PlayerRespawnPositionEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (player.getRespawnPosition() == null || player.getServer() == null) {
            return;
        }

        ServerLevel respawnLevel = player.getServer().getLevel(player.getRespawnDimension());
        if (respawnLevel == null
                || !(respawnLevel.getBlockEntity(player.getRespawnPosition()) instanceof SpawnPadBlockEntity spawnPad)) {
            return;
        }

        int playerColor = SplatcraftPlayerInfoEvents.color(player);
        int spawnPadColor = spawnPad.effectiveColor();
        if (spawnPadColor < 0 || InkDamageUtils.sameColor(respawnLevel, spawnPad.getBlockPos(), playerColor, spawnPadColor)) {
            return;
        }

        event.setDimensionTransition(DimensionTransition.missingRespawnBlock(
                player.getServer().overworld(),
                player,
                event.getDimensionTransition().postDimensionTransition()));
        event.setCopyOriginalSpawnPosition(false);
    }
}
