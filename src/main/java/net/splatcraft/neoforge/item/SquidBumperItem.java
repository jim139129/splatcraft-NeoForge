package net.splatcraft.neoforge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.entity.SquidBumperEntity;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftEntities;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public class SquidBumperItem extends Item {
    public SquidBumperItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boolean inverted = InkColorComponent.isInverted(stack);
        if (InkColorComponent.isColorLocked(stack)) {
            tooltip.add(colorName(effectiveColor(InkColorComponent.colorOrDefault(stack), inverted)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color" + (inverted ? ".inverted" : ""))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player player && !InkColorComponent.isColorLocked(stack)) {
            InkColorComponent.setColor(stack, SplatcraftPlayerInfoEvents.color(player));
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!InkColorComponent.lockFromInkwell(stack, entity) && InkColorComponent.isColorLocked(stack)) {
            InkColorComponent.clearFromInkClearingBlock(stack, entity);
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPos pos = new BlockPlaceContext(context).getClickedPos();
        Vec3 center = Vec3.atBottomCenterOf(pos);
        AABB bounds = SplatcraftEntities.SQUID_BUMPER.get().getDimensions().makeBoundingBox(center);
        if (!level.noCollision(null, bounds) || !level.getEntities(null, bounds).isEmpty()) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (!level.isClientSide) {
            SquidBumperEntity bumper = SplatcraftEntities.SQUID_BUMPER.get().create(level);
            if (bumper == null) {
                return InteractionResult.FAIL;
            }

            bumper.moveTo(center.x(), center.y(), center.z(), placementYaw(context), 0.0F);
            bumper.setYHeadRot(bumper.getYRot());
            bumper.yHeadRotO = bumper.getYRot();
            bumper.setColor(placementColor(stack, player));
            level.addFreshEntity(bumper);
            level.playSound(null, bumper.getX(), bumper.getY(), bumper.getZ(), SplatcraftSounds.SQUID_BUMPER_PLACE.get(), SoundSource.BLOCKS, 0.75F, 0.8F);
        }

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int placementColor(ItemStack stack, Player player) {
        int color;
        if (InkColorComponent.isColorLocked(stack)) {
            color = InkColorComponent.colorOrDefault(stack);
        } else {
            color = player == null ? InkColorComponent.colorOrDefault(stack) : SplatcraftPlayerInfoEvents.color(player);
        }
        return effectiveColor(color, InkColorComponent.isInverted(stack));
    }

    private static float placementYaw(UseOnContext context) {
        return (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
    }

    private static int effectiveColor(int color, boolean inverted) {
        return inverted ? 0xFFFFFF - color : color;
    }

    private static MutableComponent colorName(int color) {
        return InkColorData.builtInId(color)
                .map(SquidBumperItem::colorName)
                .orElseGet(() -> Component.literal(String.format("#%06X", color & 0xFFFFFF)));
    }

    private static MutableComponent colorName(ResourceLocation id) {
        return Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath());
    }
}
