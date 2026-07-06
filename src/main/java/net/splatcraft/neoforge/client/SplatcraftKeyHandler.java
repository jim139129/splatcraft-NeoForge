package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.splatcraft.neoforge.SplatcraftConfig;
import net.splatcraft.neoforge.item.SubWeaponItem;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.network.payload.DodgeRollPayload;
import net.splatcraft.neoforge.network.payload.PlayerSetSquidPayload;
import net.splatcraft.neoforge.network.payload.UseSubWeaponPayload;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.player.SquidFormEvents;

public final class SplatcraftKeyHandler {
    private static final int SUB_WEAPON_SQUID_DELAY = 10;
    private static final int WEAPON_USE_SQUID_DELAY = 10;

    private static final KeyMapping SQUID_FORM = new KeyMapping(
            "key.squidForm",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z,
            "key.categories.splatcraft"
    );
    private static final KeyMapping SUB_WEAPON = new KeyMapping(
            "key.subWeaponHotkey",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.splatcraft"
    );

    private static boolean squidToggleActive;
    private static boolean wasSquidKeyDown;
    private static boolean wasSubWeaponKeyDown;
    private static int activeSubWeaponSlot = -1;
    private static long activeSubWeaponStartTick = -1L;
    private static boolean wasJumping;
    private static int squidSuppressTicks;

