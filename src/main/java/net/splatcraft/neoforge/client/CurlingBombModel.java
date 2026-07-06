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
import net.splatcraft.neoforge.entity.sub.CurlingBombEntity;

public class CurlingBombModel extends EntityModel<CurlingBombEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "curling_bomb"),
            "main");

    private final ModelPart blades;
    private final ModelPart bumper1;
    private final ModelPart bumper2;
    private final ModelPart bumper3;
    private final ModelPart bumper4;
    private final ModelPart top;
    private final ModelPart body;

    public CurlingBombModel(ModelPart root) {
        blades = root.getChild("blades");
        bumper1 = root.getChild("bumper1");
        bumper2 = root.getChild("bumper2");
        bumper3 = root.getChild("bumper3");
        bumper4 = root.getChild("bumper4");
        top = root.getChild("top");
        body = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition blades = root.addOrReplaceChild("blades", CubeListBuilder.create()
                        .texOffs(26, 27).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.05F, 0.0F));
        blades.addOrReplaceChild("blade_a", CubeListBuilder.create()
                        .texOffs(2, 0).addBox(-0.5F, -1.3F, 0.7F, 1.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, 0.4363F));
        blades.addOrReplaceChild("blade_b", CubeListBuilder.create()
                        .texOffs(2, 0).addBox(-0.5F, -1.3F, 0.7F, 1.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 2.0944F, 0.4363F));
        blades.addOrReplaceChild("blade_c", CubeListBuilder.create()
                        .texOffs(2, 0).addBox(-0.5F, -1.3F, 0.7F, 1.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, -2.0944F, 0.4363F));

        addBumper(root, "bumper1", -1.5708F);
        addBumper(root, "bumper2", 3.1416F);
        addBumper(root, "bumper3", 0.0F);
        addBumper(root, "bumper4", 1.5708F);

        PartDefinition top = root.addOrReplaceChild("top", CubeListBuilder.create()
                        .texOffs(0, 9).addBox(-3.5F, -0.6F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(-0.05F)),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        PartDefinition handle = top.addOrReplaceChild("handle", CubeListBuilder.create()
                        .texOffs(0, 19).addBox(-3.5346F, 0.2775F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(21, 13).addBox(-3.5346F, -0.6225F, -1.0F, 5.0F, 1.0F, 2.0F, new CubeDeformation(0.001F)),
                PartPose.offset(2.5346F, -2.6775F, 0.0F));
        handle.addOrReplaceChild("handle_mid", CubeListBuilder.create()
                        .texOffs(22, 29).addBox(-1.2F, -0.2F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(21, 23).addBox(-1.5F, -0.2F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, 0.3847F));
        handle.addOrReplaceChild("handle_tip", CubeListBuilder.create()
                        .texOffs(0, 9).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.3706F, 2.0834F, 0.0F, 0.0F, 0.0F, -0.6109F));

        root.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(-3.5F, -4.5F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    private static void addBumper(PartDefinition root, String name, float yRot) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-3.5F, -4.5F, -5.0F, 7.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 13).addBox(-0.5F, -3.5F, -4.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, yRot, 0.0F));
    }

    @Override
    public void setupAnim(CurlingBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void prepareMobModel(CurlingBombEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        blades.yRot = entity.renderAge(partialTick) * 0.8F;
        top.y = 20.0F - Mth.clamp(entity.flashIntensity(partialTick) - 0.5F, 0.0F, 0.95F) * 3.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        blades.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper3.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper4.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        top.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
