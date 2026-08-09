package net.splatcraft.neoforge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.player.SquidInkMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Unique
    private int splatcraft$cooldownCheckTick = Integer.MIN_VALUE;
    @Unique
    private boolean splatcraft$hasWeaponCooldown;

    @Inject(method = "isInvisible", at = @At("TAIL"), cancellable = true)
    private void splatcraft$isInvisible(CallbackInfoReturnable<Boolean> callback) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player
                && SplatcraftPlayerInfoEvents.isSquid(player)
                && SquidInkMovement.canSquidHide(player)) {
            callback.setReturnValue(true);
        }
    }

    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
    private boolean splatcraft$preventWeaponCooldownSprinting(boolean sprinting) {
        Entity entity = (Entity) (Object) this;
        if (sprinting && entity instanceof Player player && hasSplatcraftWeaponCooldown(player)) {
            return false;
        }
        return sprinting;
    }

    private boolean hasSplatcraftWeaponCooldown(Player player) {
        if (splatcraft$cooldownCheckTick == player.tickCount) {
            return splatcraft$hasWeaponCooldown;
        }

        splatcraft$cooldownCheckTick = player.tickCount;
        splatcraft$hasWeaponCooldown = false;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof WeaponItem && player.getCooldowns().isOnCooldown(stack.getItem())) {
                splatcraft$hasWeaponCooldown = true;
                break;
            }
        }
        return splatcraft$hasWeaponCooldown;
    }
}
