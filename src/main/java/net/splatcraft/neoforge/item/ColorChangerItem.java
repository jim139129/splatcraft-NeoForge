package net.splatcraft.neoforge.item;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.data.ClientStageCache;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.data.Stage;
import net.splatcraft.neoforge.network.payload.StageListPayload;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.worldink.InkAreaActions;

public class ColorChangerItem extends RemoteItem {
    private static final int MODE_ALL = 0;
    private static final int MODE_ONLY_PLAYER_COLOR = 1;
    private static final int MODE_KEEP_PLAYER_COLOR = 2;
    private static final String STAGE_KEY = "Stage";
    private static final String TEAM_KEY = "Team";

    public ColorChangerItem(Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON), 3);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player player && !InkColorComponent.isColorLocked(stack)) {
            InkColorComponent.setColor(stack, SplatcraftPlayerInfoEvents.color(player));
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        InkColorComponent.lockFromInkwell(stack, entity);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boundTeam(context.level(), stack).ifPresent(team -> {
            MutableComponent teamText = Component.translatable("item.remote.team", team.teamId())
                    .withStyle(ChatFormatting.ITALIC);
            tooltip.add(team.color() >= 0
                    ? teamText.withColor(team.color())
                    : teamText.withStyle(ChatFormatting.DARK_BLUE));
        });
        if (InkColorComponent.isColorLocked(stack)) {
            tooltip.add(colorName(InkColorComponent.colorOrDefault(stack)).withStyle(ChatFormatting.GRAY));
        }
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
        if (!InkAreaActions.inWorldBounds(level, pointA, pointB)) {
            return RemoteResult.fail(Component.translatable("status.change_color.out_of_world"));
        }

        int targetColor = InkColorComponent.color(stack).orElse(color);
        int affectedColor = color;
        int normalizedMode = Math.floorMod(mode, 3);
        Optional<SplatcraftSaveData> saveData = SplatcraftSaveData.get(level);
        Optional<BoundTeam> boundTeam = boundTeam(level, stack);
        Set<StageTeamChange> changedTeams = new HashSet<>();
        int count = InkAreaActions.visitColorBlocks(level, pointA, pointB, block -> {
            if (block.color() == targetColor || !matches(normalizedMode, block, affectedColor, boundTeam)) {
                return false;
            }

            rememberStageTeamChange(saveData, level, block, changedTeams);
            block.setColor(targetColor);
            return true;
        });
        updateStageTeams(level, saveData, changedTeams, targetColor);
        updateBoundStageTeam(level, saveData, boundTeam, normalizedMode, targetColor);
        int volume = Math.max(1, InkAreaActions.volume(pointA, pointB));
        return RemoteResult.success(
                Component.translatable("status.change_color.success", count, colorName(targetColor)),
                count,
                count * 15 / volume);
    }

    private static boolean matches(
            int mode,
            InkAreaActions.TeamInkColorBlock block,
            int affectedColor,
            Optional<BoundTeam> boundTeam) {
        return switch (mode) {
            case MODE_ONLY_PLAYER_COLOR -> boundTeam
                    .map(team -> block.team().equals(team.teamId()))
                    .orElse(block.color() == affectedColor);
            case MODE_KEEP_PLAYER_COLOR -> boundTeam
                    .map(team -> !block.team().equals(team.teamId()))
                    .orElse(block.color() != affectedColor);
            case MODE_ALL -> true;
            default -> true;
        };
    }

    private static void rememberStageTeamChange(
            Optional<SplatcraftSaveData> saveData,
            Level level,
            InkAreaActions.TeamInkColorBlock block,
            Set<StageTeamChange> changedTeams) {
        String team = block.team();
        if (team.isEmpty()) {
            return;
        }

        saveData.flatMap(data -> data.localStage(level, block.pos()))
                .filter(stage -> stage.hasTeam(team) && stage.getTeamColor(team) == block.color())
                .ifPresent(stage -> changedTeams.add(new StageTeamChange(stage, team)));
    }

    private static void updateStageTeams(
            Level level,
            Optional<SplatcraftSaveData> saveData,
            Set<StageTeamChange> changedTeams,
            int targetColor) {
        if (changedTeams.isEmpty()) {
            return;
        }

        saveData.ifPresent(data -> {
            boolean changed = false;
            for (StageTeamChange change : changedTeams) {
                if (change.stage().hasTeam(change.team()) && change.stage().getTeamColor(change.team()) != targetColor) {
                    change.stage().setTeamColor(change.team(), targetColor);
                    changed = true;
                }
            }
            if (changed) {
                data.setDirty();
                syncStages(level);
            }
        });
    }

    private static void updateBoundStageTeam(
            Level level,
            Optional<SplatcraftSaveData> saveData,
            Optional<BoundTeam> boundTeam,
            int mode,
            int targetColor) {
        if (mode > MODE_ONLY_PLAYER_COLOR) {
            return;
        }

        saveData.ifPresent(data -> boundTeam
                .filter(team -> !team.stageId().isEmpty())
                .map(team -> new StageTeamChange(data.stages().get(team.stageId()), team.teamId()))
                .filter(change -> change.stage() != null
                        && change.stage().hasTeam(change.team())
                        && change.stage().getTeamColor(change.team()) != targetColor)
                .ifPresent(change -> {
                    change.stage().setTeamColor(change.team(), targetColor);
                    data.setDirty();
                    syncStages(level);
                }));
    }

    private static void syncStages(Level level) {
        if (level.getServer() != null) {
            StageListPayload.sendToAll(level.getServer());
        }
    }

    private static Optional<BoundTeam> boundTeam(Level level, ItemStack stack) {
        CompoundTag tag = customData(stack);
        if (!tag.contains(TEAM_KEY, CompoundTag.TAG_STRING) || tag.getString(TEAM_KEY).isBlank()) {
            return Optional.empty();
        }

        String stageId = tag.contains(STAGE_KEY, CompoundTag.TAG_STRING) ? tag.getString(STAGE_KEY) : "";
        String teamId = tag.getString(TEAM_KEY);
        int teamColor = stageId.isEmpty()
                ? -1
                : stage(level, stageId)
                        .filter(stage -> stage.hasTeam(teamId))
                        .map(stage -> stage.getTeamColor(teamId))
                        .orElse(-1);
        return Optional.of(new BoundTeam(stageId, teamId, teamColor));
    }

    private static Optional<Stage> stage(Level level, String stageId) {
        return level == null || level.isClientSide()
                ? ClientStageCache.stage(stageId)
                : SplatcraftSaveData.get(level)
                        .map(data -> data.stages().get(stageId));
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static MutableComponent colorName(int color) {
        return InkColorData.builtInId(color)
                .map(ColorChangerItem::colorName)
                .orElseGet(() -> Component.literal(String.format("#%06X", color)));
    }

    private static MutableComponent colorName(ResourceLocation id) {
        return Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath());
    }

    private record BoundTeam(String stageId, String teamId, int color) {
    }

    private record StageTeamChange(Stage stage, String team) {
    }
}
