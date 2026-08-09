package net.splatcraft.neoforge.item;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.worldink.InkAreaActions;

public class InkDisruptorItem extends RemoteItem {
    public InkDisruptorItem(Properties properties) {
        super(properties);
    }

    @Override
    public RemoteResult onRemoteUse(
            Level level,
            BlockPos pointA,
            BlockPos pointB,
            ItemStack stack,
            int color,
            int mode,
            Collection<ServerPlayer> targets) {
        if (!InkAreaActions.inWorldBounds(level, pointA, pointB)) {
            return RemoteResult.fail(Component.translatable("status.clear_ink.out_of_world"));
        }
        if (InkAreaActions.volume(pointA, pointB) > InkAreaActions.MAX_COMMAND_VOLUME) {
            return RemoteResult.fail(Component.translatable("status.area_command.too_large"));
        }

        int count = InkAreaActions.clearInk(level, pointA, pointB);
        long volume = Math.max(1L, InkAreaActions.volume(pointA, pointB));
        return RemoteResult.success(
                Component.translatable("status.clear_ink." + (count > 0 ? "success" : "no_ink"), count),
                count,
                (int) (count * 15L / volume));
    }
}
