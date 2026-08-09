package net.splatcraft.neoforge.scoreboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftSaveData;

public final class SplatcraftScoreboardHandler {
    public static final ObjectiveCriteria COLOR = registerCriterion(Splatcraft.MOD_ID + ".inkColor");
    public static final ObjectiveCriteria TURF_WAR_SCORE = registerCriterion(Splatcraft.MOD_ID + ".turfWarScore");

    private static final Map<Integer, ColorCriteria> COLOR_CRITERIA = new LinkedHashMap<>();

    private SplatcraftScoreboardHandler() {
    }

    /**
     * Vanilla deserializes the scoreboard during loadLevel and silently downgrades objectives
     * whose criteria are unknown to dummy. Criteria must therefore already exist in the cache
     * before level load; the overworld's SavedData is not available yet at this point, so the
     * raw data file is read directly. {@link #load} then rebuilds authoritatively from SavedData
     * once the server has started, reusing the same cached instances via ObjectiveCriteria.byName.
     */
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path dataFile = event.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("splatcraft.dat");
        if (!Files.isRegularFile(dataFile)) {
            return;
        }

        try {
            CompoundTag root = NbtIo.readCompressed(dataFile, NbtAccounter.unlimitedHeap());
            for (int color : root.getCompound("data").getIntArray("StoredCriteria")) {
                createColorCriterion(color);
            }
        } catch (IOException exception) {
            Splatcraft.LOGGER.warn("Failed to preload color criteria from {}", dataFile, exception);
        }
    }

    public static void onServerStarted(ServerStartedEvent event) {
        load(event.getServer());
    }

    /**
     * Drops this JVM's tracking map when the server stops so a subsequent single-player world does
     * not inherit the previous world's tracked colors before {@link #load} runs. The criterion
     * instances themselves stay in vanilla's cache on purpose (see {@link #removeColorCriterion}).
     */
    public static void onServerStopped(ServerStoppedEvent event) {
        COLOR_CRITERIA.clear();
    }

    public static void load(MinecraftServer server) {
        COLOR_CRITERIA.clear();
        SplatcraftSaveData.get(server).initializedColorScores().forEach(SplatcraftScoreboardHandler::createColorCriterion);
    }

    public static boolean hasColorCriterion(int color) {
        return COLOR_CRITERIA.containsKey(color);
    }

    public static void createColorCriterion(int color) {
        COLOR_CRITERIA.computeIfAbsent(color, ColorCriteria::new);
    }

    /**
     * Stops tracking a color's scores. The five {@link ObjectiveCriteria} instances are intentionally
     * left in vanilla's static {@code CRITERIA_CACHE}: evicting them would make a later re-add of the
     * same color mint fresh instances, orphaning any scoreboard objective that still references the
     * old instance (vanilla keys {@code objectivesByCriteria} by instance identity). Keeping them
     * cached costs a handful of tiny objects per color ever used and keeps re-add lossless.
     */
    public static void removeColorCriterion(int color) {
        COLOR_CRITERIA.remove(color);
    }

    public static Set<Integer> getCriteriaKeySet() {
        return COLOR_CRITERIA.keySet();
    }

    public static Collection<Integer> colors() {
        return COLOR_CRITERIA.keySet();
    }

    public static ObjectiveCriteria getColorKills(int color) {
        return COLOR_CRITERIA.get(color).colorKills();
    }

    public static ObjectiveCriteria getDeathsAsColor(int color) {
        return COLOR_CRITERIA.get(color).deathsAsColor();
    }

    public static ObjectiveCriteria getKillsAsColor(int color) {
        return COLOR_CRITERIA.get(color).killsAsColor();
    }

    public static ObjectiveCriteria getColorWins(int color) {
        return COLOR_CRITERIA.get(color).winsAsColor();
    }

    public static ObjectiveCriteria getColorLosses(int color) {
        return COLOR_CRITERIA.get(color).lossesAsColor();
    }

    public static String getColorIdentifier(int color) {
        return InkColorData.builtInId(color)
                .filter(id -> id.getNamespace().equals(Splatcraft.MOD_ID))
                .map(id -> id.getPath())
                .orElseGet(() -> String.format("%06X", color).toLowerCase());
    }

    public static void updatePlayerScore(ObjectiveCriteria criteria, Player player, int value) {
        player.getScoreboard().forAllObjectives(
                criteria,
                ScoreHolder.fromGameProfile(player.getGameProfile()),
                score -> score.set(value));
    }

    private record ColorCriteria(
            ObjectiveCriteria colorKills,
            ObjectiveCriteria deathsAsColor,
            ObjectiveCriteria killsAsColor,
            ObjectiveCriteria winsAsColor,
            ObjectiveCriteria lossesAsColor) {
        private ColorCriteria(int color) {
            this(
                    criterion("colorKills", color),
                    criterion("deathsAsColor", color),
                    criterion("killsAsColor", color),
                    criterion("winsAsColor", color),
                    criterion("lossesAsColor", color));
        }

        private static ObjectiveCriteria criterion(String name, int color) {
            String id = Splatcraft.MOD_ID + "." + name + "." + getColorIdentifier(color);
            return registerCriterion(id);
        }
    }

    private static ObjectiveCriteria registerCriterion(String id) {
        return ObjectiveCriteria.byName(id).orElseGet(() ->
                new SplatcraftCriteria(id));
    }

    private static final class SplatcraftCriteria extends ObjectiveCriteria {
        private SplatcraftCriteria(String name) {
            super(name, false, ObjectiveCriteria.RenderType.INTEGER);
        }
    }
}
