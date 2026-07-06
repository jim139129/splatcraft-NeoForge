package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.splatcraft.neoforge.entity.sub.SplatBombEntity;

public class SplatBombRenderer extends SubWeaponRenderer<SplatBombEntity, SplatBombModel> {
    public SplatBombRenderer(EntityRendererProvider.Context context) {
        super(context, new SplatBombModel(context.bakeLayer(SplatBombModel.LAYER_LOCATION)), "splat_bomb", "splat_bomb_ink", null);
    }

    @Override
    protected void applyTransforms(SplatBombEntity entity, float partialTick, PoseStack poseStack) {
        if (entity.isItem) {
            return;
        }

        poseStack.translate(0.0D, 0.2D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) * 2.0F - 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) * 2.0F - 180.0F));

        float flash = entity.flashIntensity(partialTick);
        float wobble = 1.0F + Mth.sin(flash * 100.0F) * flash * 0.01F;
        float pulse = flash * flash;
        pulse *= pulse;
        poseStack.scale((1.0F + pulse * 0.4F) * wobble, (1.0F + pulse * 0.1F) / Math.max(0.001F, wobble), (1.0F + pulse * 0.4F) * wobble);
    }

    @Override
    protected float getOverlayProgress(SplatBombEntity entity, float partialTick) {
        float flash = entity.flashIntensity(partialTick);
        return (int) (flash * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(flash, 0.5F, 1.0F);
    }
}
