package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.sub.AbstractSubWeaponEntity;
import net.splatcraft.neoforge.item.SubWeaponItem;

public class SubWeaponItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final SubWeaponItemRenderer INSTANCE = new SubWeaponItemRenderer();
    private static final Map<String, ModelResourceLocation> GUI_MODELS = Map.of(
            "splat_bomb",
            guiModel("splat_bomb"),
            "splat_bomb_2",
            guiModel("splat_bomb_2"),
            "burst_bomb",
            guiModel("burst_bomb"),
            "suction_bomb",
            guiModel("suction_bomb"),
            "curling_bomb",
            guiModel("curling_bomb"));

    private SubWeaponItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerGuiModels(ModelEvent.RegisterAdditional event) {
        GUI_MODELS.values().forEach(event::register);
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (displayContext == ItemDisplayContext.GUI && renderGuiModel(stack, poseStack, bufferSource, packedLight, packedOverlay)) {
            return;
        }

        if (!(stack.getItem() instanceof SubWeaponItem<?> subWeapon)) {
            super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        AbstractSubWeaponEntity entity = AbstractSubWeaponEntity.create(subWeapon.entityType(), level, 0.0D, 0.0D, 0.0D, stack);
        entity.isItem = true;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.55D, 0.5D);
        minecraft.getEntityRenderDispatcher().getRenderer(entity).render(
                entity,
                0.0F,
                minecraft.getTimer().getGameTimeDeltaPartialTick(true),
                poseStack,
                bufferSource,
                packedLight);
        poseStack.popPose();
    }

    private static boolean renderGuiModel(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!Splatcraft.MOD_ID.equals(itemId.getNamespace())) {
            return false;
        }

        ModelResourceLocation modelLocation = GUI_MODELS.get(itemId.getPath());
        if (modelLocation == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel guiModel = minecraft.getModelManager().getModel(modelLocation);
        if (guiModel == minecraft.getModelManager().getMissingModel()) {
            return false;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        Lighting.setupForFlatItems();
        try {
            minecraft.getItemRenderer().render(
                    stack,
                    ItemDisplayContext.GUI,
                    false,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    guiModel);
        } finally {
            Lighting.setupFor3DItems();
            poseStack.popPose();
        }
        return true;
    }

    private static ModelResourceLocation guiModel(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "item/" + name + "_gui"));
    }
}
