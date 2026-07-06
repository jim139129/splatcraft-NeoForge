package net.splatcraft.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public final class SplatcraftSuperJumpCommand {
    private static final Map<UUID, SuperJump> ACTIVE_JUMPS = new HashMap<>();

    private SplatcraftSuperJumpCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("superjump").requires(source -> source.hasPermission(2))
                .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(SplatcraftSuperJumpCommand::jumpToBlock))
                .then(Commands.argument("target", EntityArgument.entity())
                        .executes(SplatcraftSuperJumpCommand::jumpToEntity)));
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SuperJump jump = ACTIVE_JUMPS.get(player.getUUID());
        if (jump == null) {
            return;
        }

        tickJump(player, jump);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        finishJump(event.getEntity());
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        finishJump(event.getEntity());
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        finishJump(event.getEntity());
    }

    private static int jumpToBlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos target = BlockPosArgument.getLoadedBlockPos(context, "to");
        return execute(context, new Vec3(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D));
    }

    private static int jumpToEntity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        return execute(context, target.position());
    }

    private static int execute(CommandContext<CommandSourceStack> context, Vec3 target) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SuperJump jump = new SuperJump(target, player.position(), player.noPhysics);
        ACTIVE_JUMPS.put(player.getUUID(), jump);
        player.displayClientMessage(Component.literal("pchoooooo"), false);
        return 1;
    }

    private static void tickJump(ServerPlayer player, SuperJump jump) {
        if (!player.isAlive() || player.isSpectator()) {
            finishJump(player);
            return;
        }

        double distanceLeft = horizontalDistance(player.position(), jump.target());
        if (distanceLeft > jump.distanceLeft() + 0.01D) {
            finishJump(player);
            return;
        }

        jump.setDistanceLeft(distanceLeft);
        player.stopFallFlying();
        player.getAbilities().flying = false;
        player.noPhysics = true;
        player.fallDistance = 0.0F;

        double distancePercent = jump.distance() <= 0.0D ? 0.0D : distanceLeft / jump.distance();
        setJumpSquidState(player, distancePercent > 0.2D);

        if (distanceLeft < 0.25D) {
            finishJump(player);
            return;
        }

        Vec3 toTarget = jump.target().subtract(player.position());
        Vec3 horizontal = toTarget.multiply(1.0D, 0.0D, 1.0D).scale(0.1D);
        double y = distancePercent > 0.9D
                ? Math.max(2.0D, toTarget.y) * 1.25D
                : player.getDeltaMovement().y;
        player.setDeltaMovement(
                Mth.clamp(horizontal.x, -3.0D, 3.0D),
                y,
                Mth.clamp(horizontal.z, -3.0D, 3.0D));
        player.hurtMarked = true;
    }

    private static void finishJump(Player player) {
        SuperJump jump = ACTIVE_JUMPS.remove(player.getUUID());
        if (jump == null) {
            return;
        }

        player.noPhysics = jump.originalNoPhysics();
        if (SplatcraftPlayerInfoEvents.isSquid(player)) {
            SplatcraftPlayerInfoEvents.setSquid(player, false);
        }
    }

    private static void setJumpSquidState(ServerPlayer player, boolean squid) {
        if (SplatcraftPlayerInfoEvents.isSquid(player) != squid) {
            SplatcraftPlayerInfoEvents.setSquid(player, squid);
        }
    }

    private static double horizontalDistance(Vec3 left, Vec3 right) {
        double x = left.x - right.x;
        double z = left.z - right.z;
        return Math.sqrt(x * x + z * z);
    }

    private static final class SuperJump {
        private final Vec3 target;
        private final double distance;
        private final boolean originalNoPhysics;
        private double distanceLeft;

        private SuperJump(Vec3 target, Vec3 from, boolean originalNoPhysics) {
            this.target = target;
            this.distance = horizontalDistance(target, from);
            this.originalNoPhysics = originalNoPhysics;
            this.distanceLeft = distance;
        }

        private Vec3 target() {
            return target;
        }

        private double distance() {
            return distance;
        }

        private boolean originalNoPhysics() {
            return originalNoPhysics;
        }

        private double distanceLeft() {
            return distanceLeft;
        }

        private void setDistanceLeft(double distanceLeft) {
            this.distanceLeft = distanceLeft;
        }
    }
}
