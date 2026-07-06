package net.splatcraft.neoforge.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;

public class PlaceBlockDispenseBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        setSuccess(false);
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            BlockPos pos = source.pos().relative(direction);
            Direction placeDirection = source.level().isEmptyBlock(pos.below()) ? direction : Direction.UP;
            setSuccess(blockItem.place(new DirectionalPlaceContext(source.level(), pos, direction, stack, placeDirection)).consumesAction());
        }

        return stack;
    }
}
