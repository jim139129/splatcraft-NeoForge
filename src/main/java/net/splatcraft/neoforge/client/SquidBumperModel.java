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
import net.splatcraft.neoforge.entity.SquidBumperEntity;

public class SquidBumperModel extends EntityModel<SquidBumperEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "squid_bumper"),
            "main");

    private final ModelPart base;
    private final ModelPart bumper;
    private float scale = 1.0F;

    public SquidBumperModel(ModelPart root) {
        base = root.getChild("Base");
        bumper = root.getChild("Bumper");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        root.addOrReplaceChild("Base", CubeListBuilder.create()
                        .texOffs(0, 46).addBox(-5.0F, -2.0F, -5.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition bumper = root.addOrReplaceChild("Bumper", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0F, -16.0F, -7.0F, 14.0F, 14.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 28).addBox(-6.0F, -22.0F, -6.0F, 12.0F, 6.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 1).addBox(-5.0F, -27.0F, -5.0F, 10.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 17).addBox(-4.0F, -30.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        bumper.addOrReplaceChild("Left_Side", CubeListBuilder.create()
                        .texOffs(72, 28).addBox(-11.3308F, -12.0465F, -1.5F, 10.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.3308F, -12.7034F, 0.5F, 0.0F, 0.0F, 0.7854F));

        bumper.addOrReplaceChild("Right_Side", CubeListBuilder.create()
                        .texOffs(48, 28).mirror().addBox(1.3261F, -12.0465F, -1.5F, 10.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offsetAndRotation(-3.3308F, -12.7034F, 0.5F, 0.0F, 0.0F, -0.7854F));

        return LayerDefinition.create(meshDefinition, 128, 128);
    }

    @Override
    public void setupAnim(SquidBumperEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void prepareMobModel(SquidBumperEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);

        scale = entity.bumperScale(partialTick);
        bumper.yRot = Mth.lerp(partialTick, entity.yHeadRotO, entity.yHeadRot) * ((float) Math.PI / 180.0F) + (float) Math.PI;

        base.xRot = 0.0F;
        base.yRot = 0.0F;
        base.zRot = 0.0F;

        bumper.y = 24.0F;
        if (entity.inkHealth() <= 0.0F && scale > 0.0F) {
            bumper.y = 24.0F / scale;
        }
    }

    @Override
    public void copyPropertiesTo(EntityModel<SquidBumperEntity> model) {
        super.copyPropertiesTo(model);
        if (model instanceof SquidBumperModel squidBumperModel) {
            squidBumperModel.base.copyFrom(base);
            squidBumperModel.bumper.copyFrom(bumper);
            squidBumperModel.scale = scale;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        bumper.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
