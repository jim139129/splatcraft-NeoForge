package net.splatcraft.neoforge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public class ColoredArmorItem extends ArmorItem {
    public ColoredArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties.stacksTo(1));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.EMPTY;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        if (!InkColorComponent.isColorLocked(stack)) {
            InkColorComponent.setColor(stack, SplatcraftPlayerInfoEvents.color(player));
        }
        syncDyedColor(stack);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide) {
            return false;
        }

        InkColorComponent.lockFromInkwell(stack, entity);
        syncDyedColor(stack);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (InkColorComponent.isColorLocked(stack)) {
            tooltip.add(colorName(InkColorComponent.colorOrDefault(stack)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color").withStyle(ChatFormatting.GRAY));
        }
    }

    public static int tintColor(ItemStack stack, int tintIndex) {
        return tintIndex > 0 ? -1 : InkColorComponent.tintColor(stack, tintIndex);
    }

    private static void syncDyedColor(ItemStack stack) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(InkColorComponent.tintColor(stack, 0) & 0xFFFFFF, false));
    }

    private static MutableComponent colorName(int color) {
        return InkColorData.builtInId(color)
                .map(ColoredArmorItem::colorName)
                .orElseGet(() -> Component.literal(String.format("#%06X", color & 0xFFFFFF)));
    }

    private static MutableComponent colorName(ResourceLocation id) {
        return Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath());
    }
}
