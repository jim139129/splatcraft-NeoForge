package net.splatcraft.neoforge.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class SplatcraftSaveData extends SavedData {
    private static final String FILE_ID = "splatcraft";
    private static final SavedData.Factory<SplatcraftSaveData> FACTORY =
            new SavedData.Factory<>(SplatcraftSaveData::new, SplatcraftSaveData::load);

    private final Set<Integer> initializedColorScores = new LinkedHashSet<>();
    private final Map<String, Stage> stages = new LinkedHashMap<>();

    public static SplatcraftSaveData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static Optional<SplatcraftSaveData> get(Level level) {
        return level.getServer() == null ? Optional.empty() : Optional.of(get(level.getServer()));
    }

    private static SplatcraftSaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        SplatcraftSaveData data = new SplatcraftSaveData();
        for (int color : tag.getIntArray("StoredCriteria")) {
            data.initializedColorScores.add(color);
        }

        CompoundTag stagesTag = tag.getCompound("Stages");
        for (String key : stagesTag.getAllKeys()) {
            data.stages.put(key, new Stage(stagesTag.getCompound(key)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        int[] colors = initializedColorScores.stream().mapToInt(Integer::intValue).toArray();
        tag.putIntArray("StoredCriteria", colors);

        CompoundTag stagesTag = new CompoundTag();
        stages.forEach((id, stage) -> stagesTag.put(id, stage.save()));
        tag.put("Stages", stagesTag);
        return tag;
    }

    public Collection<Integer> initializedColorScores() {
        return initializedColorScores;
    }

    public void addInitializedColorScore(int color) {
        if (initializedColorScores.add(color)) {
            setDirty();
        }
    }

    public void removeInitializedColorScore(int color) {
        if (initializedColorScores.remove(color)) {
            setDirty();
        }
    }

    public Map<String, Stage> stages() {
        return stages;
    }

    public Optional<Stage> localStage(Level level, BlockPos pos) {
        return stages.values().stream()
                .filter(stage -> stage.contains(level, pos))
                .min((left, right) -> Long.compare(left.volume(), right.volume()));
    }
}
