package net.splatcraft.neoforge.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public class PowerEggCanItem extends Item {
    public PowerEggCanItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SplatcraftSounds.POWER_EGG_CAN_OPEN.get(),
                SoundSource.PLAYERS,
                0.5F,
                0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    player.getX(),
                    player.getEyeY() - 0.3D,
                    player.getZ(),
                    new ItemStack(SplatcraftItems.POWER_EGG.get(), (level.random.nextInt(4) + 1) * 10));
            itemEntity.setNoPickUpDelay();
            itemEntity.setThrower(player);

            float speed = level.random.nextFloat() * 0.5F;
            float angle = level.random.nextFloat() * ((float) Math.PI * 2.0F);
            itemEntity.setDeltaMovement(-Math.sin(angle) * speed, 0.2D, Math.cos(angle) * speed);

            level.addFreshEntity(itemEntity);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
