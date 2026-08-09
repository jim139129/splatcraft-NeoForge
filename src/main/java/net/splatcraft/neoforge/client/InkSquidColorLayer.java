package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.SplatcraftEntityState;

public class InkSquidColorLayer<T extends LivingEntity> extends RenderLayer<T, InkSquidModel<T>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/entity/ink_squid.png");

    private final InkSquidModel<T> model;

    public InkSquidColorLayer(RenderLayerParent<T, InkSquidModel<T>> parent, EntityModelSet modelSet) {
        super(parent);
        model = new InkSquidModel<>(modelSet.bakeLayer(InkSquidModel.LAYER_LOCATION));
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (entity.isInvisible()) {
            return;
        }

        getParentModel().copyPropertiesTo(model);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                ClientInkColors.visibleArgb(SplatcraftEntityState.color(entity)));
    }
}
