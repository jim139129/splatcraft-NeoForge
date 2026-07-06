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

final class ChargerChargingSound extends AbstractTickableSoundInstance {
    private final Player player;

    ChargerChargingSound(Player player) {
        super(SplatcraftSounds.CHARGER_CHARGE.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
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

        ItemStack stack = player.getUseItem();
        WeaponItem weapon = (WeaponItem) stack.getItem();
        int useTime = weapon.getUseDuration(stack, player) - player.getUseItemRemainingTicks();
        float charge = Mth.clamp(weapon.chargerChargeForUse(player, useTime), 0.0F, 1.0F);
        if (charge >= 1.0F) {
            stop();
            return;
        }

        this.pitch = charge * 0.5F + 0.5F;
    }

    void stopSound() {
        stop();
    }

    static boolean shouldPlay(Player player) {
        if (player == null || !player.isAlive() || !player.isUsingItem() || SplatcraftPlayerInfoEvents.isSquid(player)) {
            return false;
        }

        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof WeaponItem weapon) || weapon.weaponClass() != WeaponItem.WeaponClass.CHARGER) {
            return false;
        }

        int useTime = weapon.getUseDuration(stack, player) - player.getUseItemRemainingTicks();
        return weapon.chargerChargeForUse(player, useTime) < 1.0F;
    }
}
