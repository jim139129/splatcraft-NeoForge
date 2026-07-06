package net.splatcraft.neoforge.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.scores.ScoreHolder;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.item.InkTankItem;
import net.splatcraft.neoforge.item.WaxApplicatorItem;
import net.splatcraft.neoforge.particle.SquidSoulParticleOptions;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.scoreboard.SplatcraftScoreboardHandler;

public final class SplatcraftInventoryEvents {
    private static final Map<UUID, List<MatchItemSnapshot>> MATCH_ITEM_SNAPSHOTS = new HashMap<>();

    private SplatcraftInventoryEvents() {
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (event.getNewDamage() < player.getHealth()) {
            return;
        }
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
                || !SplatcraftGameRules.localizedBoolean(player.level(), player.blockPosition(), SplatcraftGameRules.KEEP_MATCH_ITEMS)) {
            MATCH_ITEM_SNAPSHOTS.remove(player.getUUID());
            return;
        }

        List<MatchItemSnapshot> snapshots = matchItemSnapshots(player);
        if (snapshots.isEmpty()) {
            MATCH_ITEM_SNAPSHOTS.remove(player.getUUID());
        } else {
            MATCH_ITEM_SNAPSHOTS.put(player.getUUID(), snapshots);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        updateDeathStats(event);
        refillCarriedInkTanks(event.getEntity());
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        replaceInkedWoolDrops(event);

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        List<MatchItemSnapshot> snapshots = MATCH_ITEM_SNAPSHOTS.get(player.getUUID());
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        for (MatchItemSnapshot snapshot : snapshots) {
            removeDrop(event, snapshot.stack());
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (event.isCanceled() || SplatcraftPlayerInfoEvents.isSquid(player)) {
            return;
        }
        if (!(event.getTarget() instanceof Sheep sheep) || !(sheep instanceof IShearable shearable)) {
            return;
        }

        ItemStack shears = player.getItemInHand(event.getHand());
        if (!shears.is(Items.SHEARS)) {
            return;
        }

        sheep.getExistingData(SplatcraftAttachments.INK_OVERLAY.get()).ifPresent(info -> {
            int woolColor = info.woolColor();
            if (woolColor < 0) {
                return;
            }

            Level level = sheep.level();
            BlockPos pos = sheep.blockPosition();
            if (!shearable.isShearable(player, shears, level, pos)) {
                return;
            }

            if (!level.isClientSide) {
                for (ItemStack drop : shearable.onSheared(player, shears, level, pos)) {
                    shearable.spawnShearedDrop(level, pos, inkedWoolDrop(drop, woolColor));
                }
                shears.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            }
            sheep.gameEvent(GameEvent.SHEAR, player);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
        });
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof WaxApplicatorItem waxApplicator)) {
            return;
        }

        Player player = event.getEntity();
        if (event.isCanceled() || SplatcraftPlayerInfoEvents.isSquid(player)) {
            return;
        }

        if (waxApplicator.clearInk(stack, event.getLevel(), event.getPos(), player, event.getHand())) {
            event.setCanceled(true);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        Player player = event.getEntity();
        List<MatchItemSnapshot> snapshots = MATCH_ITEM_SNAPSHOTS.remove(player.getUUID());
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        for (MatchItemSnapshot snapshot : snapshots) {
            ItemStack stack = snapshot.stack().copy();
            if (!putStackInSlot(player.getInventory(), snapshot.slot(), stack) && !player.getInventory().add(stack)) {
                player.drop(stack, true, true);
            }
        }
    }

    private static void refillCarriedInkTanks(LivingEntity entity) {
        if (entity instanceof Player player) {
            Inventory inventory = player.getInventory();
            inventory.items.forEach(SplatcraftInventoryEvents::refillInkTank);
            inventory.armor.forEach(SplatcraftInventoryEvents::refillInkTank);
            inventory.offhand.forEach(SplatcraftInventoryEvents::refillInkTank);
            return;
        }

        refillInkTank(entity.getItemBySlot(EquipmentSlot.CHEST));
    }

