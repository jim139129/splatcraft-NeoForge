package net.splatcraft.neoforge.item;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.blockentity.InkedBlockEntity;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.worldink.InkBlockUtils;
import net.splatcraft.neoforge.worldink.WorldInk;
import net.splatcraft.neoforge.worldink.WorldInkStorage;

public class WaxApplicatorItem extends Item {
    public WaxApplicatorItem(Properties properties) {
        super(properties.durability(256));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return hasWaxableInk(level, pos) ? InteractionResult.SUCCESS : super.useOn(context);
        }

        if (waxWorldInk(level, pos) || waxInkedBlock(level, pos)) {
            level.levelEvent(context.getPlayer(), 3003, pos, 0);
            hurt(context.getItemInHand(), context.getPlayer(), context.getHand());
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    public boolean clearInk(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return false;
        }

        if (InkBlockUtils.clearInk(level, pos, true)) {
            hurt(stack, player, hand);
            return true;
        }
        return false;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 0.0F;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.HONEYCOMB) || super.isValidRepairItem(stack, repairCandidate);
    }

    private static boolean hasWaxableInk(Level level, BlockPos pos) {
        return WorldInkStorage.getInk(level, pos).isPresent() || level.getBlockEntity(pos) instanceof InkedBlockEntity;
    }

    private static boolean waxWorldInk(Level level, BlockPos pos) {
        WorldInk.Entry ink = WorldInkStorage.getInk(level, pos).orElse(null);
        return ink != null && WorldInkStorage.setPermanentInk(level, pos, ink.color(), ink.type());
    }

    private static boolean waxInkedBlock(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof InkedBlockEntity inkedBlock)) {
            return false;
        }

        int color = inkedBlock.getColor();
        ResourceLocation inkType = inkType(level.getBlockState(pos));
        if (inkedBlock.getPermanentColor() == color && inkedBlock.getPermanentInkType().equals(inkType)) {
            return false;
        }

        inkedBlock.setPermanentColor(color);
        inkedBlock.setPermanentInkType(inkType);
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 2);
        return true;
    }

    private static ResourceLocation inkType(BlockState state) {
        if (state.is(SplatcraftBlocks.GLOWING_INKED_BLOCK.get())) {
            return PlayerInfo.GLOWING_INK_TYPE;
        }
        if (state.is(SplatcraftBlocks.CLEAR_INKED_BLOCK.get())) {
            return PlayerInfo.CLEAR_INK_TYPE;
        }
        return PlayerInfo.NORMAL_INK_TYPE;
    }

    private static void hurt(ItemStack stack, Player player, InteractionHand hand) {
        if (player != null && !player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
    }
}
