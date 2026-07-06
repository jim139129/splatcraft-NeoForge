package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.sub.AbstractSubWeaponEntity;

public abstract class SubWeaponRenderer<E extends AbstractSubWeaponEntity, M extends EntityModel<E>> extends EntityRenderer<E> {
    private final M model;
    private final ResourceLocation texture;
    private final ResourceLocation inkTexture;
    private final ResourceLocation overlayTexture;

    protected SubWeaponRenderer(
            EntityRendererProvider.Context context,
            M model,
            String texture,
            String inkTexture,
            String overlayTexture
    ) {
        super(context);
        this.model = model;
        this.texture = subTexture(texture);
        this.inkTexture = subTexture(inkTexture);
        this.overlayTexture = overlayTexture == null ? null : subTexture(overlayTexture);
    }

    @Override
    public void render(E entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        applyTransforms(entity, partialTick, poseStack);

        model.prepareMobModel(entity, 0.0F, 0.0F, partialTick);
        model.setupAnim(entity, 0.0F, 0.0F, entity.renderAge(partialTick), entityYaw, entity.getXRot());
        int overlay = OverlayTexture.pack(getOverlayProgress(entity, partialTick), false);

        renderModel(poseStack, bufferSource, packedLight, overlay, inkTexture, ClientInkColors.visibleArgb(entity.color()));
        renderModel(poseStack, bufferSource, packedLight, overlay, texture, FastColor.ARGB32.opaque(0xFFFFFF));

        if (overlayTexture != null) {
            renderModel(poseStack, bufferSource, packedLight, overlay, overlayTexture, getOverlayColor(entity, partialTick));
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(E entity) {
        return texture;
    }

    protected void applyTransforms(E entity, float partialTick, PoseStack poseStack) {
    }

    protected float getOverlayProgress(E entity, float partialTick) {
        return 0.0F;
    }

    protected int getOverlayColor(E entity, float partialTick) {
        return FastColor.ARGB32.opaque(0xFFFFFF);
    }

    protected static float pulsingScale(float flashIntensity, float axisWeight) {
        float wobble = 1.0F + Mth.sin(flashIntensity * 100.0F) * flashIntensity * 0.01F;
        float flash = flashIntensity * flashIntensity;
        flash *= flash;
        return (1.0F + flash * axisWeight) * wobble;
    }

    private void renderModel(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int overlay,
            ResourceLocation texture,
            int color
    ) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, consumer, packedLight, overlay, color);
    }

    private static ResourceLocation subTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/weapons/sub/" + name + ".png");
    }
}