    private SplatcraftKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SQUID_FORM);
        event.register(SUB_WEAPON);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.isSpectator()) {
            resetState();
            return;
        }

        boolean canUseKeys = minecraft.screen == null && minecraft.getOverlay() == null;
        handleSubWeaponHotkey(player, canUseKeys);
        handleDualieDodge(player, canUseKeys);
        boolean keyDown = SQUID_FORM.isDown() && canUseKeys;
        if (isSplatcraftUseKeyDown(minecraft, player, canUseKeys) && !keyDown) {
            squidSuppressTicks = Math.max(squidSuppressTicks, WEAPON_USE_SQUID_DELAY);
        }
        boolean targetSquid = !canRemainSquid(player) || squidSuppressTicks > 0
                ? false
                : switch (SplatcraftConfig.SQUID_KEY_MODE.get()) {
                    case HOLD -> keyDown;
                    case TOGGLE -> toggleTarget(keyDown);
                };
        if (targetSquid && !SplatcraftPlayerInfoEvents.isSquid(player) && !SquidFormEvents.canEnterSquid(player)) {
            targetSquid = false;
        }

        if (targetSquid != SplatcraftPlayerInfoEvents.isSquid(player)) {
            PlayerSetSquidPayload.sendToServer(targetSquid);
            SplatcraftPlayerInfoEvents.playerInfo(player).setSquid(targetSquid);
        }

        if (squidSuppressTicks > 0) {
            squidSuppressTicks--;
        }
        wasSquidKeyDown = keyDown;
    }

    public static boolean isSquidKeyDown() {
        return SplatcraftConfig.SQUID_KEY_MODE.get() == SplatcraftConfig.SquidKeyMode.TOGGLE
                ? squidToggleActive
                : SQUID_FORM.isDown();
    }

    public static boolean isSubWeaponHotkeyDown() {
        return SUB_WEAPON.isDown();
    }

    public static ActiveSubWeaponUse activeSubWeaponUse(Player player) {
        if (activeSubWeaponSlot < 0 || activeSubWeaponStartTick < 0L || !(player instanceof LocalPlayer)) {
            return null;
        }

        Inventory inventory = player.getInventory();
        if (activeSubWeaponSlot >= inventory.getContainerSize()) {
            return null;
        }

        ItemStack stack = inventory.getItem(activeSubWeaponSlot);
        if (!(stack.getItem() instanceof SubWeaponItem<?> subWeapon) || subWeapon.holdTime() <= 0) {
            return null;
        }

        long elapsed = player.level().getGameTime() - activeSubWeaponStartTick;
        int useTime = elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) elapsed);
        return new ActiveSubWeaponUse(InteractionHand.OFF_HAND, stack, subWeapon, useTime);
    }

    private static void handleSubWeaponHotkey(Player player, boolean canUseKeys) {
        if (!canUseKeys) {
            while (SUB_WEAPON.consumeClick()) {
                // Drop queued key presses while a screen or overlay owns input.
            }
            releaseActiveSubWeapon();
            wasSubWeaponKeyDown = false;
            return;
        }

        boolean keyDown = SUB_WEAPON.isDown();
        while (SUB_WEAPON.consumeClick()) {
            // The held state below drives start/release; consume click edges so they do not queue up.
        }

        if (keyDown && !wasSubWeaponKeyDown) {
            int slot = findSubWeaponSlot(player);
            if (slot < 0) {
                player.displayClientMessage(Component.translatable("status.cant_use"), true);
                wasSubWeaponKeyDown = true;
                return;
            }

            squidSuppressTicks = SUB_WEAPON_SQUID_DELAY;
            if (SplatcraftPlayerInfoEvents.isSquid(player)) {
                PlayerSetSquidPayload.sendToServer(false);
                SplatcraftPlayerInfoEvents.playerInfo(player).setSquid(false);
            }
            activeSubWeaponSlot = slot;
            activeSubWeaponStartTick = localSubWeaponStartTick(player, slot);
            UseSubWeaponPayload.sendToServer(slot, true);
        } else if (!keyDown && wasSubWeaponKeyDown) {
            releaseActiveSubWeapon();
        } else if (keyDown && activeSubWeaponSlot >= 0) {
            squidSuppressTicks = SUB_WEAPON_SQUID_DELAY;
        }

        wasSubWeaponKeyDown = keyDown;
    }

    private static void handleDualieDodge(Player player, boolean canUseKeys) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }

        boolean jumping = canUseKeys && localPlayer.input.jumping;
        if (jumping && !wasJumping && canRequestDualieDodge(localPlayer)) {
            DodgeRollPayload.sendToServer(localPlayer.input.leftImpulse, localPlayer.input.forwardImpulse);
        }
        wasJumping = jumping;
    }

    private static boolean isSplatcraftUseKeyDown(Minecraft minecraft, Player player, boolean canUseKeys) {
        return canUseKeys
                && minecraft.options.keyUse.isDown()
                && (isSplatcraftUseItem(player.getMainHandItem()) || isSplatcraftUseItem(player.getOffhandItem()));
    }

    private static boolean isSplatcraftUseItem(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem || stack.getItem() instanceof SubWeaponItem<?>;
    }

    private static boolean canRequestDualieDodge(LocalPlayer player) {
        if (!player.isUsingItem() || player.getCooldowns().isOnCooldown(player.getUseItem().getItem())) {
            return false;
        }
        if (!(player.getUseItem().getItem() instanceof WeaponItem weapon)
                || weapon.weaponClass() != WeaponItem.WeaponClass.DUALIE) {
            return false;
        }
        return player.input.leftImpulse != 0.0F || player.input.forwardImpulse != 0.0F;
    }

    private static boolean canRemainSquid(Player player) {
        return player.isAlive() && !player.isSleeping() && player.getVehicle() == null;
    }

    private static int findSubWeaponSlot(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof SubWeaponItem<?>) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean toggleTarget(boolean keyDown) {
        if (keyDown && !wasSquidKeyDown) {
            squidToggleActive = !squidToggleActive;
        }
        return squidToggleActive;
    }

    private static void resetState() {
        activeSubWeaponSlot = -1;
        activeSubWeaponStartTick = -1L;
        squidToggleActive = false;
        wasSquidKeyDown = false;
        wasSubWeaponKeyDown = false;
        wasJumping = false;
        squidSuppressTicks = 0;
    }

    private static void releaseActiveSubWeapon() {
        if (activeSubWeaponSlot >= 0) {
            UseSubWeaponPayload.sendToServer(activeSubWeaponSlot, false);
            activeSubWeaponSlot = -1;
            activeSubWeaponStartTick = -1L;
        }
    }

    private static long localSubWeaponStartTick(Player player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof SubWeaponItem<?> subWeapon)
                || subWeapon.holdTime() <= 0
                || !subWeapon.canStartUseFromStack(player.level(), player, stack)) {
            return -1L;
        }
        return player.level().getGameTime();
    }

    public record ActiveSubWeaponUse(
            InteractionHand hand,
            ItemStack stack,
            SubWeaponItem<?> subWeapon,
            int useTime
    ) {
    }
}
