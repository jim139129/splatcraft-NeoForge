package net.splatcraft.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.data.Stage;

public final class SplatcraftReplaceColorCommand {
    private SplatcraftReplaceColorCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("replacecolor").requires(source -> source.hasPermission(2))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .then(colorArgument("color")
                                        .executes(context -> replaceRange(context, ReplaceMode.ALL))
                                        .then(Commands.literal("only")
                                                .then(colorArgument("affectedColor")
                                                        .executes(context -> replaceRange(context, ReplaceMode.ONLY))))
                                        .then(Commands.literal("keep")
                                                .then(colorArgument("affectedColor")
                                                        .executes(context -> replaceRange(context, ReplaceMode.KEEP)))))))
                .then(SplatcraftStageCommand.stageId("stage")
                        .then(colorArgument("color")
                                .executes(context -> replaceStage(context, ReplaceMode.ALL))
                                .then(Commands.literal("only")
                                        .then(stageColorOrTeamArgument("affected")
                                                .executes(context -> replaceStage(context, ReplaceMode.ONLY))))
                                .then(Commands.literal("keep")
                                        .then(stageColorOrTeamArgument("affected")
                                                .executes(context -> replaceStage(context, ReplaceMode.KEEP)))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> colorArgument(
            String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        InkColorData.builtInColors().keySet().stream().map(id -> id.getPath()),
                        builder));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> stageColorOrTeamArgument(
            String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> {
                    String stageId = StringArgumentType.getString(context, "stage");
                    Stage stage = SplatcraftSaveData.get(context.getSource().getServer()).stages().get(stageId);
                    if (stage != null) {
                        SharedSuggestionProvider.suggest(stage.teams().keySet(), builder);
                    }
                    return SharedSuggestionProvider.suggest(
                            InkColorData.builtInColors().keySet().stream().map(id -> id.getPath()),
                            builder);
                });
    }

    private static int replaceRange(CommandContext<CommandSourceStack> context, ReplaceMode mode)
            throws CommandSyntaxException {
        int color = SplatcraftStageCommand.parseColor(StringArgumentType.getString(context, "color"));
        int affectedColor = mode == ReplaceMode.ALL
                ? -1
                : SplatcraftStageCommand.parseColor(StringArgumentType.getString(context, "affectedColor"));

        return replaceColor(
                context.getSource(),
                context.getSource().getLevel(),
                BlockPosArgument.getBlockPos(context, "from"),
                BlockPosArgument.getBlockPos(context, "to"),
                color,
                new ReplaceTarget(mode, affectedColor, ""));
    }

    private static int replaceStage(CommandContext<CommandSourceStack> context, ReplaceMode mode)
            throws CommandSyntaxException {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = SplatcraftStageCommand.requireStage(context.getSource(), stageId);
        int color = SplatcraftStageCommand.parseColor(StringArgumentType.getString(context, "color"));
        ReplaceTarget target = stageTarget(context, stage, mode);

        ServerLevel level = SplatcraftStageCommand.stageLevel(context.getSource(), stage);
        if (level == null) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("status.change_color.out_of_world"),
                    true);
            return 0;
        }

        int count = replaceColor(
                context.getSource(),
                level,
                stage.cornerA(),
                stage.cornerB(),
                color,
                target);
        if (!target.team().isEmpty()) {
            stage.setTeamColor(target.team(), color);
            SplatcraftSaveData.get(context.getSource().getServer()).setDirty();
            SplatcraftStageCommand.syncStages(context.getSource());
        }
        return count;
    }

    private static ReplaceTarget stageTarget(
            CommandContext<CommandSourceStack> context,
            Stage stage,
            ReplaceMode mode) throws CommandSyntaxException {
        if (mode == ReplaceMode.ALL) {
            return new ReplaceTarget(mode, -1, "");
        }

        String affected = StringArgumentType.getString(context, "affected");
        if (stage.hasTeam(affected)) {
            return new ReplaceTarget(mode, stage.getTeamColor(affected), affected);
        }
        return new ReplaceTarget(mode, SplatcraftStageCommand.parseColor(affected), "");
    }

    private static int replaceColor(
            CommandSourceStack source,
            Level level,
            BlockPos from,
            BlockPos to,
            int color,
            ReplaceTarget target) {
        if (!InkAreaOperations.inWorldBounds(level, from, to)) {
            source.sendSuccess(() -> Component.translatable("status.change_color.out_of_world"), true);
            return 0;
        }

        int count = InkAreaOperations.visitColorBlocks(level, from, to, block -> {
            if (block.color() == color || !target.matches(block)) {
                return false;
            }

            block.setColor(color);
            return true;
        });
        source.sendSuccess(
                () -> Component.translatable(
                        "status.change_color.success",
                        count,
                        SplatcraftStageCommand.colorName(color)),
                true);
        return count;
    }

    private enum ReplaceMode {
        ALL,
        ONLY,
        KEEP
    }

    private record ReplaceTarget(ReplaceMode mode, int color, String team) {
        boolean matches(InkAreaOperations.TeamInkColorBlock block) {
            return switch (mode) {
                case ALL -> true;
                case ONLY -> team.isEmpty() ? block.color() == color : block.team().equals(team);
                case KEEP -> team.isEmpty() ? block.color() != color : !block.team().equals(team);
            };
        }
    }
}
