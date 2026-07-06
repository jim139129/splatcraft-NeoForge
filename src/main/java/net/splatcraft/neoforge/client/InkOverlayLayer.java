package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkOverlayInfo;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;

public class InkOverlayLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation[] TEXTURES = {
            overlayTexture(0),
            overlayTexture(1),
            overlayTexture(2),
            overlayTexture(3)
    };

    public InkOverlayLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        InkOverlayInfo info = livingEntity.getExistingData(SplatcraftAttachments.INK_OVERLAY.get()).orElse(null);
        if (info == null || info.amount() <= 0.0F) {
            return;
        }

        int overlay = overlayIndex(info.amount(), livingEntity.getMaxHealth());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURES[overlay]));
        getParentModel().renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(livingEntity, 0.0F),
                ClientInkColors.visibleArgb(info.color()));
    }

    private static int overlayIndex(float amount, float maxHealth) {
        float normalized = amount / Math.max(1.0F, maxHealth);
        return Mth.clamp(Mth.ceil(normalized * TEXTURES.length) - 1, 0, TEXTURES.length - 1);
    }

    private static ResourceLocation overlayTexture(int index) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/entity/ink_overlay_" + index + ".png");
    }
}