    private static void updateDeathStats(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player target)
                || target.isSpectator()
                || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        int targetColor = SplatcraftPlayerInfoEvents.color(target);
        level.sendParticles(
                new SquidSoulParticleOptions(targetColor),
                target.getX(),
                target.getY() + 0.5D,
                target.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                1.5D);

        if (SplatcraftScoreboardHandler.hasColorCriterion(targetColor)) {
            level.getScoreboard().forAllObjectives(
                    SplatcraftScoreboardHandler.getDeathsAsColor(targetColor),
                    ScoreHolder.fromGameProfile(target.getGameProfile()),
                    score -> score.add(1));
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof Player source)) {
            return;
        }

        if (SplatcraftScoreboardHandler.hasColorCriterion(targetColor)) {
            level.getScoreboard().forAllObjectives(
                    SplatcraftScoreboardHandler.getColorKills(targetColor),
                    ScoreHolder.fromGameProfile(source.getGameProfile()),
                    score -> score.add(1));
        }

        int sourceColor = SplatcraftPlayerInfoEvents.color(source);
        if (SplatcraftScoreboardHandler.hasColorCriterion(sourceColor)) {
            level.getScoreboard().forAllObjectives(
                    SplatcraftScoreboardHandler.getKillsAsColor(sourceColor),
                    ScoreHolder.fromGameProfile(source.getGameProfile()),
                    score -> score.add(1));
        }
    }

    private static List<MatchItemSnapshot> matchItemSnapshots(Player player) {
        Inventory inventory = player.getInventory();
        List<MatchItemSnapshot> snapshots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(SplatcraftTags.Items.MATCH_ITEMS)) {
                snapshots.add(new MatchItemSnapshot(slot, stack.copy()));
            }
        }
        return snapshots;
    }

    private static boolean putStackInSlot(Inventory inventory, int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return false;
        }

        ItemStack current = inventory.getItem(slot);
        if (current.isEmpty()) {
            inventory.setItem(slot, stack);
            return true;
        }
        if (ItemStack.isSameItemSameComponents(current, stack)) {
            int originalCount = current.getCount();
            int mergedCount = Math.min(current.getMaxStackSize(), originalCount + stack.getCount());
            current.setCount(mergedCount);
            stack.shrink(mergedCount - originalCount);
            return stack.isEmpty();
        }
        return false;
    }

    private static void replaceInkedWoolDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }

        sheep.getExistingData(SplatcraftAttachments.INK_OVERLAY.get()).ifPresent(info -> {
            int woolColor = info.woolColor();
            if (woolColor < 0) {
                return;
            }

            for (ItemEntity itemEntity : event.getDrops()) {
                ItemStack stack = itemEntity.getItem();
                if (stack.is(ItemTags.WOOL)) {
                    itemEntity.setItem(inkedWoolDrop(stack, woolColor));
                }
            }
        });
    }

    private static ItemStack inkedWoolDrop(ItemStack original, int woolColor) {
        if (!original.is(ItemTags.WOOL)) {
            return original;
        }
        return InkColorComponent.setColorAndLock(
                new ItemStack(SplatcraftItems.INK_STAINED_WOOL.get(), original.getCount()),
                woolColor,
                true);
    }

    private static void removeDrop(LivingDropsEvent event, ItemStack stack) {
        int remaining = stack.getCount();
        Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext() && remaining > 0) {
            ItemStack dropStack = iterator.next().getItem();
            if (!ItemStack.isSameItemSameComponents(dropStack, stack)) {
                continue;
            }

            int removed = Math.min(remaining, dropStack.getCount());
            dropStack.shrink(removed);
            remaining -= removed;
            if (dropStack.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static void refillInkTank(ItemStack stack) {
        if (stack.getItem() instanceof InkTankItem) {
            InkTankItem.refill(stack);
        }
    }

    private record MatchItemSnapshot(int slot, ItemStack stack) {
    }
}
