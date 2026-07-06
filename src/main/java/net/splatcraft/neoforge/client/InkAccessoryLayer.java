package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public class InkAccessoryLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final HumanoidModel<AbstractClientPlayer> model;

    public InkAccessoryLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            HumanoidModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        PlayerInfo info = SplatcraftPlayerInfoEvents.playerInfo(player);
        ItemStack inkBand = info.inkBand();
        if (inkBand.isEmpty() || player.isSpectator() || player.isInvisible()) {
            return;
        }
        if (ItemStack.matches(inkBand, player.getMainHandItem())
                || ItemStack.matches(inkBand, player.getOffhandItem())) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(inkBand.getItem());
        String suffix = customModelSuffix(inkBand, itemId);
        ResourceLocation texture = modelTexture(itemId, suffix, false);
        ResourceLocation coloredTexture = modelTexture(itemId, suffix, true);

        if (!hasResource(texture)) {
            return;
        }

        getParentModel().copyPropertiesTo(model);
        model.leftArm.visible = player.getMainArm() == HumanoidArm.LEFT;
        model.leftLeg.visible = player.getMainArm() == HumanoidArm.LEFT;
        model.rightArm.visible = player.getMainArm() == HumanoidArm.RIGHT;
        model.rightLeg.visible = player.getMainArm() == HumanoidArm.RIGHT;

        boolean foil = inkBand.hasFoil();
        renderModel(poseStack, bufferSource, packedLight, foil, texture, -1);
        if (hasResource(coloredTexture)) {
            renderModel(
                    poseStack,
                    bufferSource,
                    packedLight,
                    foil,
                    coloredTexture,
                    ClientInkColors.visibleArgb(SplatcraftPlayerInfoEvents.color(player)));
        }
    }

    private static String customModelSuffix(ItemStack stack, ResourceLocation itemId) {
        CustomModelData customModelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (customModelData == null) {
            return "";
        }

        String suffix = "_" + customModelData.value();
        return hasResource(modelTexture(itemId, suffix, false)) ? suffix : "";
    }

    private static ResourceLocation modelTexture(ResourceLocation itemId, String suffix, boolean colored) {
        return ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(),
                "textures/models/" + itemId.getPath() + suffix + (colored ? "_colored" : "") + ".png");
    }

    private static boolean hasResource(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    private void renderModel(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            boolean foil,
            ResourceLocation texture,
            int color) {
        VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(
                bufferSource,
                RenderType.armorCutoutNoCull(texture),
                foil);
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
    }
}
