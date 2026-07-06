package net.splatcraft.neoforge.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class Stage {
    public static final List<String> VALID_BOOLEAN_SETTINGS = List.of(
            "inkDecay",
            "universalInk",
            "requireInkTank",
            "keepMatchItems",
            "waterDamage",
            "inkFriendlyFire",
            "inkHealing",
            "inkHealingConsumesHunger",
            "inkableGround",
            "inkDestroysFoliage",
            "rechargeableInkTank");
    public static final List<String> VALID_INTEGER_SETTINGS = List.of(
            "inkDecayRate",
            "inkMobDamagePercentage");
    public static final List<String> VALID_SETTINGS = List.of(
            "inkDecay",
            "inkDecayRate",
            "universalInk",
            "requireInkTank",
            "keepMatchItems",
            "waterDamage",
            "inkFriendlyFire",
            "inkHealing",
            "inkHealingConsumesHunger",
            "inkableGround",
            "inkDestroysFoliage",
            "inkMobDamagePercentage",
            "rechargeableInkTank");

    private final Map<String, Boolean> settings = new HashMap<>();
    private final Map<String, Integer> integerSettings = new HashMap<>();
    private final Map<String, Integer> teams = new HashMap<>();
    private BlockPos cornerA;
    private BlockPos cornerB;
    private ResourceLocation dimension;

    public Stage(Level level, BlockPos cornerA, BlockPos cornerB) {
        this.dimension = level.dimension().location();
        this.cornerA = cornerA;
        this.cornerB = cornerB;
    }

    public Stage(CompoundTag tag) {
        load(tag);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("CornerA", NbtUtils.writeBlockPos(cornerA));
        tag.put("CornerB", NbtUtils.writeBlockPos(cornerB));
        tag.putString("Dimension", dimension.toString());

        CompoundTag settingsTag = new CompoundTag();
        settings.forEach(settingsTag::putBoolean);
        tag.put("Settings", settingsTag);

        CompoundTag integerSettingsTag = new CompoundTag();
        integerSettings.forEach(integerSettingsTag::putInt);
        tag.put("IntegerSettings", integerSettingsTag);

        CompoundTag teamsTag = new CompoundTag();
        teams.forEach(teamsTag::putInt);
        tag.put("Teams", teamsTag);
        return tag;
    }

    public boolean contains(Level level, BlockPos pos) {
        if (!level.dimension().location().equals(dimension)) {
            return false;
        }

        int minX = Math.min(cornerA.getX(), cornerB.getX());
        int minY = Math.min(cornerA.getY(), cornerB.getY());
        int minZ = Math.min(cornerA.getZ(), cornerB.getZ());
        int maxX = Math.max(cornerA.getX(), cornerB.getX());
        int maxY = Math.max(cornerA.getY(), cornerB.getY());
        int maxZ = Math.max(cornerA.getZ(), cornerB.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public long volume() {
        long x = Math.abs((long) cornerA.getX() - cornerB.getX()) + 1L;
        long y = Math.abs((long) cornerA.getY() - cornerB.getY()) + 1L;
        long z = Math.abs((long) cornerA.getZ() - cornerB.getZ()) + 1L;
        return x * y * z;
    }

    public boolean hasSetting(String key) {
        return settings.containsKey(key);
    }

    public boolean hasSetting(GameRules.Key<GameRules.BooleanValue> rule) {
        return hasSetting(ruleKey(rule));
    }

    public boolean getSetting(String key) {
        return settings.getOrDefault(key, false);
    }

    public boolean getSetting(GameRules.Key<GameRules.BooleanValue> rule) {
        return getSetting(ruleKey(rule));
    }

    public void applySetting(String key, Boolean value) {
        if (value == null) {
            settings.remove(key);
        } else {
            settings.put(key, value);
        }
    }

    public boolean hasIntegerSetting(String key) {
        return integerSettings.containsKey(key);
    }

    public boolean hasIntegerSetting(GameRules.Key<GameRules.IntegerValue> rule) {
        return hasIntegerSetting(ruleKey(rule));
    }

    public int getIntegerSetting(String key) {
        return integerSettings.getOrDefault(key, 0);
    }

    public int getIntegerSetting(GameRules.Key<GameRules.IntegerValue> rule) {
        return getIntegerSetting(ruleKey(rule));
    }

    public void applySetting(String key, Integer value) {
        if (value == null) {
            integerSettings.remove(key);
        } else {
            integerSettings.put(key, value);
        }
    }

    public Map<String, Integer> teams() {
        return teams;
    }

    public boolean hasTeam(String teamId) {
        return teams.containsKey(teamId);
    }

    public int getTeamColor(String teamId) {
        return teams.getOrDefault(teamId, -1);
    }

    public void setTeamColor(String teamId, int teamColor) {
        teams.put(teamId, teamColor);
    }

    public void removeTeam(String teamId) {
        teams.remove(teamId);
    }

    public BlockPos cornerA() {
        return cornerA;
    }

    public void setCornerA(BlockPos cornerA) {
        this.cornerA = cornerA;
    }

    public BlockPos cornerB() {
        return cornerB;
    }

    public void setCornerB(BlockPos cornerB) {
        this.cornerB = cornerB;
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    private void load(CompoundTag tag) {
        cornerA = NbtUtils.readBlockPos(tag, "CornerA").orElse(BlockPos.ZERO);
        cornerB = NbtUtils.readBlockPos(tag, "CornerB").orElse(BlockPos.ZERO);
        ResourceLocation parsedDimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        dimension = parsedDimension == null ? Level.OVERWORLD.location() : parsedDimension;

        settings.clear();
        CompoundTag settingsTag = tag.getCompound("Settings");
        for (String key : settingsTag.getAllKeys()) {
            settings.put(key, settingsTag.getBoolean(key));
        }

        integerSettings.clear();
        CompoundTag integerSettingsTag = tag.getCompound("IntegerSettings");
        for (String key : integerSettingsTag.getAllKeys()) {
            integerSettings.put(key, integerSettingsTag.getInt(key));
        }

        teams.clear();
        CompoundTag teamsTag = tag.getCompound("Teams");
        for (String key : teamsTag.getAllKeys()) {
            teams.put(key, teamsTag.getInt(key));
        }
    }

    private static String ruleKey(GameRules.Key<?> rule) {
        return rule.toString().replace("splatcraft.", "");
    }
}
