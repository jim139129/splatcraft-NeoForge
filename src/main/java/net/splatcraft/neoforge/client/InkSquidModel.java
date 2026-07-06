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
import net.splatcraft.neoforge.entity.InkSquidEntity;

public class InkSquidModel extends EntityModel<InkSquidEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "ink_squid"),
            "main");

    private final ModelPart squid;
    private final ModelPart rightLimb;
    private final ModelPart leftLimb;

    public InkSquidModel(ModelPart root) {
        squid = root.getChild("squid");
        rightLimb = squid.getChild("RightLimb");
        leftLimb = squid.getChild("LeftLimb");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition squid = root.addOrReplaceChild("squid", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = squid.addOrReplaceChild("Body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 9).addBox(-6.0F, -5.0F, -6.0F, 12.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(27, 0).addBox(-5.0F, -4.0F, -8.0F, 10.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 6).addBox(-4.0F, -3.0F, -10.0F, 8.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 12).addBox(-2.0F, -2.0F, -12.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("eyes", CubeListBuilder.create()
                        .texOffs(18, 19).addBox(-2.5F, -5.0F, -2.0F, 5.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(-3.0F, -4.5F, -2.25F, 6.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("tentacles", CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-2.6593F, -3.75F, 6.6593F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(-1.495F, -3.75F, 5.495F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(-0.1161F, -2.25F, 4.1161F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(-1.495F, -2.25F, 5.495F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(-0.1161F, -3.75F, 4.1161F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(0.9875F, -3.75F, 2.9671F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 0.0F, -2.25F, 0.0F, -0.7854F, 0.0F));

        squid.addOrReplaceChild("LeftLimb", CubeListBuilder.create()
                        .texOffs(0, 23).addBox(0.0F, -3.0F, 0.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 29).addBox(-1.0F, -3.0F, 3.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 0.0F, 2.0F));

        squid.addOrReplaceChild("RightLimb", CubeListBuilder.create()
                        .texOffs(10, 23).mirror().addBox(-2.0F, -3.0F, 0.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
                        .texOffs(14, 29).mirror().addBox(-2.0F, -3.0F, 3.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(-2.0F, 0.0F, 2.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void setupAnim(InkSquidEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void prepareMobModel(InkSquidEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);

        boolean swimming = entity.isSwimming();
        squid.xRot = swimming ? -entity.getXRot() * ((float) Math.PI / 180.0F) : 0.0F;

        if (entity.onGround() || swimming) {
            rightLimb.yRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / (swimming ? 2.2F : 1.5F);
            leftLimb.yRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount / (swimming ? 2.2F : 1.5F);
        } else {
            rightLimb.yRot -= rightLimb.yRot / 8.0F;
            leftLimb.yRot -= leftLimb.yRot / 8.0F;
        }
    }

    @Override
    public void copyPropertiesTo(EntityModel<InkSquidEntity> model) {
        super.copyPropertiesTo(model);
        if (model instanceof InkSquidModel inkSquidModel) {
            inkSquidModel.squid.copyFrom(squid);
            inkSquidModel.leftLimb.copyFrom(leftLimb);
            inkSquidModel.rightLimb.copyFrom(rightLimb);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        squid.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
