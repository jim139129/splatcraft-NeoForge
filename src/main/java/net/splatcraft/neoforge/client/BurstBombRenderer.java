package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.splatcraft.neoforge.entity.sub.BurstBombEntity;

public class BurstBombRenderer extends SubWeaponRenderer<BurstBombEntity, BurstBombModel> {
    public BurstBombRenderer(EntityRendererProvider.Context context) {
        super(context, new BurstBombModel(context.bakeLayer(BurstBombModel.LAYER_LOCATION)), "burst_bomb", "burst_bomb_ink", null);
    }

    @Override
    protected void applyTransforms(BurstBombEntity entity, float partialTick, PoseStack poseStack) {
        if (entity.isItem) {
            return;
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F));
        poseStack.scale(1.0F, -1.0F, 1.0F);
    }
}
