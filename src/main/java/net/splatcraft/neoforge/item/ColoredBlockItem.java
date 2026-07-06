package net.splatcraft.neoforge.item;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftData;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public class ColoredBlockItem extends BlockItem {
    private final boolean matchColor;
    private Supplier<? extends ItemLike> clearItem;
    private boolean addStarterColors;

    public ColoredBlockItem(Block block, Properties properties, boolean matchColor) {
        super(block, properties);
        this.matchColor = matchColor;
    }

    public boolean matchesColor() {
        return matchColor;
    }

    public ColoredBlockItem clearsTo(ItemLike clearItem) {
        this.clearItem = () -> clearItem;
        return this;
    }

    public ColoredBlockItem clearsTo(Supplier<? extends ItemLike> clearItem) {
        this.clearItem = clearItem;
        return this;
    }

    public ColoredBlockItem clearsToSelf() {
        return clearsTo(() -> this);
    }

    public Optional<Item> clearItem() {
        return clearItem == null ? Optional.empty() : Optional.of(clearItem.get().asItem());
    }

    public ColoredBlockItem addStarterColors() {
        this.addStarterColors = true;
        return this;
    }

    public boolean addsStarterColors() {
        return addStarterColors;
    }

    public void fillCreativeTab(CreativeModeTab.Output output) {
        ItemStack uncolored = new ItemStack(this);
        InkColorComponent.setColor(uncolored, -1);
        output.accept(uncolored);

        if (!addStarterColors) {
            return;
        }

        ItemStack inverted = new ItemStack(this);
        InkColorComponent.setColorLocked(inverted, false);
        InkColorComponent.setInverted(inverted, true);
        output.accept(inverted);

        for (int color : SplatcraftData.starterColors()) {
            output.accept(InkColorComponent.setColorAndLock(new ItemStack(this), color, true));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !matchColor || !(entity instanceof Player player)
                || InkColorComponent.isColorLocked(stack)) {
            return;
        }

        int playerColor = SplatcraftPlayerInfoEvents.color(player);
        if (InkColorComponent.color(stack).orElse(-1) != playerColor) {
            InkColorComponent.setColor(stack, playerColor);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (InkColorComponent.lockFromInkwell(stack, entity)) {
            return false;
        }

        Optional<Item> clear = clearItem();
        if (clear.isPresent() && shouldClear(stack, entity)) {
            entity.setItem(new ItemStack(clear.get(), stack.getCount()));
        }
        return false;
    }

    private boolean shouldClear(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide) {
            return false;
        }

        if (clearItem().filter(item -> item == this).isPresent() && InkColorComponent.color(stack).isEmpty()) {
            return false;
        }

        BlockPos pos = entity.blockPosition();
        BlockState state = level.getBlockState(pos);
        return state.is(SplatcraftTags.Blocks.INK_CLEARING_BLOCKS)
                || state.getFluidState().is(FluidTags.WATER) && !state.isFaceSturdy(level, pos, Direction.DOWN);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boolean inverted = InkColorComponent.isInverted(stack);
        if (InkColorComponent.isColorLocked(stack)) {
            tooltip.add(colorName(InkColorComponent.colorOrDefault(stack)).withStyle(ChatFormatting.GRAY));
            if (inverted) {
                tooltip.add(Component.translatable("item.splatcraft.tooltip.inverted")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            }
        } else if (matchColor) {
            tooltip.add(Component.translatable(
                    "item.splatcraft.tooltip.matches_color" + (inverted ? ".inverted" : ""))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            BlockPos pos,
            Level level,
            Player player,
            ItemStack stack,
            BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof InkColorBlockEntity colorBlock) {
            InkColorComponent.color(stack).ifPresent(colorBlock::setColor);
            colorBlock.setInverted(InkColorComponent.isInverted(stack));
        } else if (blockEntity instanceof ColoredBarrierBlockEntity barrier) {
            InkColorComponent.color(stack).ifPresent(barrier::setColor);
            barrier.setInverted(InkColorComponent.isInverted(stack));
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    public static int tintColor(ItemStack stack, int tintIndex) {
        return InkColorComponent.tintColor(stack, tintIndex);
    }

    public static float hasColor(ItemStack stack) {
        return InkColorComponent.color(stack).isPresent() ? 1.0F : 0.0F;
    }

    private static MutableComponent colorName(int color) {
        return InkColorData.builtInId(color)
                .map(ColoredBlockItem::colorName)
                .orElseGet(() -> Component.literal(String.format("#%06X", color & 0xFFFFFF)));
    }

    private static MutableComponent colorName(ResourceLocation id) {
        return Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath());
    }
}
