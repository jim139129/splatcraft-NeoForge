package net.splatcraft.neoforge.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class AbstractInkTankModel extends HumanoidModel<LivingEntity> {
    protected final List<ModelPart> inkPieces = new ArrayList<>();

    public AbstractInkTankModel(ModelPart root) {
        super(root);
    }

    public void prepareForRender(float inkPercent, EquipmentSlot armorSlot, HumanoidModel<?> defaultModel) {
        copyDefaultProperties(defaultModel);
        this.rightLeg.visible = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
        this.leftLeg.visible = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
        this.body.visible = armorSlot == EquipmentSlot.CHEST;
        this.leftArm.visible = armorSlot == EquipmentSlot.CHEST;
        this.rightArm.visible = armorSlot == EquipmentSlot.CHEST;
        this.head.visible = armorSlot == EquipmentSlot.HEAD;
        this.hat.visible = armorSlot == EquipmentSlot.HEAD;
        setInkLevels(Mth.clamp(inkPercent, 0.0F, 1.0F));
    }

    public void setInkLevels(float inkPercent) {
        for (int i = 1; i <= inkPieces.size(); i++) {
            ModelPart box = inkPieces.get(i - 1);
            if (inkPercent <= 0.0F) {
                box.visible = false;
                continue;
            }
            box.visible = true;
            box.y = 23.25F - Math.min(i * inkPercent, i);
        }
    }

    public static void createEmptyMesh(PartDefinition partDefinition) {
        partDefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        partDefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        partDefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        partDefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        partDefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        partDefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void copyDefaultProperties(HumanoidModel<?> defaultModel) {
        ((HumanoidModel) defaultModel).copyPropertiesTo(this);
    }
}
