package net.splatcraft.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.FastColor;
import net.splatcraft.neoforge.SplatcraftConfig;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

final class ClientInkColors {
    private static final int COLOR_LOCK_FRIENDLY = InkColorData.resolve("color_lock_friendly").orElse(0xDEA801);
    private static final int COLOR_LOCK_HOSTILE = InkColorData.resolve("color_lock_hostile").orElse(0x4717A9);

    private ClientInkColors() {
    }

    static int visibleColor(int color) {
        if (color < 0) {
            return color;
        }

        int rgb = color & 0xFFFFFF;
        if (!SplatcraftConfig.COLOR_LOCK.get()) {
            return rgb;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return rgb;
        }

        int playerColor = SplatcraftPlayerInfoEvents.color(player) & 0xFFFFFF;
        return playerColor == rgb ? COLOR_LOCK_FRIENDLY : COLOR_LOCK_HOSTILE;
    }

    static int visibleColor(float red, float green, float blue) {
        int color = Math.round(red * 255.0F) << 16
                | Math.round(green * 255.0F) << 8
                | Math.round(blue * 255.0F);
        return visibleColor(color);
    }

    static int visibleArgb(int color) {
        if (color < 0) {
            return -1;
        }
        return FastColor.ARGB32.opaque(visibleColor(color));
    }

    static int visibleTint(int color) {
        return color == -1 ? -1 : visibleArgb(color);
    }

    static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }
}
