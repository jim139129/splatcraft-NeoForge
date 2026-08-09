package net.splatcraft.neoforge.worldink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.splatcraft.neoforge.mixin.ChunkMapAccessor;
import net.splatcraft.neoforge.particle.SplatcraftParticleEffects;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;

public final class WorldInkHandler {
    private static final int MAX_DECAYABLE_PER_CHUNK = 3;
    private static final int MAX_DECAYABLE_CHUNKS = 10;

    private WorldInkHandler() {
    }

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
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

        List<ChunkPos> candidates = activeInkChunks(level);
        if (candidates.isEmpty()) {
            return;
        }

        RandomSource random = level.random;
        int maxChunkChecks = Math.min(random.nextInt(MAX_DECAYABLE_CHUNKS), candidates.size());
        for (int index = 0; index < maxChunkChecks; index++) {
            ChunkPos chunkPos = candidates.get(random.nextInt(candidates.size()));
            WorldInkStorage.getLoaded(level, chunkPos).ifPresent(worldInk -> decayChunk(level, chunkPos, worldInk, random));
        }
    }

    private static void checkForInkRemoval(ServerLevel level, BlockPos pos) {
        WorldInkStorage.getInk(level, pos)
                .filter(ignored -> InkBlockUtils.isUninkable(level, pos))
                .ifPresent(entry -> {
                    SplatcraftParticleEffects.inkDestroy(level, pos, entry.color());
                    InkBlockUtils.clearInk(level, pos, true);
                });
    }

    /**
     * All ticking chunks that contain ink, like the 1.18 implementation. Sampling only a small
     * radius around each player left visible ink outside that radius (or in force-loaded chunks)
     * decaying never.
     */
    private static List<ChunkPos> activeInkChunks(ServerLevel level) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (ChunkHolder holder : ((ChunkMapAccessor) level.getChunkSource().chunkMap).splatcraft$getChunks()) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk == null) {
                continue;
            }
            Optional<WorldInk> worldInk = chunk.getExistingData(SplatcraftAttachments.WORLD_INK.get());
            if (worldInk.isPresent() && !worldInk.get().inkInChunk().isEmpty()) {
                chunks.add(chunk.getPos());
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
