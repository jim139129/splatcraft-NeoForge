package net.splatcraft.neoforge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class FilterItem extends Item {
    private final boolean glowing;
    private final boolean omni;

    public FilterItem(Properties properties) {
        this(properties, false, false);
    }

    public FilterItem(Properties properties, boolean glowing, boolean omni) {
        super(properties.stacksTo(1));
        this.glowing = glowing;
        this.omni = omni;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.splatcraft.filter.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return glowing;
    }

    public boolean isOmni() {
        return omni;
    }
}
