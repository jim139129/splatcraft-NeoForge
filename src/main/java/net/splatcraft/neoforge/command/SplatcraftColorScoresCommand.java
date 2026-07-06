package net.splatcraft.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.scoreboard.SplatcraftScoreboardHandler;

public final class SplatcraftColorScoresCommand {
    private static final DynamicCommandExceptionType CRITERION_ALREADY_EXISTS =
            new DynamicCommandExceptionType(color -> Component.translatable("commands.colorscores.add.duplicate", color));

    private SplatcraftColorScoresCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("colorscores").requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("color", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        InkColorData.builtInColors().keySet().stream().map(id -> id.getPath()),
                                        builder))
                                .executes(SplatcraftColorScoresCommand::add)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("color", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        SplatcraftScoreboardHandler.getCriteriaKeySet().stream()
                                                .map(SplatcraftScoreboardHandler::getColorIdentifier),
                                        builder))
                                .executes(SplatcraftColorScoresCommand::remove)))
                .then(Commands.literal("list").executes(SplatcraftColorScoresCommand::list)));
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int color = SplatcraftStageCommand.parseColor(StringArgumentType.getString(context, "color"));
        if (SplatcraftScoreboardHandler.hasColorCriterion(color)) {
            throw CRITERION_ALREADY_EXISTS.create(SplatcraftStageCommand.colorName(color));
        }

        SplatcraftScoreboardHandler.createColorCriterion(color);
        SplatcraftSaveData.get(context.getSource().getServer()).addInitializedColorScore(color);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.colorscores.add.success",
                        SplatcraftStageCommand.colorName(color)),
                true);
        return color;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int color = parseColorCriterion(StringArgumentType.getString(context, "color"));
        SplatcraftScoreboardHandler.removeColorCriterion(color);
        SplatcraftSaveData.get(context.getSource().getServer()).removeInitializedColorScore(color);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.colorscores.remove.success",
                        SplatcraftStageCommand.colorName(color)),
                true);
        return color;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        Collection<Integer> colors = SplatcraftScoreboardHandler.colors();
        if (colors.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.colorscores.list.empty"),
                    false);
        } else {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.colorscores.list.count", colors.size()),
                    false);
            colors.forEach(color -> context.getSource().sendSuccess(
                    () -> Component.translatable(
                            "commands.colorscores.list.entry",
                            SplatcraftScoreboardHandler.getColorIdentifier(color),
                            SplatcraftStageCommand.colorName(color)),
                    false));
        }
        return colors.size();
    }

    private static int parseColorCriterion(String value) throws CommandSyntaxException {
        String trimmed = value.trim();
        for (int color : SplatcraftScoreboardHandler.getCriteriaKeySet()) {
            if (SplatcraftScoreboardHandler.getColorIdentifier(color).equals(trimmed)) {
                return color;
            }
        }
        return SplatcraftStageCommand.parseColor(trimmed);
    }
}
