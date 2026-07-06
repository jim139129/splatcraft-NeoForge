package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.InkProjectileEntity;

public class InkProjectileRenderer extends EntityRenderer<InkProjectileEntity> {
    private final Map<String, InkProjectileModel> models;

    public InkProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        models = Map.of(
                "default", new InkProjectileModel(context.bakeLayer(InkProjectileModel.LAYER_LOCATION)),
                "shooter", new ShooterInkProjectileModel(context.bakeLayer(ShooterInkProjectileModel.LAYER_LOCATION)),
                "charger", new ShooterInkProjectileModel(context.bakeLayer(ShooterInkProjectileModel.LAYER_LOCATION)),
                "blaster", new BlasterInkProjectileModel(context.bakeLayer(BlasterInkProjectileModel.LAYER_LOCATION)),
                "roller", new RollerInkProjectileModel(context.bakeLayer(RollerInkProjectileModel.LAYER_LOCATION)));
    }

    @Override
    public void render(
            InkProjectileEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (entity.isInvisible()) {
            return;
        }

        if (entity.tickCount < 3 && entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D) {
            return;
        }

        String type = entity.projectileType();
        float scale = entity.projectileSize() * ("default".equals(type) ? 1.0F : 2.5F);
        InkProjectileModel model = models.getOrDefault(type, models.get("default"));
        ResourceLocation texture = getTextureLocation(entity);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.4D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.scale(scale, scale, scale);

        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, entityYaw, entity.getXRot());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ClientInkColors.visibleArgb(entity.color()));
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(InkProjectileEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Splatcraft.MOD_ID,
                "textures/entity/ink_projectile_" + entity.projectileType() + ".png");
    }
}
