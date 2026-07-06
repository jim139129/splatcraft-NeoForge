package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.blockentity.RemotePedestalBlockEntity;

public class RemotePedestalRenderer implements BlockEntityRenderer<RemotePedestalBlockEntity> {
    public RemotePedestalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            RemotePedestalBlockEntity pedestal,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack remote = pedestal.getItem(0);
        if (remote.isEmpty()) {
            return;
        }

        Level level = pedestal.getLevel();
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                remote,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                level,
                (int) pedestal.getBlockPos().asLong());
        poseStack.popPose();
    }
}
