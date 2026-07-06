package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.splatcraft.neoforge.entity.sub.SuctionBombEntity;

public class SuctionBombRenderer extends SubWeaponRenderer<SuctionBombEntity, SuctionBombModel> {
    public SuctionBombRenderer(EntityRendererProvider.Context context) {
        super(context, new SuctionBombModel(context.bakeLayer(SuctionBombModel.LAYER_LOCATION)), "suction_bomb", "suction_bomb_ink", null);
    }

    @Override
    protected void applyTransforms(SuctionBombEntity entity, float partialTick, PoseStack poseStack) {
        if (entity.isItem) {
            return;
        }

        poseStack.translate(0.0D, 0.15D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F));
        poseStack.translate(0.0D, -0.15D, 0.0D);
        poseStack.scale(1.0F, -1.0F, 1.0F);

        float flash = entity.flashIntensity(partialTick);
        float xz = pulsingScale(flash, 0.4F);
        float y = (1.0F + flash * flash * flash * flash * 0.1F) / Math.max(0.001F, 1.0F + Mth.sin(flash * 100.0F) * flash * 0.01F);
        poseStack.scale(xz, y, xz);
    }

    @Override
    protected float getOverlayProgress(SuctionBombEntity entity, float partialTick) {
        float flash = entity.flashIntensity(partialTick);
        return (int) (flash * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(flash, 0.5F, 1.0F);
    }
}
