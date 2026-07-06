package net.splatcraft.neoforge.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.item.InkTankUtils;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

final class RollerRollSound extends AbstractTickableSoundInstance {
    private final Player player;

    RollerRollSound(Player player, boolean brush) {
        super(sound(brush), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (!shouldPlay(player)) {
            stop();
            return;
        }

        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();

        Vec3 motion = player.getDeltaMovement();
        double movement = motion.multiply(1.0D, 0.0D, 1.0D).length();
        double turn = Math.abs(player.yHeadRotO - player.yHeadRot);
        double activity = Math.max(turn, movement) * 3.0D;
        this.volume = activity >= 0.01D ? Mth.lerp((float) Mth.clamp(activity, 0.0D, 0.5D), 0.0F, 1.0F) : 0.0F;
    }

    void stopSound() {
        stop();
    }

    static boolean shouldPlay(Player player) {
        if (player == null || !player.isAlive() || SplatcraftPlayerInfoEvents.isSquid(player)) {
            return false;
        }

        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof WeaponItem weapon) || !weapon.isRollerRollActiveForUse(player, stack)) {
            return false;
        }

        return InkTankUtils.canConsume(player, weapon, weapon.rollerRollInkConsumptionForUse(player, stack));
    }

    static boolean isBrush(Player player) {
        if (player == null) {
            return false;
        }

        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof WeaponItem weapon && weapon.isRollerBrush();
    }

    private static SoundEvent sound(boolean brush) {
        return brush ? SplatcraftSounds.BRUSH_ROLL.get() : SplatcraftSounds.ROLLER_ROLL.get();
    }
}
