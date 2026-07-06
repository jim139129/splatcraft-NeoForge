package net.splatcraft.neoforge.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

public final class ClientStageCache {
    private static final Map<String, Stage> STAGES = new LinkedHashMap<>();

    private ClientStageCache() {
    }

    public static Optional<Stage> stage(String stageId) {
        return Optional.ofNullable(STAGES.get(stageId));
    }

    public static Map<String, Stage> stages() {
        return Collections.unmodifiableMap(STAGES);
    }

    public static void replace(CompoundTag tag) {
        STAGES.clear();
        for (String stageId : tag.getAllKeys()) {
            STAGES.put(stageId, new Stage(tag.getCompound(stageId)));
        }
    }

    public static void clear() {
        STAGES.clear();
    }
}
