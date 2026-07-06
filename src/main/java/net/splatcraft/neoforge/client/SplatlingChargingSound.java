package net.splatcraft.neoforge.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

final class SplatlingChargingSound extends AbstractTickableSoundInstance {
    private static final float SECOND_LEVEL_START = 1.0F;
    private static final float MAX_CHARGE = 2.0F;

    private final Player player;
    private final boolean secondLevel;

    SplatlingChargingSound(Player player, boolean secondLevel) {
        super(
                secondLevel
                        ? SplatcraftSounds.SPLATLING_CHARGE_SECOND_LEVEL.get()
                        : SplatcraftSounds.SPLATLING_CHARGE.get(),
                SoundSource.PLAYERS,
                SoundInstance.createUnseededRandom());
        this.player = player;
        this.secondLevel = secondLevel;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();

        if (!shouldPlay(player)) {
            stop();
            return;
        }

        float charge = currentCharge(player);
        if (isSecondLevelCharge(charge) != secondLevel) {
            stop();
            return;
        }

        float levelCharge = secondLevel ? charge - SECOND_LEVEL_START : charge;
        this.pitch = Mth.clamp(levelCharge, 0.0F, 1.0F) * 0.5F + 0.5F;
    }

    boolean isSecondLevel() {
        return secondLevel;
    }

    void stopSound() {
        stop();
    }

    static boolean shouldPlay(Player player) {
        if (player == null || !player.isAlive() || !player.isUsingItem() || SplatcraftPlayerInfoEvents.isSquid(player)) {
            return false;
        }

        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof WeaponItem weapon && weapon.weaponClass() == WeaponItem.WeaponClass.SPLATLING;
    }

    static boolean usesSecondLevel(Player player) {
        return isSecondLevelCharge(currentCharge(player));
    }

    private static boolean isSecondLevelCharge(float charge) {
        return charge > SECOND_LEVEL_START;
    }

    private static float currentCharge(Player player) {
        if (player == null) {
            return 0.0F;
        }

        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof WeaponItem weapon) || weapon.weaponClass() != WeaponItem.WeaponClass.SPLATLING) {
            return 0.0F;
        }

        int useTime = weapon.getUseDuration(stack, player) - player.getUseItemRemainingTicks();
        return Mth.clamp(weapon.splatlingChargeForUse(stack, useTime), 0.0F, MAX_CHARGE);
    }
}
