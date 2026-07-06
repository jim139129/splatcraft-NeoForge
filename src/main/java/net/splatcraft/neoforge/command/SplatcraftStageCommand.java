package net.splatcraft.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.splatcraft.neoforge.blockentity.SpawnPadBlockEntity;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.data.Stage;
import net.splatcraft.neoforge.network.payload.StageListPayload;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public final class SplatcraftStageCommand {
    static final DynamicCommandExceptionType STAGE_NOT_FOUND =
            new DynamicCommandExceptionType(stage -> Component.translatable("arg.stage.notFound", stage));
    private static final DynamicCommandExceptionType STAGE_ALREADY_EXISTS =
            new DynamicCommandExceptionType(stage -> Component.translatable("arg.stage.alreadyExists", stage));
    private static final DynamicCommandExceptionType SETTING_NOT_FOUND =
            new DynamicCommandExceptionType(setting -> Component.translatable("arg.stageSetting.notFound", setting));
    static final DynamicCommandExceptionType TEAM_NOT_FOUND =
            new DynamicCommandExceptionType(args -> Component.translatable(
                    "arg.stageTeam.notFound",
                    ((Object[]) args)[0],
                    ((Object[]) args)[1]));
    static final DynamicCommandExceptionType COLOR_NOT_FOUND =
            new DynamicCommandExceptionType(color -> Component.translatable("arg.inkColor.notFound", color));
    private static final DynamicCommandExceptionType NO_SPAWN_PADS_FOUND =
            new DynamicCommandExceptionType(stage -> Component.translatable("arg.stageWarp.noSpawnPads", stage));
    private static final DynamicCommandExceptionType NO_PLAYERS_FOUND =
            new DynamicCommandExceptionType(stage -> Component.translatable("arg.stageWarp.noPlayers", stage));

    private SplatcraftStageCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command =
                Commands.literal("stage").requires(source -> source.hasPermission(2));

        command.then(Commands.literal("add")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(SplatcraftStageCommand::add)))));
        command.then(Commands.literal("remove")
                .then(stageId("stage").executes(SplatcraftStageCommand::remove)));
        command.then(Commands.literal("list").executes(SplatcraftStageCommand::list));
        command.then(Commands.literal("settings").then(stageId("stage")
                .then(Commands.literal("cornerA")
                        .executes(context -> getStageCorner(context, true))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> setStageCorner(context, true))))
                .then(Commands.literal("cornerB")
                        .executes(context -> getStageCorner(context, false))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> setStageCorner(context, false))))
                .then(stageSetting("setting")
                        .executes(SplatcraftStageCommand::getSetting)
                        .then(Commands.literal("true").executes(context -> setSetting(context, true)))
                        .then(Commands.literal("false").executes(context -> setSetting(context, false)))
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                .executes(SplatcraftStageCommand::setIntegerSetting))
                        .then(Commands.literal("default").executes(SplatcraftStageCommand::clearSetting)))));
        command.then(Commands.literal("teams").then(stageId("stage")
                .then(Commands.literal("set")
                        .then(stageTeam("teamName", "stage")
                                .then(Commands.argument("teamColor", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                InkColorData.builtInColors().keySet().stream().map(Object::toString),
                                                builder))
                                        .executes(SplatcraftStageCommand::setTeam))))
                .then(Commands.literal("remove")
                        .then(stageTeam("teamName", "stage").executes(SplatcraftStageCommand::removeTeam)))
                .then(Commands.literal("get")
                        .then(stageTeam("teamName", "stage").executes(SplatcraftStageCommand::getTeam)))));
        command.then(Commands.literal("warp").then(stageId("stage")
                .executes(SplatcraftStageCommand::warpSelf)
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> warp(context, false))
                        .then(Commands.argument("setSpawn", BoolArgumentType.bool())
                                .executes(context -> warp(context, BoolArgumentType.getBool(context, "setSpawn")))
                                .then(Commands.literal("self").executes(context ->
                                        warp(context, BoolArgumentType.getBool(context, "setSpawn"))))
                                .then(Commands.literal("any").executes(context ->
                                        warpAny(context, BoolArgumentType.getBool(context, "setSpawn"))))
                                .then(Commands.literal("color")
                                        .then(Commands.argument("color", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        InkColorData.builtInColors().keySet().stream().map(Object::toString),
                                                        builder))
                                                .executes(context -> warp(
                                                        context,
                                                        BoolArgumentType.getBool(context, "setSpawn"),
                                                        parseColor(StringArgumentType.getString(context, "color"))))))
                                .then(Commands.literal("team")
                                        .then(stageTeam("team", "stage").executes(context -> warpToTeam(
                                                context,
                                                BoolArgumentType.getBool(context, "setSpawn"),
                                                StringArgumentType.getString(context, "team")))))))));

        dispatcher.register(command);
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> stageId(String argumentName) {
        return Commands.argument(argumentName, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        SplatcraftSaveData.get(context.getSource().getServer()).stages().keySet(),
                        builder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> stageSetting(String argumentName) {
        return Commands.argument(argumentName, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Stage.VALID_SETTINGS, builder));
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> stageTeam(String argumentName, String stageArgumentName) {
        return Commands.argument(argumentName, StringArgumentType.word())
                .suggests((context, builder) -> {
                    try {
                        Stage stage = SplatcraftSaveData.get(context.getSource().getServer())
                                .stages()
                                .get(StringArgumentType.getString(context, stageArgumentName));
                        return stage == null
                                ? builder.buildFuture()
                                : SharedSuggestionProvider.suggest(stage.teams().keySet(), builder);
                    } catch (IllegalArgumentException ignored) {
                        return builder.buildFuture();
                    }
                });
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return add(
                context.getSource(),
                StringArgumentType.getString(context, "name"),
                BlockPosArgument.getLoadedBlockPos(context, "from"),
                BlockPosArgument.getLoadedBlockPos(context, "to"));
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return remove(context.getSource(), StringArgumentType.getString(context, "stage"));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        Map<String, Stage> stages = SplatcraftSaveData.get(context.getSource().getServer()).stages();
        if (stages.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No stages configured"), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("Stages: " + String.join(", ", stages.keySet())), false);
        return stages.size();
    }

    private static int setSetting(CommandContext<CommandSourceStack> context, Boolean value) throws CommandSyntaxException {
        return setSetting(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "setting"),
                value);
    }

    private static int getSetting(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return getSetting(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "setting"));
    }

    private static int setIntegerSetting(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setIntegerSetting(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "setting"),
                IntegerArgumentType.getInteger(context, "value"));
    }

    private static int clearSetting(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return clearSetting(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "setting"));
    }

    private static int setStageCorner(CommandContext<CommandSourceStack> context, boolean cornerA)
            throws CommandSyntaxException {
        return setStageCorner(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                BlockPosArgument.getLoadedBlockPos(context, "pos"),
                cornerA);
    }

    private static int getStageCorner(CommandContext<CommandSourceStack> context, boolean cornerA)
            throws CommandSyntaxException {
        return getStageCorner(context.getSource(), StringArgumentType.getString(context, "stage"), cornerA);
    }

    private static int setTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setTeam(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "teamName"),
                parseColor(StringArgumentType.getString(context, "teamColor")));
    }

    private static int removeTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return removeTeam(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "teamName"));
    }

    private static int getTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return getTeam(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                StringArgumentType.getString(context, "teamName"));
    }

    private static int warpSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return warpPlayers(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                Collections.singleton(context.getSource().getPlayerOrException()),
                false);
    }

    private static int warp(CommandContext<CommandSourceStack> context, boolean setSpawn) throws CommandSyntaxException {
        return warpPlayers(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                EntityArgument.getPlayers(context, "players"),
                setSpawn);
    }

    private static int warp(CommandContext<CommandSourceStack> context, boolean setSpawn, int color)
            throws CommandSyntaxException {
        return warpPlayers(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                EntityArgument.getPlayers(context, "players"),
                setSpawn,
                color);
    }

    private static int warpAny(CommandContext<CommandSourceStack> context, boolean setSpawn)
            throws CommandSyntaxException {
        return warpPlayersToAny(
                context.getSource(),
                StringArgumentType.getString(context, "stage"),
                EntityArgument.getPlayers(context, "players"),
                setSpawn);
    }

    private static int warpToTeam(CommandContext<CommandSourceStack> context, boolean setSpawn, String teamId)
            throws CommandSyntaxException {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = requireStage(context.getSource(), stageId);
        if (!stage.hasTeam(teamId)) {
            throw TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
        }

        return warpPlayers(
                context.getSource(),
                stageId,
                EntityArgument.getPlayers(context, "players"),
                setSpawn,
                stage.getTeamColor(teamId));
    }

    private static int add(CommandSourceStack source, String stageId, BlockPos from, BlockPos to)
            throws CommandSyntaxException {
        SplatcraftSaveData data = SplatcraftSaveData.get(source.getServer());
        if (data.stages().containsKey(stageId)) {
            throw STAGE_ALREADY_EXISTS.create(stageId);
        }

        data.stages().put(stageId, new Stage(source.getLevel(), from, to));
        data.setDirty();
        syncStages(source);
        source.sendSuccess(() -> Component.translatable("commands.stage.add.success", stageId), true);
        return 1;
    }

    private static int remove(CommandSourceStack source, String stageId) throws CommandSyntaxException {
        SplatcraftSaveData data = SplatcraftSaveData.get(source.getServer());
        if (data.stages().remove(stageId) == null) {
            throw STAGE_NOT_FOUND.create(stageId);
        }

        data.setDirty();
        syncStages(source);
        source.sendSuccess(() -> Component.translatable("commands.stage.remove.success", stageId), true);
        return 1;
    }

    private static int setSetting(CommandSourceStack source, String stageId, String setting, Boolean value)
            throws CommandSyntaxException {
        requireValidBooleanSetting(setting);
        Stage stage = requireStage(source, stageId);

        stage.applySetting(setting, value);
        SplatcraftSaveData.get(source.getServer()).setDirty();
        syncStages(source);
        if (value == null) {
            source.sendSuccess(
                    () -> Component.translatable("commands.stage.setting.success.default", setting, stageId),
                    true);
        } else {
            source.sendSuccess(
                    () -> Component.translatable("commands.stage.setting.success", setting, stageId, value),
                    true);
        }
        return 1;
    }

    private static int setIntegerSetting(CommandSourceStack source, String stageId, String setting, int value)
            throws CommandSyntaxException {
        requireValidIntegerSetting(setting);
        Stage stage = requireStage(source, stageId);

        stage.applySetting(setting, value);
        SplatcraftSaveData.get(source.getServer()).setDirty();
        syncStages(source);
        source.sendSuccess(
                () -> Component.translatable("commands.stage.setting.success", setting, stageId, value),
                true);
        return 1;
    }

    private static int clearSetting(CommandSourceStack source, String stageId, String setting)
            throws CommandSyntaxException {
        requireValidSetting(setting);
        Stage stage = requireStage(source, stageId);

        if (Stage.VALID_INTEGER_SETTINGS.contains(setting)) {
            stage.applySetting(setting, (Integer) null);
        } else {
            stage.applySetting(setting, (Boolean) null);
        }

        SplatcraftSaveData.get(source.getServer()).setDirty();
        syncStages(source);
        source.sendSuccess(
                () -> Component.translatable("commands.stage.setting.success.default", setting, stageId),
                true);
        return 1;
    }

    private static int getSetting(CommandSourceStack source, String stageId, String setting)
            throws CommandSyntaxException {
        requireValidSetting(setting);
        Stage stage = requireStage(source, stageId);

        if (Stage.VALID_INTEGER_SETTINGS.contains(setting) && stage.hasIntegerSetting(setting)) {
            source.sendSuccess(
                    () -> Component.translatable("commands.stage.setting.get", setting, stageId, stage.getIntegerSetting(setting)),
                    true);
        } else if (stage.hasSetting(setting)) {
            source.sendSuccess(
                    () -> Component.translatable("commands.stage.setting.get", setting, stageId, stage.getSetting(setting)),
                    true);
        } else {
            source.sendSuccess(
                    () -> Component.translatable("commands.stage.setting.get.default", setting, stageId),
                    true);
        }
        return 1;
    }

    private static int setStageCorner(CommandSourceStack source, String stageId, BlockPos pos, boolean cornerA)
            throws CommandSyntaxException {
        Stage stage = requireStage(source, stageId);
        if (cornerA) {
            stage.setCornerA(pos);
        } else {
            stage.setCornerB(pos);
        }

        SplatcraftSaveData.get(source.getServer()).setDirty();
        syncStages(source);
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.stage.setting.area.success",
                        cornerA ? "A" : "B",
                        stageId,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()),
                true);
        return 1;
    }

    private static int getStageCorner(CommandSourceStack source, String stageId, boolean cornerA)
            throws CommandSyntaxException {
        Stage stage = requireStage(source, stageId);
        BlockPos pos = cornerA ? stage.cornerA() : stage.cornerB();
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.stage.setting.area.get",
                        cornerA ? "A" : "B",
                        stageId,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()),
                true);
        return 1;
    }

    private static int setTeam(CommandSourceStack source, String stageId, String teamId, int teamColor)
            throws CommandSyntaxException {
        Stage stage = requireStage(source, stageId);
        int affectedBlocks = visitStageInkColorBlocks(source, stage, block -> {
            if (block.color() == teamColor && !block.team().equals(teamId)) {
                block.setTeam(teamId);
                return true;
            }
            return false;
        });

        stage.setTeamColor(teamId, teamColor);
        SplatcraftSaveData.get(source.getServer()).setDirty();
        syncStages(source);
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.stage.teams.set.success",
                        affectedBlocks,
                        stageId,
                        coloredText(teamId, teamColor)),
                true);
        return 1;
    }

    private static int removeTeam(CommandSourceStack source, String stageId, String teamId)
            throws CommandSyntaxException {
        Stage stage = requireStage(source, stageId);
        if (!stage.hasTeam(teamId)) {
            throw TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
        }

        int teamColor = stage.getTeamColor(teamId);
        int affectedBlocks = visitStageInkColorBlocks(source, stage, block -> {
            if (block.color() == teamColor && block.team().equals(teamId)) {
                block.setTeam("");
                return true;
            }
            return false;
        });

        stage.removeTeam(teamId);
        SplatcraftSaveData.get(source.getServer()).setDirty();
        syncStages(source);
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.stage.teams.remove.success",
                        coloredText(teamId, teamColor),
                        stageId,
                        affectedBlocks),
                true);
        return 1;
    }

    private static int getTeam(CommandSourceStack source, String stageId, String teamId)
            throws CommandSyntaxException {
        Stage stage = requireStage(source, stageId);
        if (!stage.hasTeam(teamId)) {
            throw TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
        }

        int teamColor = stage.getTeamColor(teamId);
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.stage.teams.get.success",
                        coloredText(teamId, teamColor),
                        stageId,
                        colorName(teamColor)),
                true);
        return teamColor;
    }

    private static int warpPlayers(
            CommandSourceStack source,
            String stageId,
            Collection<ServerPlayer> targets,
            boolean setSpawn) throws CommandSyntaxException {
        return warpPlayers(source, stageId, targets, setSpawn, -1);
    }

    private static int warpPlayers(
            CommandSourceStack source,
            String stageId,
            Collection<ServerPlayer> targets,
            boolean setSpawn,
            int color) throws CommandSyntaxException {
        Stage stage = requireStage(source, stageId);
        ServerLevel stageLevel = requireStageLevel(source, stage);
        Map<Integer, List<SpawnPadBlockEntity>> spawnPads = collectSpawnPadsByColor(stageLevel, stage);
        if (spawnPads.isEmpty()) {
            throw NO_SPAWN_PADS_FOUND.create(stageId);
        }

        Map<Integer, Integer> playersTeleported = new HashMap<>();
        for (ServerPlayer player : targets) {
            int playerColor = color == -1 ? SplatcraftPlayerInfoEvents.color(player) : color;
            List<SpawnPadBlockEntity> matchingSpawnPads = spawnPads.get(playerColor);
            if (matchingSpawnPads == null || matchingSpawnPads.isEmpty()) {
                continue;
            }

            int index = playersTeleported.getOrDefault(playerColor, 0);
            teleportToSpawnPad(player, stageLevel, matchingSpawnPads.get(index % matchingSpawnPads.size()), setSpawn);
            playersTeleported.put(playerColor, index + 1);
        }

        int result = playersTeleported.values().stream().mapToInt(Integer::intValue).sum();
        if (result == 0) {
            throw NO_PLAYERS_FOUND.create(stageId);
        }

        source.sendSuccess(() -> Component.translatable("commands.stage.warp.success", result, stageId), true);
        return result;
    }

    private static int warpPlayersToAny(
            CommandSourceStack source,
            String stageId,
            Collection<ServerPlayer> targets,
            boolean setSpawn) throws CommandSyntaxException {
        Stage stage = requireStage(source, stageId);
        ServerLevel stageLevel = requireStageLevel(source, stage);
        List<SpawnPadBlockEntity> spawnPads = collectSpawnPads(stageLevel, stage);
        if (spawnPads.isEmpty()) {
            throw NO_SPAWN_PADS_FOUND.create(stageId);
        }

        int playersTeleported = 0;
        for (ServerPlayer player : targets) {
            teleportToSpawnPad(player, stageLevel, spawnPads.get(playersTeleported % spawnPads.size()), setSpawn);
            playersTeleported++;
        }

        if (playersTeleported == 0) {
            throw NO_PLAYERS_FOUND.create(stageId);
        }

        int result = playersTeleported;
        source.sendSuccess(() -> Component.translatable("commands.stage.warp.success", result, stageId), true);
        return result;
    }

    static Stage requireStage(CommandSourceStack source, String stageId) throws CommandSyntaxException {
        Stage stage = SplatcraftSaveData.get(source.getServer()).stages().get(stageId);
        if (stage == null) {
            throw STAGE_NOT_FOUND.create(stageId);
        }
        return stage;
    }

    private static void requireValidSetting(String setting) throws CommandSyntaxException {
        if (!Stage.VALID_SETTINGS.contains(setting)) {
            throw SETTING_NOT_FOUND.create(setting);
        }
    }

    private static void requireValidBooleanSetting(String setting) throws CommandSyntaxException {
        if (!Stage.VALID_BOOLEAN_SETTINGS.contains(setting)) {
            throw SETTING_NOT_FOUND.create(setting);
        }
    }

    private static void requireValidIntegerSetting(String setting) throws CommandSyntaxException {
        if (!Stage.VALID_INTEGER_SETTINGS.contains(setting)) {
            throw SETTING_NOT_FOUND.create(setting);
        }
    }

    static int visitStageInkColorBlocks(
            CommandSourceStack source,
            Stage stage,
            InkAreaOperations.InkColorBlockVisitor visitor) {
        ServerLevel level = stageLevel(source, stage);
        if (level == null) {
            return 0;
        }

        return InkAreaOperations.visitColorBlocks(level, stage.cornerA(), stage.cornerB(), visitor);
    }

    private static ServerLevel requireStageLevel(CommandSourceStack source, Stage stage) throws CommandSyntaxException {
        ServerLevel level = stageLevel(source, stage);
        if (level == null) {
            throw STAGE_NOT_FOUND.create(stage.dimension().toString());
        }
        return level;
    }

    static ServerLevel stageLevel(CommandSourceStack source, Stage stage) {
        return InkAreaOperations.stageLevel(source.getLevel(), stage);
    }

    public static void syncStages(CommandSourceStack source) {
        StageListPayload.sendToAll(source.getServer());
    }

    private static Map<Integer, List<SpawnPadBlockEntity>> collectSpawnPadsByColor(ServerLevel level, Stage stage) {
        Map<Integer, List<SpawnPadBlockEntity>> spawnPads = new HashMap<>();
        for (SpawnPadBlockEntity spawnPad : collectSpawnPads(level, stage)) {
            spawnPads.computeIfAbsent(spawnPad.effectiveColor(), color -> new ArrayList<>()).add(spawnPad);
        }
        return spawnPads;
    }

    private static List<SpawnPadBlockEntity> collectSpawnPads(ServerLevel level, Stage stage) {
        BlockPos min = InkAreaOperations.min(stage.cornerA(), stage.cornerB());
        BlockPos max = InkAreaOperations.max(stage.cornerA(), stage.cornerB());
        List<SpawnPadBlockEntity> spawnPads = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(pos.immutable()) instanceof SpawnPadBlockEntity spawnPad) {
                spawnPads.add(spawnPad);
            }
        }
        return spawnPads;
    }

    private static void teleportToSpawnPad(
            ServerPlayer player,
            ServerLevel stageLevel,
            SpawnPadBlockEntity spawnPad,
            boolean setSpawn) {
        BlockPos pos = spawnPad.getBlockPos();
        float yaw = spawnPadYaw(spawnPad);
        player.teleportTo(stageLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, yaw, 0.0F);
        if (setSpawn) {
            player.setRespawnPosition(stageLevel.dimension(), pos, yaw, false, true);
        }
    }

    private static float spawnPadYaw(SpawnPadBlockEntity spawnPad) {
        if (spawnPad.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = spawnPad.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
            return facing.toYRot();
        }
        return 0.0F;
    }

    static int parseColor(String value) throws CommandSyntaxException {
        String trimmed = value.trim();
        try {
            int color = Integer.parseInt(trimmed);
            if (color >= 0 && color <= 0xFFFFFF) {
                return color;
            }
        } catch (NumberFormatException ignored) {
        }

        return InkColorData.resolve(trimmed).orElseThrow(() -> COLOR_NOT_FOUND.create(trimmed));
    }

    static Component colorName(int color) {
        return InkColorData.builtInId(color)
                .<Component>map(id -> Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath()))
                .orElseGet(() -> Component.literal(String.format("#%06X", color)));
    }

    static MutableComponent coloredText(String text, int color) {
        return Component.literal(text).withColor(color);
    }

}
