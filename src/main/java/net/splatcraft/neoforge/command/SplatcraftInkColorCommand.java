package net.splatcraft.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.Stage;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public final class SplatcraftInkColorCommand {
    private SplatcraftInkColorCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("inkcolor").requires(source -> source.hasPermission(2))
                .then(Commands.argument("color", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                InkColorData.builtInColors().keySet().stream().map(id -> id.getPath()),
                                builder))
                        .executes(context -> setColor(
                                context.getSource(),
                                SplatcraftStageCommand.parseColor(StringArgumentType.getString(context, "color"))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setColor(
                                        context.getSource(),
                                        SplatcraftStageCommand.parseColor(StringArgumentType.getString(context, "color")),
                                        EntityArgument.getPlayers(context, "targets")))))
                .then(SplatcraftStageCommand.stageId("stage")
                        .then(SplatcraftStageCommand.stageTeam("team", "stage")
                                .executes(SplatcraftInkColorCommand::setColorByTeam)
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(SplatcraftInkColorCommand::setColorByTeamTargets)))));
    }

    private static int setColor(CommandSourceStack source, int color) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SplatcraftPlayerInfoEvents.setColor(player, color);
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.inkcolor.success.single",
                        player.getDisplayName(),
                        SplatcraftStageCommand.colorName(color)),
                true);
        return 1;
    }

    private static int setColor(CommandSourceStack source, int color, Collection<ServerPlayer> targets) {
        targets.forEach(player -> SplatcraftPlayerInfoEvents.setColor(player, color));
        if (targets.size() == 1) {
            ServerPlayer player = targets.iterator().next();
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.inkcolor.success.single",
                            player.getDisplayName(),
                            SplatcraftStageCommand.colorName(color)),
                    true);
        } else {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.inkcolor.success.multiple",
                            targets.size(),
                            SplatcraftStageCommand.colorName(color)),
                    true);
        }
        return targets.size();
    }

    private static int setColorByTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setColorByTeam(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "team"));
    }

    private static int setColorByTeamTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setColorByTeam(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "team"),
                EntityArgument.getPlayers(context, "targets"));
    }

    private static int setColorByTeam(CommandSourceStack source, String stageId, String teamId)
            throws CommandSyntaxException {
        Stage stage = requireTeam(source, stageId, teamId);
        return setColor(source, stage.getTeamColor(teamId));
    }

    private static int setColorByTeam(
            CommandSourceStack source,
            String stageId,
            String teamId,
            Collection<ServerPlayer> targets) throws CommandSyntaxException {
        Stage stage = requireTeam(source, stageId, teamId);
        return setColor(source, stage.getTeamColor(teamId), targets);
    }

    private static Stage requireTeam(CommandSourceStack source, String stageId, String teamId)
            throws CommandSyntaxException {
        Stage stage = SplatcraftStageCommand.requireStage(source, stageId);
        if (!stage.hasTeam(teamId)) {
            throw SplatcraftStageCommand.TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
        }
        return stage;
    }
}
