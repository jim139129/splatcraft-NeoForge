package net.splatcraft.neoforge.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.ClientStageCache;
import net.splatcraft.neoforge.data.SplatcraftSaveData;
import net.splatcraft.neoforge.data.Stage;
import net.splatcraft.neoforge.network.payload.GameRulesPayload;

public final class SplatcraftGameRules {
    private static final Map<String, GameRules.Key<GameRules.BooleanValue>> BOOLEAN_RULES = new LinkedHashMap<>();
    private static final Map<String, GameRules.Key<GameRules.IntegerValue>> INTEGER_RULES = new LinkedHashMap<>();
    private static final Map<String, Boolean> DEFAULT_BOOLEAN_VALUES = new LinkedHashMap<>();
    private static final Map<String, Integer> DEFAULT_INTEGER_VALUES = new LinkedHashMap<>();
    private static final Map<String, Boolean> CLIENT_BOOLEAN_VALUES = new LinkedHashMap<>();
    private static final Map<String, Integer> CLIENT_INTEGER_VALUES = new LinkedHashMap<>();

    public static final GameRules.Key<GameRules.BooleanValue> INK_DECAY =
            booleanRule("inkDecay", GameRules.Category.UPDATES, true);
    public static final GameRules.Key<GameRules.IntegerValue> INK_DECAY_RATE =
            intRule("inkDecayRate", GameRules.Category.UPDATES, 3);
    public static final GameRules.Key<GameRules.BooleanValue> COLORED_PLAYER_NAMES =
            booleanRule("coloredPlayerNames", GameRules.Category.PLAYER, false);
    public static final GameRules.Key<GameRules.BooleanValue> KEEP_MATCH_ITEMS =
            booleanRule("keepMatchItems", GameRules.Category.PLAYER, false);
    public static final GameRules.Key<GameRules.BooleanValue> UNIVERSAL_INK =
            booleanRule("universalInk", GameRules.Category.PLAYER, false);
    public static final GameRules.Key<GameRules.BooleanValue> DROP_CRATE_LOOT =
            booleanRule("dropCrateLoot", GameRules.Category.DROPS, false);
    public static final GameRules.Key<GameRules.BooleanValue> WATER_DAMAGE =
            booleanRule("waterDamage", GameRules.Category.PLAYER, false);
    public static final GameRules.Key<GameRules.BooleanValue> REQUIRE_INK_TANK =
            booleanRule("requireInkTank", GameRules.Category.PLAYER, true);
    public static final GameRules.Key<GameRules.BooleanValue> INK_FRIENDLY_FIRE =
            booleanRule("inkFriendlyFire", GameRules.Category.PLAYER, false);
    public static final GameRules.Key<GameRules.BooleanValue> INK_HEALING =
            booleanRule("inkHealing", GameRules.Category.PLAYER, true);
    public static final GameRules.Key<GameRules.BooleanValue> INK_HEALING_CONSUMES_HUNGER =
            booleanRule("inkHealingConsumesHunger", GameRules.Category.PLAYER, true);
    public static final GameRules.Key<GameRules.BooleanValue> INK_DAMAGE_COOLDOWN =
            booleanRule("inkDamageCooldown", GameRules.Category.PLAYER, false);
    public static final GameRules.Key<GameRules.IntegerValue> INK_MOB_DAMAGE_PERCENTAGE =
            intRule("inkMobDamagePercentage", GameRules.Category.MOBS, 70);
    public static final GameRules.Key<GameRules.BooleanValue> INFINITE_INK_IN_CREATIVE =
            booleanRule("infiniteInkInCreative", GameRules.Category.PLAYER, true);
    public static final GameRules.Key<GameRules.BooleanValue> INKABLE_GROUND =
            booleanRule("inkableGround", GameRules.Category.MISC, true);
    public static final GameRules.Key<GameRules.BooleanValue> INK_DESTROYS_FOLIAGE =
            booleanRule("inkDestroysFoliage", GameRules.Category.MISC, true);
    public static final GameRules.Key<GameRules.BooleanValue> RECHARGEABLE_INK_TANK =
            booleanRule("rechargeableInkTank", GameRules.Category.PLAYER, true);

    private SplatcraftGameRules() {
    }

    public static void init() {
    }

    public static boolean booleanValue(Level level, GameRules.Key<GameRules.BooleanValue> rule) {
        if (level.isClientSide) {
            return clientBooleanValue(rule);
        }
        return level.getGameRules().getBoolean(rule);
    }

