package net.splatcraft.neoforge.client;

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

public class ArmoredInkTankModel extends AbstractInkTankModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "armoredinktankmodel"),
            "main");

    public ArmoredInkTankModel(ModelPart root) {
        super(root);
        ModelPart inkTank = root.getChild("body").getChild("Torso").getChild("Ink_Tank");
        for (int i = 0; i < 6; i++) {
            inkPieces.add(inkTank.getChild("InkPiece_" + i));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        createEmptyMesh(partDefinition);

        CubeDeformation deformation = new CubeDeformation(1.0F);
        PartDefinition body = partDefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(16, 0)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, deformation),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        partDefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        partDefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(40, 0).mirror()
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        PartDefinition torso = body.addOrReplaceChild("Torso", CubeListBuilder.create(), PartPose.offset(0.0F, -0.25F, 0.0F));
        PartDefinition inkTank = torso.addOrReplaceChild(
                "Ink_Tank",
                CubeListBuilder.create().texOffs(0, 19)
                        .addBox(-2.0F, 3.25F, 3.25F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 19)
                        .addBox(-2.0F, 10.25F, 3.25F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 24)
                        .addBox(1.0F, 4.25F, 3.25F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 24)
                        .addBox(1.0F, 4.25F, 6.25F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 24)
                        .addBox(-2.0F, 4.25F, 6.25F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(14, 24)
                        .addBox(-2.0F, 4.25F, 3.25F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -2.25F, -1.225F));

        for (int i = 0; i < 6; i++) {
            inkTank.addOrReplaceChild(
                    "InkPiece_" + i,
                    CubeListBuilder.create().texOffs(116, 0)
                            .addBox(-1.5F, -13.0F, 4.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                    PartPose.offset(0.0F, 23.25F, -0.75F));
        }

        return LayerDefinition.create(meshDefinition, 128, 128);
    }
}
