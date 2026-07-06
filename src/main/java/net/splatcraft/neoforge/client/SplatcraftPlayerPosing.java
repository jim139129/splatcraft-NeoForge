package net.splatcraft.neoforge.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.neoforge.item.SubWeaponItem;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public final class SplatcraftPlayerPosing {
    private SplatcraftPlayerPosing() {
    }

    public static void setupPlayerAngles(Player player, PlayerModel<?> model, float partialTick) {
        if (player == null || model == null || SplatcraftPlayerInfoEvents.isSquid(player)) {
            return;
        }

        PoseContext context = activePose(player, partialTick);
        if (context == null) {
            return;
        }

        ModelPart activeArm = armFor(model, player, context.hand());
        ModelPart offArm = activeArm == model.leftArm ? model.rightArm : model.leftArm;
        ItemStack offStack = player.getItemInHand(otherHand(context.hand()));
        float progress = context.animationProgress();

        switch (context.weapon().weaponClass()) {
            case SHOOTER, BLASTER -> firePose(model, activeArm);
            case DUALIE -> {
                firePose(model, activeArm);
                if (offStack.getItem() instanceof WeaponItem offWeapon
                        && offWeapon.weaponClass() == WeaponItem.WeaponClass.DUALIE) {
                    firePose(model, offArm);
                }
            }
            case SUB -> subWeaponPose(model, activeArm, context);
            case SPLATLING -> {
                activeArm.yRot = -0.1F + model.head.yRot;
                activeArm.xRot = model.head.xRot;
            }
            case CHARGER -> chargerPose(model, activeArm, offArm);
            case SLOSHER -> slosherPose(activeArm, progress);
            case ROLLER -> rollerPose(model, activeArm, context.weapon(), progress);
        }
    }

    private static PoseContext activePose(Player player, float partialTick) {
        SplatcraftKeyHandler.ActiveSubWeaponUse subWeaponUse = SplatcraftKeyHandler.activeSubWeaponUse(player);
        if (subWeaponUse != null) {
            return new PoseContext(
                    player,
                    subWeaponUse.hand(),
                    subWeaponUse.stack(),
                    subWeaponUse.subWeapon(),
                    partialTick,
                    false,
                    subWeaponUse.useTime());
        }

        if (player.isUsingItem()) {
            InteractionHand hand = player.getUsedItemHand();
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof WeaponItem weapon) {
                return new PoseContext(player, hand, stack, weapon, partialTick, true);
            }
        }

        PoseContext mainHand = cooldownPose(player, InteractionHand.MAIN_HAND, partialTick);
        if (mainHand != null) {
            return mainHand;
        }
        return cooldownPose(player, InteractionHand.OFF_HAND, partialTick);
    }

    private static PoseContext cooldownPose(Player player, InteractionHand hand, float partialTick) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return null;
        }
        if (!player.getCooldowns().isOnCooldown(stack.getItem()) && !WeaponItem.hasActiveChargeState(stack)) {
            return null;
        }
        return new PoseContext(player, hand, stack, weapon, partialTick, false, 0);
    }

    private static void firePose(PlayerModel<?> model, ModelPart arm) {
        arm.yRot = -0.1F + model.head.yRot;
        arm.xRot = (-(float) Math.PI / 2.0F) + model.head.xRot;
    }

    private static void subWeaponPose(PlayerModel<?> model, ModelPart arm, PoseContext context) {
        if (context.stack().getItem() instanceof SubWeaponItem<?> subWeapon && context.useTime() >= subWeapon.holdTime()) {
            return;
        }
        arm.yRot = -0.1F + model.head.yRot;
        arm.xRot = (float) Math.PI / 8.0F;
        arm.zRot = ((float) Math.PI / 6.0F) * (arm == model.leftArm ? -1.0F : 1.0F);
    }

    private static void chargerPose(PlayerModel<?> model, ModelPart activeArm, ModelPart offArm) {
        if (activeArm == model.rightArm) {
            activeArm.yRot = -0.1F + model.head.yRot;
            offArm.yRot = 0.1F + model.head.yRot + 0.4F;
        } else {
            offArm.yRot = -0.1F + model.head.yRot - 0.4F;
            activeArm.yRot = 0.1F + model.head.yRot;
        }
        activeArm.xRot = (-(float) Math.PI / 2.0F) + model.head.xRot;
        offArm.xRot = (-(float) Math.PI / 2.0F) + model.head.xRot;
    }

    private static void slosherPose(ModelPart arm, float progress) {
        arm.yRot = 0.0F;
        arm.xRot = -0.36F;
        float angle = progress * (float) Math.PI + (float) Math.PI / 1.8F;
        if (angle < 6.5F) {
            arm.xRot = Mth.cos(angle * 0.6662F);
        }
    }

    private static void rollerPose(PlayerModel<?> model, ModelPart arm, WeaponItem weapon, float progress) {
        arm.xRot = 0.1F * 0.5F - (float) Math.PI / 10.0F;
        if (weapon.isRollerBrush()) {
            float angle = -progress * (float) Math.PI / 2.0F + (float) Math.PI / 1.8F;
            arm.yRot = model.head.yRot + Mth.cos(angle);
            return;
        }

        arm.yRot = model.head.yRot;
        float angle = progress * (float) Math.PI / 2.0F + (float) Math.PI / 1.8F;
        arm.xRot = Mth.cos(angle) + 0.1F * 0.5F - (float) Math.PI / 10.0F;
    }

    private static ModelPart armFor(PlayerModel<?> model, Player player, InteractionHand hand) {
        boolean left = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm() == HumanoidArm.LEFT
                : player.getMainArm() == HumanoidArm.RIGHT;
        return left ? model.leftArm : model.rightArm;
    }

    private static InteractionHand otherHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static float animationTicks(WeaponItem weapon, boolean airborne) {
        JsonObject raw = weapon.settings().raw();
        return switch (weapon.weaponClass()) {
            case ROLLER -> Math.max(1, intValue(rollerAction(raw, airborne), "startup_time", 6));
            case SLOSHER, BLASTER -> Math.max(1, intValue(object(raw, "shot"), "startup_ticks", 1));
            default -> Math.max(1, intValue(object(raw, "shot"), "firing_speed", 4));
        };
    }

    private static JsonObject rollerAction(JsonObject raw, boolean airborne) {
        JsonObject fallback = object(raw, "swing");
        return airborne && raw.has("fling") && raw.get("fling").isJsonObject()
                ? object(raw, "fling")
                : fallback;
    }

    private static JsonObject object(JsonObject parent, String name) {
        JsonElement element = parent.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : fallback;
    }

    private record PoseContext(
            Player player,
            InteractionHand hand,
            ItemStack stack,
            WeaponItem weapon,
            float partialTick,
            boolean using,
            int forcedUseTime
    ) {
        private PoseContext(Player player, InteractionHand hand, ItemStack stack, WeaponItem weapon, float partialTick, boolean using) {
            this(player, hand, stack, weapon, partialTick, using, -1);
        }

        private int useTime() {
            if (forcedUseTime >= 0) {
                return forcedUseTime;
            }
            return using
                    ? Math.max(0, weapon.getUseDuration(stack, player) - player.getUseItemRemainingTicks())
                    : 0;
        }

        private float animationProgress() {
            float cooldown = player.getCooldowns().getCooldownPercent(stack.getItem(), partialTick);
            if (cooldown > 0.0F) {
                return Mth.clamp(1.0F - cooldown, 0.0F, 1.0F);
            }
            return Mth.clamp(useTime() / animationTicks(weapon, !player.onGround()), 0.0F, 1.0F);
        }
    }
}
