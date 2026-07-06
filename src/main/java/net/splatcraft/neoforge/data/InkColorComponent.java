package net.splatcraft.neoforge.data;

import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;

public final class InkColorComponent {
    private static final String COLOR_KEY = "Color";
    private static final String INVERTED_KEY = "Inverted";
    private static final String COLOR_LOCKED_KEY = "ColorLocked";

    private InkColorComponent() {
    }

    public static OptionalInt color(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(COLOR_KEY)) {
            return OptionalInt.empty();
        }

        if (tag.contains(COLOR_KEY, CompoundTag.TAG_STRING)) {
            return InkColorData.resolve(tag.getString(COLOR_KEY));
        }

        return OptionalInt.of(tag.getInt(COLOR_KEY));
    }

    public static int colorOrDefault(ItemStack stack) {
        return color(stack).orElse(InkColorData.DEFAULT_COLOR);
    }

    public static ItemStack setColor(ItemStack stack, int color) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (color < 0) {
                tag.remove(COLOR_KEY);
            } else {
                tag.putInt(COLOR_KEY, color);
            }
        });
        return stack;
    }

    public static boolean isInverted(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(INVERTED_KEY);
    }

    public static ItemStack setInverted(ItemStack stack, boolean inverted) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (inverted) {
                tag.putBoolean(INVERTED_KEY, true);
            } else {
                tag.remove(INVERTED_KEY);
            }
        });
        return stack;
    }

    public static boolean isColorLocked(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(COLOR_LOCKED_KEY);
    }

    public static ItemStack setColorLocked(ItemStack stack, boolean locked) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (locked) {
                tag.putBoolean(COLOR_LOCKED_KEY, true);
            } else {
                tag.remove(COLOR_LOCKED_KEY);
            }
        });
        return stack;
    }

    public static ItemStack setColorAndLock(ItemStack stack, int color, boolean locked) {
        setColor(stack, color);
        setColorLocked(stack, locked);
        return stack;
    }

    public static boolean lockFromInkwell(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide) {
            return false;
        }

        BlockPos pos = entity.blockPosition().below();
        if (level.getBlockState(pos).is(SplatcraftBlocks.INKWELL.get())
                && level.getBlockEntity(pos) instanceof InkColorBlockEntity inkwell) {
            int inkwellColor = inkwell.effectiveColor();
            if (color(stack).orElse(-1) != inkwellColor || !isColorLocked(stack)) {
                setColorAndLock(stack, inkwellColor, true);
                return true;
            }
        }
        return false;
    }

    public static boolean clearFromInkClearingBlock(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide || color(stack).orElse(-1) == 0xFFFFFF) {
            return false;
        }

        BlockPos pos = entity.blockPosition().below();
        BlockState state = level.getBlockState(pos);
        if (!state.is(SplatcraftTags.Blocks.INK_CLEARING_BLOCKS)
                && !(state.getFluidState().is(FluidTags.WATER) && !state.isFaceSturdy(level, pos, Direction.DOWN))) {
            return false;
        }

        setColorAndLock(stack, 0xFFFFFF, false);
        return true;
    }

    public static int tintColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return -1;
        }

        int color = colorOrDefault(stack);
        return FastColor.ARGB32.opaque(isInverted(stack) ? 0xFFFFFF - color : color);
    }
}
