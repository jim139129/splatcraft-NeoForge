package net.splatcraft.neoforge.item;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.SplatcraftConfig;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.player.SquidInkMovement;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.registry.SplatcraftItems;

public class InkTankItem extends ArmorItem {
    private static final String INK_KEY = "Ink";
    private static final String RECOVERY_COOLDOWN_KEY = "RecoveryCooldown";
    private static final String CANNOT_RECHARGE_KEY = "CannotRecharge";
    private static final String INFINITE_INK_KEY = "InfiniteInk";
    private static final String HIDE_TOOLTIP_KEY = "HideTooltip";
    private static final float RECHARGE_PER_TICK = 0.5F;
    private static final float SUBMERGED_RECHARGE_PER_TICK = 100.0F / 20.0F / 3.0F;

    private final int capacity;
    private final String tagId;

    public InkTankItem(int capacity, Holder<ArmorMaterial> material, Properties properties) {
        this(capacity, "ink_tank", material, properties);
    }

    public InkTankItem(int capacity, String tagId, Holder<ArmorMaterial> material, Properties properties) {
        super(material, ArmorItem.Type.CHESTPLATE, properties.stacksTo(1));
        this.capacity = capacity;
        this.tagId = tagId;
        SplatcraftTags.Items.putInkTankTags(this, tagId);
    }

    public int capacity() {
        return capacity;
    }

    public String tagId() {
        return tagId;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (getDefense() == 0 && getToughness() == 0.0F && getMaterial().value().knockbackResistance() == 0.0F) {
            return ItemAttributeModifiers.EMPTY;
        }
        return super.getDefaultAttributeModifiers(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (!SplatcraftGameRules.localizedBoolean(level, entity.blockPosition(), SplatcraftGameRules.RECHARGEABLE_INK_TANK)) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST) != stack) {
            return;
        }
        int playerColor = SplatcraftPlayerInfoEvents.color(player);
        if (InkColorComponent.isColorLocked(stack)) {
            if (InkColorComponent.colorOrDefault(stack) != playerColor) {
                return;
            }
        } else {
            InkColorComponent.setColor(stack, playerColor);
        }
        if (!canRecharge(stack, true)
                || player.getCooldowns().isOnCooldown(stack.getItem())
                || weaponActivityBlocksRecharge(player)) {
            return;
        }

