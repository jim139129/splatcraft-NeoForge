package net.splatcraft.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.ScoreHolder;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.splatcraft.neoforge.data.Stage;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftCriteriaTriggers;
import net.splatcraft.neoforge.registry.SplatcraftStats;
import net.splatcraft.neoforge.scoreboard.SplatcraftScoreboardHandler;
import net.splatcraft.neoforge.worldink.InkAreaActions;

public final class SplatcraftScanTurfCommand {
    private SplatcraftScanTurfCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scanturf").requires(source -> source.hasPermission(2))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .executes(context -> scanRange(context, ScanMode.TOP_DOWN, defaultTargets(context.getSource())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> scanRange(
                                                context,
                                                ScanMode.TOP_DOWN,
                                                EntityArgument.getPlayers(context, "targets")))
                                        .then(Commands.literal("topDown")
                                                .executes(context -> scanRange(
                                                        context,
                                                        ScanMode.TOP_DOWN,
                                                        EntityArgument.getPlayers(context, "targets"))))
                                        .then(Commands.literal("multiLayered")
                                                .executes(context -> scanRange(
                                                        context,
                                                        ScanMode.MULTI_LAYERED,
                                                        EntityArgument.getPlayers(context, "targets")))))))
                .then(SplatcraftStageCommand.stageId("stage")
                        .executes(context -> scanStage(context, ScanMode.TOP_DOWN, defaultTargets(context.getSource())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> scanStage(
                                        context,
                                        ScanMode.TOP_DOWN,
                                        EntityArgument.getPlayers(context, "targets")))
                                .then(Commands.literal("topDown")
                                        .executes(context -> scanStage(
                                                context,
                                                ScanMode.TOP_DOWN,
                                                EntityArgument.getPlayers(context, "targets"))))
                                .then(Commands.literal("multiLayered")
                                        .executes(context -> scanStage(
                                                context,
                                                ScanMode.MULTI_LAYERED,
                                                EntityArgument.getPlayers(context, "targets")))))));
    }

    private static Collection<ServerPlayer> defaultTargets(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return Collections.singleton(player);
        }
        return source.getLevel().players();
    }

    private static int scanRange(
            CommandContext<CommandSourceStack> context,
            ScanMode mode,
            Collection<ServerPlayer> targets) {
        return scan(
                context.getSource(),
                context.getSource().getLevel(),
                BlockPosArgument.getBlockPos(context, "from"),
                BlockPosArgument.getBlockPos(context, "to"),
                mode,
                targets,
                null);
    }

    private static int scanStage(
            CommandContext<CommandSourceStack> context,
            ScanMode mode,
            Collection<ServerPlayer> targets) throws CommandSyntaxException {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = SplatcraftStageCommand.requireStage(context.getSource(), stageId);
        ServerLevel level = SplatcraftStageCommand.stageLevel(context.getSource(), stage);
        if (level == null) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("status.scan_turf.out_of_world"),
                    true);
            return 0;
        }

        return scan(
                context.getSource(),
                level,
                stage.cornerA(),
                stage.cornerB(),
                mode,
                targets,
                stage);
    }

    private static int scan(
            CommandSourceStack source,
            ServerLevel level,
            BlockPos from,
            BlockPos to,
            ScanMode mode,
            Collection<ServerPlayer> targets,
            Stage stage) {
        if (!InkAreaOperations.inWorldBounds(level, from, to)) {
            source.sendSuccess(() -> Component.translatable("status.scan_turf.out_of_world"), true);
            return 0;
        }

        InkAreaActions.TurfScanResult result = InkAreaActions.scanTurf(
                level,
                from,
                to,
                mode == ScanMode.TOP_DOWN ? InkAreaActions.SCAN_TOP_DOWN : InkAreaActions.SCAN_MULTI_LAYERED);
        if (result.scores().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("status.scan_turf.no_ink"), true);
            return 0;
        }

        int winner = result.winner();
        sendResults(targets, result);
        updatePlayers(level, targets, result, winner);
        if (stage != null) {
            updateStageTeams(level, stage, winner);
        }

        source.sendSuccess(
                () -> Component.translatable("commands.scanturf.success", result.scannedBlocks()),
                true);
        return winner;
    }

    private static void sendResults(Collection<ServerPlayer> targets, InkAreaActions.TurfScanResult result) {
        for (ServerPlayer target : targets) {
            for (Map.Entry<Integer, Integer> entry : result.scores().entrySet()) {
                target.displayClientMessage(
                        Component.translatable(
                                "status.scan_turf.score",
                                SplatcraftStageCommand.colorName(entry.getKey()),
                                String.format("%.1f", result.percent(entry.getValue()))),
                        false);
            }
            target.displayClientMessage(
                    Component.translatable(
                            "status.scan_turf.winner",
                            SplatcraftStageCommand.colorName(result.winner())),
                    false);
        }
    }

    private static void updatePlayers(
            ServerLevel level,
            Collection<ServerPlayer> targets,
            InkAreaActions.TurfScanResult result,
            int winner) {
        Collection<ServerPlayer> players = targets.isEmpty() ? level.players() : targets;
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

    private static void updateStageTeams(ServerLevel level, Stage stage, int winner) {
        for (Map.Entry<String, Integer> team : stage.teams().entrySet()) {
            if (team.getValue() == winner) {
                level.getScoreboard().forAllObjectives(
                        Stats.CUSTOM.get(SplatcraftStats.TURF_WARS_WON),
                        ScoreHolder.forNameOnly("[" + team.getKey() + "]"),
                        score -> score.add(1));
            }
        }
    }

    private enum ScanMode {
        TOP_DOWN,
        MULTI_LAYERED
    }

}
