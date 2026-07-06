package net.splatcraft.neoforge.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.player.SquidInkMovement;
import net.splatcraft.neoforge.registry.SplatcraftAttributes;

public final class SquidMovementInputHandler {
    private SquidMovementInputHandler() {
    }

    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        Input input = event.getInput();

        float multiplier = 1.0F;
        boolean squid = SplatcraftPlayerInfoEvents.isSquid(player);
        boolean squidSwimming = squid && SquidInkMovement.canSquidSwim(player);
        boolean squidClimbing = squid && SquidInkMovement.canSquidClimb(player);
        if (squidSwimming || squidClimbing) {
            multiplier = input.shiftKeyDown ? 1.0F : 3.0F;
        } else if (SquidInkMovement.onEnemyInk(player)) {
            multiplier = 0.5F;
        }

        input.forwardImpulse *= multiplier;
        input.leftImpulse *= multiplier;
        applyWeaponUseMobility(player, input);

        if (squidSwimming && !player.getAbilities().flying && (input.forwardImpulse != 0.0F || input.leftImpulse != 0.0F)) {
            float speed = (float) player.getAttributeValue(SplatcraftAttributes.INK_SWIM_SPEED) * (player.onGround() ? 1.0F : 0.75F);
            player.moveRelative(speed, new Vec3(input.leftImpulse, 0.0D, input.forwardImpulse).normalize());
        }

        if (squidClimbing && !player.getAbilities().flying) {
            applySquidClimbingMovement(player, input);
        }
    }

    private static void applyWeaponUseMobility(Player player, Input input) {
        if (!player.isUsingItem()) {
            return;
        }

        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return;
        }

        float mobility = weaponUseMobility(player, stack, weapon);
        input.forwardImpulse *= 5.0F * mobility;
        input.leftImpulse *= 5.0F * mobility;
    }

    private static float weaponUseMobility(Player player, ItemStack stack, WeaponItem weapon) {
        JsonObject raw = weapon.settings().raw();
        return switch (weapon.weaponClass()) {
            case ROLLER -> rollerMobility(player, stack, weapon, raw);
            case SPLATLING -> floatValue(object(raw, "charge"), "mobility_while_charging", weapon.settings().stats().mobility());
            default -> weapon.settings().stats().mobility();
        };
    }

    private static float rollerMobility(Player player, ItemStack stack, WeaponItem weapon, JsonObject raw) {
        JsonObject swing = object(raw, "swing");
        JsonObject roll = object(raw, "roll");
        int useTime = weapon.getUseDuration(stack, player) - player.getUseItemRemainingTicks();
        int startupTicks = Math.max(0, intValue(swing, "startup_time", 0));
        if (useTime < startupTicks) {
            return floatValue(swing, "mobility", weapon.settings().stats().mobility());
        }

        float rollMobility = floatValue(roll, "mobility", weapon.settings().stats().mobility());
        float dashMobility = floatValue(roll, "dash_mobility", rollMobility);
        int dashTime = Math.max(1, intValue(roll, "dash_time", 1));
        float dashScale = Math.min(1.0F, Math.max(0.0F, (useTime - startupTicks) / (float) dashTime));
        return rollMobility + (dashMobility - rollMobility) * dashScale;
    }

    private static JsonObject object(JsonObject parent, String name) {
        JsonElement element = parent.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static float floatValue(JsonObject object, String name, float fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsFloat()
                : fallback;
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : fallback;
    }

    private static void applySquidClimbingMovement(Player player, Input input) {
        double maxClimbSpeed = input.jumping ? 0.46D : 0.4D;
        if (player.getDeltaMovement().y() < maxClimbSpeed) {
            player.moveRelative(0.06F * (input.jumping ? 2.0F : 1.7F), new Vec3(0.0D, input.forwardImpulse, -Math.min(0.0F, input.forwardImpulse)).normalize());
        }
        if (player.getDeltaMovement().y() <= 0.0D && !input.shiftKeyDown) {
            player.moveRelative(0.035F, new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (input.shiftKeyDown) {
            player.setDeltaMovement(player.getDeltaMovement().x(), Math.max(0.0D, player.getDeltaMovement().y()), player.getDeltaMovement().z());
        }
    }
}
