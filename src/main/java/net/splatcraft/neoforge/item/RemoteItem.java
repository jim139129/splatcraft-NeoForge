package net.splatcraft.neoforge.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.data.ClientStageCache;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.data.Stage;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public abstract class RemoteItem extends Item {
    private static final String MODE_KEY = "Mode";
    private static final String POINT_A_KEY = "PointA";
    private static final String POINT_B_KEY = "PointB";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String STAGE_KEY = "Stage";
    private static final String TARGETS_KEY = "Targets";
    protected static final Collection<ServerPlayer> ALL_TARGETS = Collections.unmodifiableList(new java.util.ArrayList<>());

    private final int totalModes;

    protected RemoteItem(Properties properties) {
        this(properties, 1);
    }

    protected RemoteItem(Properties properties, int totalModes) {
        super(properties.stacksTo(1));
        this.totalModes = Math.max(1, totalModes);
    }

    public static boolean hasCoordSet(ItemStack stack) {
        CompoundTag tag = customData(stack);
        return tag.contains(STAGE_KEY, CompoundTag.TAG_STRING)
                || tag.contains(POINT_A_KEY, CompoundTag.TAG_COMPOUND)
                && tag.contains(POINT_B_KEY, CompoundTag.TAG_COMPOUND);
    }

    public static Optional<CoordSet> coordSet(Level level, ItemStack stack) {
        CompoundTag tag = customData(stack);
        if (!hasCoordSet(stack)) {
            return Optional.empty();
        }

        if (tag.contains(STAGE_KEY, CompoundTag.TAG_STRING)) {
            return stageCoordSet(level, tag.getString(STAGE_KEY));
        }

        return rawCoordSet(tag);
    }

    public static int mode(ItemStack stack) {
        return customData(stack).getInt(MODE_KEY);
    }

    public static void setMode(ItemStack stack, int mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, mode));
    }

    public static int cycleMode(ItemStack stack, int totalModes) {
        int mode = (mode(stack) + 1) % Math.max(1, totalModes);
        setMode(stack, mode);
        return mode;
    }

    public abstract RemoteResult onRemoteUse(
            Level level,
            BlockPos pointA,
            BlockPos pointB,
            ItemStack stack,
            int color,
            int mode,
            Collection<ServerPlayer> targets);

    public RemoteResult onRemoteUse(Level level, ItemStack stack, int color, Vec3 origin, Entity user) {
        Optional<RemoteArea> area = remoteArea(level, stack);
        if (area.isEmpty()) {
            return RemoteResult.fail(Component.translatable("status.remote.undefined_area"));
        }

        Collection<ServerPlayer> targets;
        try {
            targets = remoteTargets(level, stack, origin, user);
        } catch (CommandSyntaxException e) {
            return RemoteResult.fail(Component.literal(e.getMessage()));
        }

        RemoteArea remoteArea = area.get();
        CoordSet coordSet = remoteArea.coordSet();
        return onRemoteUse(
                level,
                remoteArea.level(),
                coordSet.pointA(),
                coordSet.pointB(),
                stack,
                color,
                mode(stack),
                targets);
    }

    protected RemoteResult onRemoteUse(
            Level usedOnLevel,
            Level targetLevel,
            BlockPos pointA,
            BlockPos pointB,
            ItemStack stack,
            int color,
            int mode,
            Collection<ServerPlayer> targets) {
        return onRemoteUse(targetLevel, pointA, pointB, stack, color, mode, targets);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (hasCoordSet(stack)) {
            return InteractionResult.PASS;
        }

        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!addCoord(context.getLevel(), stack, context.getClickedPos())) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player != null) {
            BlockPos pos = context.getClickedPos();
            String key = hasSecondPoint(stack) ? "b" : "a";
            player.displayClientMessage(
                    Component.translatable("status.coord_set." + key, pos.getX(), pos.getY(), pos.getZ()),
                    true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && totalModes > 1) {
            int nextMode = cycleMode(stack, totalModes);
            player.displayClientMessage(
                    Component.translatable(
                            "status.remote_mode",
                            Component.translatable(getDescriptionId(stack) + ".mode." + nextMode)),
                    true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (hasCoordSet(stack)) {
            if (!level.isClientSide) {
                RemoteResult result = onRemoteUse(level, stack, SplatcraftPlayerInfoEvents.color(player), player.position(), player);
                if (result.output() != null) {
                    player.displayClientMessage(result.output(), true);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.REMOTE_USE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
                return new InteractionResultHolder<>(result.success() ? InteractionResult.SUCCESS : InteractionResult.FAIL, stack);
            }
            return InteractionResultHolder.success(stack);
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag tag = customData(stack);
        if (tag.contains(STAGE_KEY, CompoundTag.TAG_STRING)) {
            coordSet(context.level(), stack).ifPresentOrElse(
                    coords -> tooltip.add(Component.translatable(
                            "item.remote.stage",
                            tag.getString(STAGE_KEY),
                            coords.pointA().getX(),
                            coords.pointA().getY(),
                            coords.pointA().getZ(),
                            coords.pointB().getX(),
                            coords.pointB().getY(),
                            coords.pointB().getZ())),
                    () -> tooltip.add(Component.translatable("item.remote.coords.invalid").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)));
        } else if (hasCoordSet(stack)) {
            coordSet(context.level(), stack).ifPresent(coords -> tooltip.add(Component.translatable(
                    "item.remote.coords.b",
                    coords.pointA().getX(),
                    coords.pointA().getY(),
                    coords.pointA().getZ(),
                    coords.pointB().getX(),
                    coords.pointB().getY(),
                    coords.pointB().getZ())));
        } else if (tag.contains(POINT_A_KEY, CompoundTag.TAG_COMPOUND)) {
            NbtUtils.readBlockPos(tag, POINT_A_KEY).ifPresent(pos -> tooltip.add(Component.translatable(
                    "item.remote.coords.a",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ())));
        }
        if (hasTargetSelector(stack)) {
            tooltip.add(Component.translatable("item.remote.targets", tag.getString(TARGETS_KEY))
                    .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.ITALIC));
        }
        if (!tag.contains(STAGE_KEY, CompoundTag.TAG_STRING) && hasWrongDimension(stack, context.level())) {
            tooltip.add(Component.translatable("item.remote.coords.invalid").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        }
    }

    private static boolean addCoord(Level level, ItemStack stack, BlockPos pos) {
        if (hasCoordSet(stack)) {
            return false;
        }

        ResourceLocation dimension = level.dimension().location();
        CompoundTag tag = customData(stack);
        ResourceLocation storedDimension = tag.contains(DIMENSION_KEY, CompoundTag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(DIMENSION_KEY))
                : null;
        if (storedDimension != null && !storedDimension.equals(dimension)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            if (!data.contains(DIMENSION_KEY, CompoundTag.TAG_STRING)) {
                data.putString(DIMENSION_KEY, dimension.toString());
            }
            data.put(data.contains(POINT_A_KEY, CompoundTag.TAG_COMPOUND) ? POINT_B_KEY : POINT_A_KEY, NbtUtils.writeBlockPos(pos));
        });
        return true;
    }

    private static boolean hasSecondPoint(ItemStack stack) {
        return customData(stack).contains(POINT_B_KEY, CompoundTag.TAG_COMPOUND);
    }

    private static boolean hasWrongDimension(ItemStack stack, Level level) {
        if (level == null) {
            return false;
        }
        CompoundTag tag = customData(stack);
        ResourceLocation storedDimension = storedDimension(tag);
        return storedDimension != null
                && !storedDimension.equals(level.dimension().location())
                && level.getServer() != null
                && remoteLevel(level, storedDimension).isEmpty();
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    protected static boolean hasTargetSelector(ItemStack stack) {
        return targetSelector(stack).map(selector -> !selector.isEmpty()).orElse(false);
    }

    protected static boolean targetsAllPlayers(Collection<ServerPlayer> targets) {
        return targets == ALL_TARGETS;
    }

    private static Optional<String> targetSelector(ItemStack stack) {
        CompoundTag tag = customData(stack);
        if (!tag.contains(TARGETS_KEY, CompoundTag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.of(tag.getString(TARGETS_KEY).trim());
    }

    private Collection<ServerPlayer> remoteTargets(Level level, ItemStack stack, Vec3 origin, Entity user)
            throws CommandSyntaxException {
        Optional<String> selector = targetSelector(stack);
        if (selector.isEmpty() || selector.get().isEmpty() || !(level instanceof ServerLevel serverLevel)) {
            return ALL_TARGETS;
        }

        CommandSourceStack source = new CommandSourceStack(
                CommandSource.NULL,
                origin,
                Vec2.ZERO,
                serverLevel,
                2,
                getName(stack).getString(),
                getName(stack),
                serverLevel.getServer(),
                user);
        return EntityArgument.players().parse(new StringReader(selector.get())).findPlayers(source);
    }

    private static Optional<RemoteArea> remoteArea(Level level, ItemStack stack) {
        if (level == null) {
            return Optional.empty();
        }

        CompoundTag tag = customData(stack);
        if (!hasCoordSet(stack)) {
            return Optional.empty();
        }

        if (tag.contains(STAGE_KEY, CompoundTag.TAG_STRING)) {
            return stageArea(level, tag.getString(STAGE_KEY));
        }

        Optional<CoordSet> coords = rawCoordSet(tag);
        Optional<Level> remoteLevel = remoteLevel(level, storedDimension(tag));
        return coords.flatMap(coordSet -> remoteLevel.map(targetLevel -> new RemoteArea(targetLevel, coordSet)));
    }

    private static Optional<CoordSet> stageCoordSet(Level level, String stageId) {
        return stage(level, stageId)
                .map(stage -> new CoordSet(stage.cornerA(), stage.cornerB()));
    }

    private static Optional<RemoteArea> stageArea(Level level, String stageId) {
        return stage(level, stageId)
                .flatMap(stage -> remoteLevel(level, stage.dimension())
                        .map(targetLevel -> new RemoteArea(
                                targetLevel,
                                new CoordSet(stage.cornerA(), stage.cornerB()))));
    }

    private static Optional<Stage> stage(Level level, String stageId) {
        if (level == null || level.isClientSide()) {
            return ClientStageCache.stage(stageId);
        }

        return SplatcraftSaveData.get(level)
                .map(data -> data.stages().get(stageId));
    }

    private static Optional<CoordSet> rawCoordSet(CompoundTag tag) {
        Optional<BlockPos> pointA = NbtUtils.readBlockPos(tag, POINT_A_KEY);
        Optional<BlockPos> pointB = NbtUtils.readBlockPos(tag, POINT_B_KEY);
        return pointA.flatMap(a -> pointB.map(b -> new CoordSet(a, b)));
    }

    private static Optional<Level> remoteLevel(Level fallbackLevel, ResourceLocation dimension) {
        if (fallbackLevel == null) {
            return Optional.empty();
        }
        if (dimension == null || dimension.equals(fallbackLevel.dimension().location())) {
            return Optional.of(fallbackLevel);
        }
        if (fallbackLevel.getServer() == null) {
            return Optional.empty();
        }

        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimension);
        return Optional.ofNullable(fallbackLevel.getServer().getLevel(levelKey));
    }

    private static ResourceLocation storedDimension(CompoundTag tag) {
        return tag.contains(DIMENSION_KEY, CompoundTag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(DIMENSION_KEY))
                : null;
    }

    public record CoordSet(BlockPos pointA, BlockPos pointB) {
    }

    private record RemoteArea(Level level, CoordSet coordSet) {
    }

    public static class RemoteResult {
        private final boolean success;
        private final Component output;
        private final int commandResult;
        private final int comparatorResult;

        protected RemoteResult(boolean success, Component output, int commandResult, int comparatorResult) {
            this.success = success;
            this.output = output;
            this.commandResult = commandResult;
            this.comparatorResult = comparatorResult;
        }

        public static RemoteResult success(Component output, int commandResult, int comparatorResult) {
            return new RemoteResult(true, output, commandResult, comparatorResult);
        }

        public static RemoteResult fail(Component output) {
            return new RemoteResult(false, output, 0, 0);
        }

        public boolean success() {
            return success;
        }

        public Component output() {
            return output;
        }

        public int commandResult() {
            return commandResult;
        }

        public int comparatorResult() {
            return comparatorResult;
        }
    }
}
