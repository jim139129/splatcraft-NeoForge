package net.splatcraft.neoforge.item;

import java.util.Collection;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.ScoreHolder;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftCriteriaTriggers;
import net.splatcraft.neoforge.registry.SplatcraftStats;
import net.splatcraft.neoforge.scoreboard.SplatcraftScoreboardHandler;
import net.splatcraft.neoforge.worldink.InkAreaActions;

public class TurfScannerItem extends RemoteItem {
    public TurfScannerItem(Properties properties) {
        super(properties, 2);
    }

    @Override
    protected RemoteResult onRemoteUse(
            Level usedOnLevel,
            Level targetLevel,
            BlockPos pointA,
            BlockPos pointB,
            ItemStack stack,
            int color,
            int mode,
            Collection<ServerPlayer> targets) {
        return scanAndReport(targetLevel, usedOnLevel, pointA, pointB, mode, targets);
    }

    @Override
    public RemoteResult onRemoteUse(
            Level level,
            BlockPos pointA,
            BlockPos pointB,
            ItemStack stack,
            int color,
            int mode,
            Collection<ServerPlayer> targets) {
        return scanAndReport(level, level, pointA, pointB, mode, targets);
    }

    private RemoteResult scanAndReport(
            Level scanLevel,
            Level outputLevel,
            BlockPos pointA,
            BlockPos pointB,
            int mode,
            Collection<ServerPlayer> targets) {
        if (!InkAreaActions.inWorldBounds(scanLevel, pointA, pointB)) {
            return RemoteResult.fail(Component.translatable("status.scan_turf.out_of_world"));
        }

        InkAreaActions.TurfScanResult result = InkAreaActions.scanTurf(scanLevel, pointA, pointB, Math.floorMod(mode, 2));
        if (result.scores().isEmpty()) {
            return RemoteResult.fail(Component.translatable("status.scan_turf.no_ink"));
        }

        int winner = result.winner();
        if (outputLevel instanceof ServerLevel serverLevel) {
            Collection<ServerPlayer> resultTargets = targetsAllPlayers(targets) ? serverLevel.players() : targets;
            if (!resultTargets.isEmpty()) {
                sendResults(resultTargets, result);
            }
            updatePlayers(serverLevel, resultTargets, result, winner);
        }
        return new TurfScanRemoteResult(
                true,
                Component.translatable("commands.scanturf.success", result.scannedBlocks()),
                winner,
                comparatorResult(result),
                result);
    }

    private static int comparatorResult(InkAreaActions.TurfScanResult result) {
        if (result.scannedBlocks() <= 0) {
            return 0;
        }
        int winnerScore = result.scores().getOrDefault(result.winner(), 0);
        return Math.max(0, Math.min(15, winnerScore * 15 / result.scannedBlocks()));
    }

    private static void sendResults(Collection<ServerPlayer> targets, InkAreaActions.TurfScanResult result) {
        for (ServerPlayer target : targets) {
            sendResults(target, result);
        }
    }

    private static void sendResults(ServerPlayer player, InkAreaActions.TurfScanResult result) {
        for (Map.Entry<Integer, Integer> entry : result.scores().entrySet()) {
            player.displayClientMessage(
                    Component.translatable(
                            "status.scan_turf.score",
                            colorName(entry.getKey()),
                            String.format("%.1f", result.percent(entry.getValue()))),
                    false);
        }
        player.displayClientMessage(
                Component.translatable("status.scan_turf.winner", colorName(result.winner())),
                false);
    }

    private static void updatePlayers(
            ServerLevel level,
            Collection<ServerPlayer> players,
            InkAreaActions.TurfScanResult result,
            int winner) {
        for (ServerPlayer player : players) {
            int playerColor = SplatcraftPlayerInfoEvents.color(player);
            int score = result.scores().getOrDefault(playerColor, 0);
            SplatcraftCriteriaTriggers.SCAN_TURF.get().trigger(player, score, playerColor == winner);
            SplatcraftScoreboardHandler.updatePlayerScore(SplatcraftScoreboardHandler.TURF_WAR_SCORE, player, score);
            if (playerColor == winner) {
                player.awardStat(SplatcraftStats.TURF_WARS_WON);
            }
            if (SplatcraftScoreboardHandler.hasColorCriterion(playerColor)) {
                level.getScoreboard().forAllObjectives(
                        playerColor == winner
                                ? SplatcraftScoreboardHandler.getColorWins(playerColor)
                                : SplatcraftScoreboardHandler.getColorLosses(playerColor),
                        ScoreHolder.fromGameProfile(player.getGameProfile()),
                        scoreAccess -> scoreAccess.add(1));
            }
        }
    }

    private static MutableComponent colorName(int color) {
        return InkColorData.builtInId(color)
                .map(TurfScannerItem::colorName)
                .orElseGet(() -> Component.literal(String.format("#%06X", color)));
    }

    private static MutableComponent colorName(ResourceLocation id) {
        return Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath());
    }

    private static class TurfScanRemoteResult extends RemoteResult {
        private final InkAreaActions.TurfScanResult scanResult;

        TurfScanRemoteResult(
                boolean success,
                Component output,
                int commandResult,
                int comparatorResult,
                InkAreaActions.TurfScanResult scanResult
        ) {
            super(success, output, commandResult, comparatorResult);
            this.scanResult = scanResult;
        }

        InkAreaActions.TurfScanResult scanResult() {
            return scanResult;
        }
    }
}
