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
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.SquidBumperEntity;

public class SquidBumperColorLayer extends RenderLayer<SquidBumperEntity, SquidBumperModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/entity/squid_bumper.png");

    private final SquidBumperModel model;

    public SquidBumperColorLayer(RenderLayerParent<SquidBumperEntity, SquidBumperModel> parent, EntityModelSet modelSet) {
        super(parent);
        model = new SquidBumperModel(modelSet.bakeLayer(SquidBumperModel.LAYER_LOCATION));
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            SquidBumperEntity bumper,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (bumper.isInvisible()) {
            return;
        }

        getParentModel().copyPropertiesTo(model);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(bumper, 0.0F),
                ClientInkColors.visibleArgb(bumper.color()));
    }
}
