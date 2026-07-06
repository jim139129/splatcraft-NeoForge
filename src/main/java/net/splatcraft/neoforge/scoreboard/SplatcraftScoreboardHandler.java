package net.splatcraft.neoforge.scoreboard;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.mixin.ObjectiveCriteriaAccessor;

public final class SplatcraftScoreboardHandler {
    public static final ObjectiveCriteria COLOR = registerCriterion(Splatcraft.MOD_ID + ".inkColor");
    public static final ObjectiveCriteria TURF_WAR_SCORE = registerCriterion(Splatcraft.MOD_ID + ".turfWarScore");

    private static final Map<Integer, ColorCriteria> COLOR_CRITERIA = new LinkedHashMap<>();

    private SplatcraftScoreboardHandler() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        load(event.getServer());
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

    public static void removeColorCriterion(int color) {
        ColorCriteria criteria = COLOR_CRITERIA.remove(color);
        if (criteria != null) {
            removeCriterion(criteria.colorKills());
            removeCriterion(criteria.deathsAsColor());
            removeCriterion(criteria.killsAsColor());
            removeCriterion(criteria.winsAsColor());
            removeCriterion(criteria.lossesAsColor());
        }
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

    private static void removeCriterion(ObjectiveCriteria criteria) {
        ObjectiveCriteriaAccessor.splatcraft$getCriteriaCache().remove(criteria.getName(), criteria);
    }

    private static final class SplatcraftCriteria extends ObjectiveCriteria {
        private SplatcraftCriteria(String name) {
            super(name, false, ObjectiveCriteria.RenderType.INTEGER);
        }
    }
}