        float ink = getInkAmount(stack);
        if (ink < capacity) {
            setInkAmount(stack, ink + rechargePerTick(player));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (InkColorComponent.isColorLocked(stack)) {
            tooltipComponents.add(colorName(InkColorComponent.colorOrDefault(stack)).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (hideTooltip(stack)) {
            return;
        }
        if (!canRecharge(stack, false)) {
            tooltipComponents.add(Component.translatable("item.splatcraft.ink_tank.cant_recharge").withStyle(ChatFormatting.GRAY));
        }
        if (tooltipFlag.isAdvanced()) {
            tooltipComponents.add(Component.translatable(
                    "item.splatcraft.ink_tank.ink",
                    String.format(Locale.ROOT, "%.1f", getInkAmount(stack)),
                    capacity).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return durabilityIndicatorEnabled()
                && customData(stack).contains(INK_KEY, CompoundTag.TAG_FLOAT)
                && getInkAmount(stack) < capacity;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getInkAmount(stack) / capacity * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return SplatcraftConfig.VANILLA_INK_DURABILITY_COLOR.get()
                ? super.getBarColor(stack)
                : InkColorComponent.colorOrDefault(stack);
    }

    public boolean canUse(Item item) {
        TagKey<Item> blacklist = SplatcraftTags.Items.INK_TANK_BLACKLIST.get(this);
        if (blacklist != null && item.builtInRegistryHolder().is(blacklist)) {
            return false;
        }

        TagKey<Item> whitelist = SplatcraftTags.Items.INK_TANK_WHITELIST.get(this);
        if (whitelist != null && item.builtInRegistryHolder().is(whitelist)) {
            return true;
        }

        return canUseBuiltInFallback(item);
    }

    private boolean canUseBuiltInFallback(Item item) {
        if (!(item instanceof WeaponItem)) {
            return false;
        }
        if ("ink_tank_jr".equals(tagId)) {
            return item instanceof SubWeaponItem<?>
                    || item == SplatcraftItems.SPLATTERSHOT_JR.get()
                    || item == SplatcraftItems.KENSA_SPLATTERSHOT_JR.get();
        }
        return "ink_tank".equals(tagId)
                || "classic_ink_tank".equals(tagId)
                || "armored_ink_tank".equals(tagId);
    }

    public static int tintColor(ItemStack stack, int tintIndex) {
        return InkColorComponent.tintColor(stack, tintIndex);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide) {
            return false;
        }

        InkColorComponent.lockFromInkwell(stack, entity);
        return false;
    }

    public static float getInkAmount(ItemStack stack) {
        if (!(stack.getItem() instanceof InkTankItem tank)) {
            return 0.0F;
        }

        CompoundTag tag = customData(stack);
        if (tag.getBoolean(INFINITE_INK_KEY)) {
            return tank.capacity;
        }
        if (!tag.contains(INK_KEY, CompoundTag.TAG_FLOAT)) {
            return 0.0F;
        }
        return clamp(tag.getFloat(INK_KEY), 0.0F, tank.capacity);
    }

    public static void setInkAmount(ItemStack stack, float amount) {
        if (!(stack.getItem() instanceof InkTankItem tank)) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putFloat(INK_KEY, clamp(amount, 0.0F, tank.capacity));
        });
    }

    public static void refill(ItemStack stack) {
        if (!(stack.getItem() instanceof InkTankItem tank)) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putFloat(INK_KEY, tank.capacity);
            tag.putInt(RECOVERY_COOLDOWN_KEY, 0);
        });
    }

    public static boolean consumeInk(ItemStack stack, float amount, int recoveryCooldown) {
        if (!(stack.getItem() instanceof InkTankItem) || amount <= 0.0F) {
            return true;
        }

        float current = getInkAmount(stack);
        if (current + 0.0001F < amount) {
            return false;
        }

        setInkAmount(stack, current - amount);
        setRecoveryCooldown(stack, recoveryCooldown);
        return true;
    }

    public static boolean canRecharge(ItemStack stack, boolean tickCooldown) {
        CompoundTag tag = customData(stack);
        if (tag.contains(CANNOT_RECHARGE_KEY)) {
            return false;
        }
        if (!tickCooldown) {
            return true;
        }

        int cooldown = Math.max(0, tag.getInt(RECOVERY_COOLDOWN_KEY));
        if (cooldown <= 0) {
            return true;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutableTag -> {
            mutableTag.putInt(RECOVERY_COOLDOWN_KEY, cooldown - 1);
        });
        return false;
    }

    public static void setRecoveryCooldown(ItemStack stack, int recoveryCooldown) {
        if (!(stack.getItem() instanceof InkTankItem)) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(RECOVERY_COOLDOWN_KEY, Math.max(tag.getInt(RECOVERY_COOLDOWN_KEY), recoveryCooldown));
        });
    }

    private static float rechargePerTick(Player player) {
        return SplatcraftPlayerInfoEvents.isSquid(player) && SquidInkMovement.canSquidHide(player)
                ? SUBMERGED_RECHARGE_PER_TICK
                : RECHARGE_PER_TICK;
    }

    private static boolean weaponActivityBlocksRecharge(Player player) {
        if (player.getUseItem().getItem() instanceof WeaponItem) {
            return true;
        }
        if (WeaponItem.hasActiveChargeState(player.getMainHandItem())
                || WeaponItem.hasActiveChargeState(player.getOffhandItem())) {
            return true;
        }
        return weaponCooldownBlocksRecharge(player, player.getMainHandItem())
                || weaponCooldownBlocksRecharge(player, player.getOffhandItem());
    }

    private static boolean weaponCooldownBlocksRecharge(Player player, ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof WeaponItem && player.getCooldowns().isOnCooldown(item);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static boolean hideTooltip(ItemStack stack) {
        return customData(stack).getBoolean(HIDE_TOOLTIP_KEY);
    }

    private static boolean durabilityIndicatorEnabled() {
        return SplatcraftConfig.INK_INDICATOR.get() == SplatcraftConfig.InkIndicatorMode.BOTH
                || SplatcraftConfig.INK_INDICATOR.get() == SplatcraftConfig.InkIndicatorMode.DURABILITY;
    }

    private static MutableComponent colorName(int color) {
        return InkColorData.builtInId(color)
                .map(InkTankItem::colorName)
                .orElseGet(() -> Component.literal(String.format("#%06X", color & 0xFFFFFF)));
    }

    private static MutableComponent colorName(ResourceLocation id) {
        return Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
