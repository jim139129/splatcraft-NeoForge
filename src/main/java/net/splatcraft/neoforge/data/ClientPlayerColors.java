package net.splatcraft.neoforge.data;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.UUID;

public final class ClientPlayerColors {
    private static final Map<UUID, Integer> COLORS_BY_ID = new HashMap<>();
    private static final Map<String, Integer> COLORS_BY_NAME = new TreeMap<>();

    private ClientPlayerColors() {
    }

    public static void clear() {
        COLORS_BY_ID.clear();
        COLORS_BY_NAME.clear();
    }

    public static void put(UUID playerId, String playerName, int color) {
        if (playerId != null) {
            COLORS_BY_ID.put(playerId, color);
        }
        if (playerName != null && !playerName.isBlank()) {
            COLORS_BY_NAME.put(playerName, color);
        }
    }

    public static OptionalInt color(UUID playerId) {
        Integer color = COLORS_BY_ID.get(playerId);
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }

    public static OptionalInt color(String playerName) {
        Integer color = COLORS_BY_NAME.get(playerName);
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }
}
