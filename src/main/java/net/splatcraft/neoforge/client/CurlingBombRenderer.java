package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.splatcraft.neoforge.entity.sub.CurlingBombEntity;

public class CurlingBombRenderer extends SubWeaponRenderer<CurlingBombEntity, CurlingBombModel> {
    public CurlingBombRenderer(EntityRendererProvider.Context context) {
        super(context, new CurlingBombModel(context.bakeLayer(CurlingBombModel.LAYER_LOCATION)), "curling_bomb", "curling_bomb_ink", "curling_bomb_overlay");
    }

    @Override
    protected void applyTransforms(CurlingBombEntity entity, float partialTick, PoseStack poseStack) {
        if (entity.isItem) {
            poseStack.translate(0.0D, -1.5D, 0.0D);
            return;
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 180.0F));
        float flash = entity.flashIntensity(partialTick);
        float wobble = 1.0F + Mth.sin(flash * 100.0F) * flash * 0.01F;
        float pulse = flash * flash;
        pulse *= pulse;
        poseStack.scale((1.0F + pulse * 0.4F) * wobble, -((1.0F + pulse * 0.1F) / Math.max(0.001F, wobble)), (1.0F + pulse * 0.4F) * wobble);
        poseStack.translate(0.0D, -1.5D, 0.0D);
    }

    @Override
    protected float getOverlayProgress(CurlingBombEntity entity, float partialTick) {
        float flash = entity.flashIntensity(partialTick);
        return (int) (flash * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(flash, 0.5F, 1.0F);
    }

    @Override
    protected int getOverlayColor(CurlingBombEntity entity, float partialTick) {
        float value = Mth.clamp(entity.flashIntensity(partialTick), 0.0F, 1.0F);
        return FastColor.ARGB32.color(255, Math.round(value * 255.0F), Math.round((1.0F - value) * 255.0F), 0);
    }
}
