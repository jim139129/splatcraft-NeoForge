package net.splatcraft.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.splatcraft.neoforge.data.Stage;

public final class SplatcraftClearInkCommand {
    private SplatcraftClearInkCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clearink").requires(source -> source.hasPermission(2))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .executes(SplatcraftClearInkCommand::clearRange)))
                .then(SplatcraftStageCommand.stageId("stage")
                        .executes(SplatcraftClearInkCommand::clearStage)));
    }

    private static int clearRange(CommandContext<CommandSourceStack> context) {
        return clearInk(
                context.getSource(),
                context.getSource().getLevel(),
                BlockPosArgument.getBlockPos(context, "from"),
                BlockPosArgument.getBlockPos(context, "to"));
    }

    private static int clearStage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = SplatcraftStageCommand.requireStage(context.getSource(), stageId);
        ServerLevel level = SplatcraftStageCommand.stageLevel(context.getSource(), stage);
        if (level == null) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("status.clear_ink.out_of_world"),
                    true);
            return 0;
        }

        return clearInk(context.getSource(), level, stage.cornerA(), stage.cornerB());
    }

    private static int clearInk(CommandSourceStack source, Level level, BlockPos from, BlockPos to) {
        if (!InkAreaOperations.inWorldBounds(level, from, to)) {
            source.sendSuccess(() -> Component.translatable("status.clear_ink.out_of_world"), true);
            return 0;
        }

        int count = InkAreaOperations.clearInk(level, from, to);
        if (count > 0) {
            source.sendSuccess(
                    () -> Component.translatable("status.clear_ink.success", count),
                    true);
        } else {
            source.sendSuccess(
                    () -> Component.translatable("status.clear_ink.no_ink"),
                    true);
        }
        return count;
    }
}
