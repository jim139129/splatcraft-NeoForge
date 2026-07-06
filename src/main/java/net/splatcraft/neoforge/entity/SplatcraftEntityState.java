package net.splatcraft.neoforge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public final class SplatcraftEntityState {
    private SplatcraftEntityState() {
    }

    public static boolean isSquid(Entity entity) {
        return entity instanceof InkSquidEntity
                || entity instanceof Player player && SplatcraftPlayerInfoEvents.isSquid(player);
    }

    public static int color(Entity entity) {
        if (entity instanceof Player player) {
            return SplatcraftPlayerInfoEvents.color(player);
        }
        return entity instanceof ColoredEntity colored ? colored.splatcraftColor() : -1;
    }
}
