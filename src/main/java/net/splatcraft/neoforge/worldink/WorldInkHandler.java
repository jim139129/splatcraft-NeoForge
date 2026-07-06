package net.splatcraft.neoforge.worldink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;

public final class WorldInkHandler {
    private static final int MAX_DECAYABLE_PER_CHUNK = 3;
    private static final int MAX_DECAYABLE_CHUNKS = 10;
    private static final int PLAYER_CHUNK_SAMPLE_RADIUS = 3;

    private WorldInkHandler() {
    }

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        checkForInkRemoval(level, event.getPos());
        for (Direction direction : event.getNotifiedSides()) {
            checkForInkRemoval(level, event.getPos().relative(direction));
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockPlaceContext placementContext = blockItem.updatePlacementContext(
                new BlockPlaceContext(event.getEntity(), event.getHand(), event.getItemStack(), event.getHitVec()));
        if (placementContext == null) {
            return;
        }

        BlockState placementState = blockItem.getBlock().getStateForPlacement(placementContext);
        BlockPos supportPos = placementContext.getClickedPos().below();
        if (placementState != null
                && InkBlockUtils.isBlockFoliage(placementState)
                && WorldInkStorage.isInked(event.getLevel(), supportPos)
                && SplatcraftGameRules.localizedBoolean(event.getLevel(), supportPos, SplatcraftGameRules.INK_DESTROYS_FOLIAGE)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()) {
            return;
        }

        List<ChunkPos> candidates = activeInkChunksNearPlayers(level);
        if (candidates.isEmpty()) {
            return;
        }

        RandomSource random = level.random;
        int maxChunkChecks = Math.min(random.nextInt(MAX_DECAYABLE_CHUNKS + 1), candidates.size());
        for (int index = 0; index < maxChunkChecks; index++) {
            ChunkPos chunkPos = candidates.get(random.nextInt(candidates.size()));
            WorldInkStorage.getLoaded(level, chunkPos).ifPresent(worldInk -> decayChunk(level, chunkPos, worldInk, random));
        }
    }

    private static void checkForInkRemoval(Level level, BlockPos pos) {
        if (WorldInkStorage.isInked(level, pos) && InkBlockUtils.isUninkable(level, pos)) {
            InkBlockUtils.clearInk(level, pos, true);
        }
    }

    private static List<ChunkPos> activeInkChunksNearPlayers(ServerLevel level) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            int centerX = SectionPos.blockToSectionCoord(player.getBlockX());
            int centerZ = SectionPos.blockToSectionCoord(player.getBlockZ());
            for (int x = centerX - PLAYER_CHUNK_SAMPLE_RADIUS; x <= centerX + PLAYER_CHUNK_SAMPLE_RADIUS; x++) {
                for (int z = centerZ - PLAYER_CHUNK_SAMPLE_RADIUS; z <= centerZ + PLAYER_CHUNK_SAMPLE_RADIUS; z++) {
                    ChunkPos chunkPos = new ChunkPos(x, z);
                    if (chunks.contains(chunkPos)) {
                        continue;
                    }
                    Optional<WorldInk> worldInk = WorldInkStorage.getLoaded(level, chunkPos);
                    if (worldInk.isPresent() && !worldInk.get().inkInChunk().isEmpty()) {
                        chunks.add(chunkPos);
                    }
                }
            }
        }
        return chunks;
    }

    private static void decayChunk(ServerLevel level, ChunkPos chunkPos, WorldInk worldInk, RandomSource random) {
        List<BlockPos> decayableInk = new ArrayList<>(worldInk.inkInChunk().keySet());
        int blockCount = 0;
        while (!decayableInk.isEmpty() && blockCount < MAX_DECAYABLE_PER_CHUNK) {
            BlockPos localPos = decayableInk.remove(random.nextInt(decayableInk.size()));
            BlockPos worldPos = new BlockPos(
                    chunkPos.x * 16 + localPos.getX(),
                    localPos.getY(),
                    chunkPos.z * 16 + localPos.getZ());
            WorldInk.Entry entry = worldInk.getInk(localPos);
            if (entry == null || entry.equals(worldInk.getPermanentInk(localPos))) {
                continue;
            }
            if (!SplatcraftGameRules.localizedBoolean(level, worldPos, SplatcraftGameRules.INK_DECAY)) {
                continue;
            }
            if (random.nextFloat() >= SplatcraftGameRules.localizedInt(level, worldPos, SplatcraftGameRules.INK_DECAY_RATE) * 0.001F) {
                continue;
            }

            int adjacentInk = adjacentInk(level, worldPos);
            if (adjacentInk > 0 && random.nextInt(adjacentInk * 2) != 0) {
                continue;
            }

            InkBlockUtils.clearInk(level, worldPos);
            blockCount++;
        }
    }

    private static int adjacentInk(Level level, BlockPos pos) {
        int adjacentInk = 0;
        for (Direction direction : Direction.values()) {
            if (WorldInkStorage.isInked(level, pos.relative(direction))) {
                adjacentInk++;
            }
        }
        return adjacentInk;
    }
}
