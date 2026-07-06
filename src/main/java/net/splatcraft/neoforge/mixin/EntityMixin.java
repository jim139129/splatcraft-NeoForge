package net.splatcraft.neoforge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.player.SquidInkMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isInvisible", at = @At("TAIL"), cancellable = true)
    private void splatcraft$isInvisible(CallbackInfoReturnable<Boolean> callback) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player
                && SplatcraftPlayerInfoEvents.isSquid(player)
                && SquidInkMovement.canSquidHide(player)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void splatcraft$setSprinting(boolean sprinting, CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        if (sprinting && entity instanceof Player player && hasSplatcraftWeaponCooldown(player)) {
            player.setSprinting(false);
            callback.cancel();
        }
    }

    private static boolean hasSplatcraftWeaponCooldown(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof WeaponItem && player.getCooldowns().isOnCooldown(stack.getItem())) {
                return true;
            }
        }
        return false;
    }
}