    public static boolean localizedBoolean(Level level, BlockPos pos, GameRules.Key<GameRules.BooleanValue> rule) {
        return localStage(level, pos)
                .filter(stage -> stage.hasSetting(rule))
                .map(stage -> stage.getSetting(rule))
                .orElseGet(() -> booleanValue(level, rule));
    }

    public static int intValue(Level level, GameRules.Key<GameRules.IntegerValue> rule) {
        if (level.isClientSide) {
            return clientIntegerValue(rule);
        }
        return level.getGameRules().getInt(rule);
    }

    public static int localizedInt(Level level, BlockPos pos, GameRules.Key<GameRules.IntegerValue> rule) {
        return localStage(level, pos)
                .filter(stage -> stage.hasIntegerSetting(rule))
                .map(stage -> stage.getIntegerSetting(rule))
                .orElseGet(() -> intValue(level, rule));
    }

    public static Map<String, Boolean> booleanValues(MinecraftServer server) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        BOOLEAN_RULES.forEach((id, rule) -> values.put(id, server.getGameRules().getBoolean(rule)));
        return values;
    }

    public static Map<String, Integer> integerValues(MinecraftServer server) {
        Map<String, Integer> values = new LinkedHashMap<>();
        INTEGER_RULES.forEach((id, rule) -> values.put(id, server.getGameRules().getInt(rule)));
        return values;
    }

    public static void replaceClientValues(Map<String, Boolean> booleanValues, Map<String, Integer> integerValues) {
        resetClientValues();
        booleanValues.forEach((id, value) -> {
            if (CLIENT_BOOLEAN_VALUES.containsKey(id)) {
                CLIENT_BOOLEAN_VALUES.put(id, value);
            }
        });
        integerValues.forEach((id, value) -> {
            if (CLIENT_INTEGER_VALUES.containsKey(id)) {
                CLIENT_INTEGER_VALUES.put(id, value);
            }
        });
    }

    public static void resetClientValues() {
        CLIENT_BOOLEAN_VALUES.clear();
        CLIENT_BOOLEAN_VALUES.putAll(DEFAULT_BOOLEAN_VALUES);
        CLIENT_INTEGER_VALUES.clear();
        CLIENT_INTEGER_VALUES.putAll(DEFAULT_INTEGER_VALUES);
    }

    private static Optional<Stage> localStage(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return ClientStageCache.stages().values().stream()
                    .filter(stage -> stage.contains(level, pos))
                    .min((left, right) -> Long.compare(left.volume(), right.volume()));
        }
        return SplatcraftSaveData.get(level).flatMap(data -> data.localStage(level, pos));
    }

    private static boolean clientBooleanValue(GameRules.Key<GameRules.BooleanValue> rule) {
        return CLIENT_BOOLEAN_VALUES.getOrDefault(rule.getId(), false);
    }

    private static int clientIntegerValue(GameRules.Key<GameRules.IntegerValue> rule) {
        return CLIENT_INTEGER_VALUES.getOrDefault(rule.getId(), 0);
    }

    private static GameRules.Key<GameRules.BooleanValue> booleanRule(String name, GameRules.Category category, boolean defaultValue) {
        GameRules.Key<GameRules.BooleanValue> rule = GameRules.register(
                Splatcraft.MOD_ID + "." + name,
                category,
                GameRules.BooleanValue.create(defaultValue, (server, value) -> GameRulesPayload.sendToAll(server)));
        BOOLEAN_RULES.put(rule.getId(), rule);
        DEFAULT_BOOLEAN_VALUES.put(rule.getId(), defaultValue);
        CLIENT_BOOLEAN_VALUES.put(rule.getId(), defaultValue);
        return rule;
    }

    private static GameRules.Key<GameRules.IntegerValue> intRule(String name, GameRules.Category category, int defaultValue) {
        GameRules.Key<GameRules.IntegerValue> rule = GameRules.register(
                Splatcraft.MOD_ID + "." + name,
                category,
                GameRules.IntegerValue.create(defaultValue, (server, value) -> GameRulesPayload.sendToAll(server)));
        INTEGER_RULES.put(rule.getId(), rule);
        DEFAULT_INTEGER_VALUES.put(rule.getId(), defaultValue);
        CLIENT_INTEGER_VALUES.put(rule.getId(), defaultValue);
        return rule;
    }
}
