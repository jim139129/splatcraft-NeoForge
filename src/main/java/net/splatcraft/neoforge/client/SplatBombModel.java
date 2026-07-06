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
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.sub.SplatBombEntity;

public class SplatBombModel extends EntityModel<SplatBombEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "splat_bomb"),
            "main");

    private final ModelPart main;

    public SplatBombModel(ModelPart root) {
        main = root.getChild("Main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition main = root.addOrReplaceChild("Main", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, 0.0F));

        main.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-3.0F, -5.6F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-3.0F, -0.1F, -3.4F, 6.0F, 0.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(12, 18).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 4.0F, 0.0F));

        addFin(main, "front_fin", 0.0F);
        addFin(main, "left_fin", 2.0944F);
        addFin(main, "right_fin", -2.0944F);
        addFoot(main, "front_foot", 0.0F, 0.0F, -2.7F);
        addFoot(main, "left_foot", 2.0944F, -2.3F, 1.45F);
        addFoot(main, "right_foot", -2.0944F, 2.3F, 1.45F);

        return LayerDefinition.create(meshDefinition, 32, 32);
    }

    private static void addFin(PartDefinition root, String name, float yRot) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(0, 6).addBox(-3.0F, -6.0F, -0.25F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.25F, -2.2F, -0.2618F, yRot, 0.0F));
    }

    private static void addFoot(PartDefinition root, String name, float yRot, float x, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.1F))
                        .texOffs(12, 14).addBox(-3.5F, -0.55F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(x, 3.0F, z, 0.0F, yRot, 0.0F));
    }

    @Override
    public void setupAnim(SplatBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
