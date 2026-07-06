package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.sub.SuctionBombEntity;

public class SuctionBombModel extends EntityModel<SuctionBombEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "suction_bomb"),
            "main");

    private final ModelPart main;
    private final ModelPart neck;
    private final ModelPart top;

    public SuctionBombModel(ModelPart root) {
        main = root.getChild("Main");
        neck = main.getChild("Neck");
        top = neck.getChild("Top");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition main = root.addOrReplaceChild("Main", CubeListBuilder.create()
                        .texOffs(0, 10).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(14, 15).addBox(-1.0F, -4.25F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition neck = main.addOrReplaceChild("Neck", CubeListBuilder.create()
                        .texOffs(12, 10).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.2F))
                        .texOffs(0, 10).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, -3.75F, 0.0F));
        neck.addOrReplaceChild("Top", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -7.7F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(12, 0).addBox(-1.5F, -1.7F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, -2.5F, 0.0F));
        return LayerDefinition.create(meshDefinition, 32, 32);
    }

    @Override
    public void setupAnim(SuctionBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void prepareMobModel(SuctionBombEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        float shake = entity.flashIntensity(partialTick);
        if (shake > 0.0F) {
            float rotation = -Mth.sin(shake * 18.0F) * shake * 0.25F;
            neck.xRot = rotation * 0.5F;
            top.xRot = rotation;
        } else {
            neck.xRot = 0.0F;
            top.xRot = 0.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
