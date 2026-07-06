package net.splatcraft.neoforge.item;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public final class InkTankUtils {
    private InkTankUtils() {
    }

    public static boolean tryConsume(Player player, Item weapon, float amount, int recoveryCooldown, boolean sendMessage) {
        return tryConsume(player, weapon, amount, recoveryCooldown, sendMessage, false);
    }

    public static boolean tryConsume(Player player, Item weapon, float amount, int recoveryCooldown, boolean sendMessage, boolean subWeapon) {
        InkCheck check = check(player, weapon, amount);
        if (check != InkCheck.OK) {
            if (!subWeapon) {
                delayRecharge(player, recoveryCooldown);
            }
            if (sendMessage) {
                sendFailureFeedback(player, subWeapon, check);
            }
            return false;
        }

        if (amount <= 0.0F || bypassesInkTank(player)) {
            return true;
        }

        Optional<ItemStack> tank = findUsableTank(player, weapon);
        return tank.isPresent() && InkTankItem.consumeInk(tank.get(), amount, recoveryCooldown);
    }

    public static boolean canConsume(Player player, Item weapon, float amount) {
        return check(player, weapon, amount) == InkCheck.OK;
    }

    public static void sendFailureFeedback(Player player, Item weapon, float amount, boolean subWeapon) {
        sendFailureFeedback(player, subWeapon, check(player, weapon, amount));
    }

    private static InkCheck check(Player player, Item weapon, float amount) {
        if (amount <= 0.0F || bypassesInkTank(player)) {
            return InkCheck.OK;
        }

        Optional<ItemStack> tank = findTank(player);
        if (tank.isEmpty()) {
            return InkCheck.NO_TANK;
        }
        if (!canUse(tank.get(), weapon)) {
            return InkCheck.INCOMPATIBLE_TANK;
        }
        if (InkTankItem.getInkAmount(tank.get()) + 0.0001F < amount) {
            return InkCheck.LOW_INK;
        }
        return InkCheck.OK;
    }

    public static void delayRecharge(Player player, Item weapon, int recoveryCooldown) {
        delayRecharge(player, recoveryCooldown);
    }

    private static void delayRecharge(Player player, int recoveryCooldown) {
        if (recoveryCooldown <= 0 || bypassesInkTank(player)) {
            return;
        }
        findTank(player)
                .ifPresent(tank -> InkTankItem.setRecoveryCooldown(tank, recoveryCooldown));
    }

    public static boolean refund(Player player, Item weapon, float amount) {
        if (amount <= 0.0F || bypassesInkTank(player)) {
            return true;
        }

        Optional<ItemStack> tank = findUsableTank(player, weapon);
        if (tank.isEmpty()) {
            return false;
        }

        InkTankItem.setInkAmount(tank.get(), InkTankItem.getInkAmount(tank.get()) + amount);
        return true;
    }

    private static boolean bypassesInkTank(Player player) {
        Level level = player.level();
        if (!SplatcraftGameRules.localizedBoolean(level, player.blockPosition(), SplatcraftGameRules.REQUIRE_INK_TANK)) {
            return true;
        }
        return player.getAbilities().instabuild
                && SplatcraftGameRules.localizedBoolean(level, player.blockPosition(), SplatcraftGameRules.INFINITE_INK_IN_CREATIVE);
    }

    private static Optional<ItemStack> findUsableTank(Player player, Item weapon) {
        return findTank(player)
                .filter(stack -> canUse(stack, weapon));
    }

    private static Optional<ItemStack> findTank(Player player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestStack.getItem() instanceof InkTankItem ? Optional.of(chestStack) : Optional.empty();
    }

    private static boolean canUse(ItemStack stack, Item weapon) {
        return stack.getItem() instanceof InkTankItem tank && tank.canUse(weapon);
    }

    public static void sendNoInkFeedback(Player player, boolean subWeapon) {
        sendFailureFeedback(player, subWeapon, InkCheck.LOW_INK);
    }

    private static void sendFailureFeedback(Player player, boolean subWeapon, InkCheck check) {
        if (check == InkCheck.OK) {
            return;
        }
        player.displayClientMessage(Component.translatable(check.translationKey()).withStyle(ChatFormatting.RED), true);
        SoundEvent sound = subWeapon ? SplatcraftSounds.NO_INK_SUB.get() : SplatcraftSounds.NO_INK.get();
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                0.8F,
                ((player.level().getRandom().nextFloat() - player.level().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
    }

    private enum InkCheck {
        OK(""),
        NO_TANK("status.no_ink_tank"),
        INCOMPATIBLE_TANK("status.ink_tank_incompatible"),
        LOW_INK("status.no_ink");

        private final String translationKey;

        InkCheck(String translationKey) {
            this.translationKey = translationKey;
        }

        private String translationKey() {
            return translationKey;
        }
    }
}
