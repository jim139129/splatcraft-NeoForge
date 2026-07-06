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

public class ShooterInkProjectileModel extends InkProjectileModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "shooter_ink_projectile"),
            "main");

    public ShooterInkProjectileModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition main = root.addOrReplaceChild("main", CubeListBuilder.create()
                        .texOffs(0, 5).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition middle = main.addOrReplaceChild("middle", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -0.75F, 2.0F, 2.0F, 3.0F, new CubeDeformation(-0.3F)),
                PartPose.offset(0.0F, -1.0F, 1.5F));
        middle.addOrReplaceChild("back", CubeListBuilder.create()
                        .texOffs(7, 0).addBox(-0.5F, -0.5F, -0.75F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.15F)),
                PartPose.offset(0.0F, 0.0F, 2.0F));
        return LayerDefinition.create(meshDefinition, 16, 16);
    }
}
