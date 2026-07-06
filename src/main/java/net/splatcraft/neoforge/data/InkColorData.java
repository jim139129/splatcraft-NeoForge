package net.splatcraft.neoforge.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.neoforge.Splatcraft;

public final class InkColorData {
    public static final int DEFAULT_COLOR = 0x1F1F2D;

    private static final Map<ResourceLocation, Integer> BUILT_IN_COLORS = new LinkedHashMap<>();

    static {
        register("orange", 0xDF641A);
        register("blue", 0x26229F);
        register("pink", 0xC83D79);
        register("green", 0x409D3B);
        register("light_blue", 0x228CFF);
        register("turquoise", 0x048188);
        register("yellow", 0xE1A307);
        register("lilac", 0x4D24A3);
        register("lemon", 0x91B00B);
        register("plum", 0x830B9C);
        register("cyan", 0x4ACBCB);
        register("peach", 0xEA8546);
        register("mint", 0x08B672);
        register("cherry", 0xE24F65);
        register("neon_pink", 0xCF0466);
        register("neon_green", 0x17A80D);
        register("neon_orange", 0xE85407);
        register("neon_blue", 0x2E0CB5);
        register("hero_yellow", 0xD3F526);
        register("octo_pink", 0xE51B5E);
        register("mojang", 0xDF242F);
        register("cobalt", 0x005682);
        register("ice", 0x88FFC1);
        register("floral", 0xFF9BEE);
        register("omni_green", 0x93E720);
        register("mana", 0xF33EF1);
        register("color_lock_friendly", 0xDEA801);
        register("color_lock_hostile", 0x4717A9);
        register("dye_white", 0xFAFAFA);
        register("dye_orange", 16351261);
        register("dye_magenta", 13061821);
        register("dye_light_blue", 3847130);
        register("dye_yellow", 16701501);
        register("dye_lime", 8439583);
        register("dye_pink", 15961002);
        register("dye_gray", 4673362);
        register("dye_light_gray", 10329495);
        register("dye_cyan", 1481884);
        register("dye_purple", 8991416);
        register("dye_blue", 3949738);
        register("dye_brown", 8606770);
        register("dye_green", 6192150);
        register("dye_red", 11546150);
        register("dye_black", 1908001);
        register("royal_blue", 0x525CF5);
        register("moth_green", 0x425113);
        register("light_green", 0x85E378);
        register("purple", 0x6C0676);
        register("mustard", 0xCE8003);
        register("lumigreen", 0x60AB43);
        register("dark_blue", 0x0D195E);
        register("soda", 0x65B799);
        register("deep_blue", 0x0D37C3);
        register("fuchsia", 0xE532D6);
        register("winter_green", 0x4DE29D);
        register("pumpkin", 0xDD6900);
        register("redwood", 0x5B342E);
        register("default", DEFAULT_COLOR);
    }

    private InkColorData() {
    }

    public static OptionalInt resolve(String value) {
        String trimmed = value.trim();
        OptionalInt hex = parseHex(trimmed);
        if (hex.isPresent()) {
            return hex;
        }

        ResourceLocation id = ResourceLocation.tryParse(trimmed);
        if (id == null) {
            return OptionalInt.empty();
        }

        Integer color = BUILT_IN_COLORS.get(id);
        if (color != null) {
            return OptionalInt.of(color);
        }

        if (!trimmed.contains(":")) {
            color = BUILT_IN_COLORS.get(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, trimmed));
        }
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }

    public static Map<ResourceLocation, Integer> builtInColors() {
        return Collections.unmodifiableMap(BUILT_IN_COLORS);
    }

    public static int builtInOrder(int color) {
        int index = 0;
        for (int builtInColor : BUILT_IN_COLORS.values()) {
            if (builtInColor == color) {
                return index;
            }
            index++;
        }
        return Integer.MAX_VALUE;
    }

    public static Optional<ResourceLocation> builtInId(int color) {
        for (Map.Entry<ResourceLocation, Integer> entry : BUILT_IN_COLORS.entrySet()) {
            if (entry.getValue() == color) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private static void register(String name, int color) {
        BUILT_IN_COLORS.put(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name), color);
    }

    private static OptionalInt parseHex(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6) {
            return OptionalInt.empty();
        }

        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return OptionalInt.empty();
            }
        }

        return OptionalInt.of(Integer.parseInt(hex, 16));
    }
}
