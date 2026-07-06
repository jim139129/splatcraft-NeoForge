package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.SplatcraftConfig;
import net.splatcraft.neoforge.network.payload.PlayerSkinOverlayPayload;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public class PlayerInkColoredSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<UUID, ResourceLocation> TEXTURES = new HashMap<>();
    private static final String PATH = "config/skins/";

    private final PlayerModel<AbstractClientPlayer> model;

    public PlayerInkColoredSkinLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            PlayerModel<AbstractClientPlayer> model
    ) {
        super(parent);
        this.model = model;
    }

    public static void uploadLocalOverlay(LocalPlayer player) {
        Path path = Paths.get(SplatcraftConfig.INK_COLORED_SKIN_LAYER_PATH.get());
        if (!Files.isRegularFile(path)) {
            return;
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] imageBytes = NativeImage.read(inputStream).asByteArray();
            if (imageBytes.length > PlayerSkinOverlayPayload.MAX_IMAGE_BYTES) {
                Splatcraft.LOGGER.warn("Player ink skin overlay {} is too large: {} bytes", path, imageBytes.length);
                return;
            }
            PlayerSkinOverlayPayload.sendToServer(player.getUUID(), imageBytes);
        } catch (IOException exception) {
            Splatcraft.LOGGER.warn("Failed to upload player ink skin overlay from {}", path, exception);
        }
    }

    public static void apply(UUID playerId, byte[] imageBytes) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation location = textureLocation(playerId);
        minecraft.getTextureManager().release(location);
        TEXTURES.remove(playerId);

        if (imageBytes.length == 0) {
            return;
        }

        try {
            DynamicTexture texture = new DynamicTexture(NativeImage.read(new ByteArrayInputStream(imageBytes)));
            minecraft.getTextureManager().register(location, texture);
            TEXTURES.put(playerId, location);
        } catch (IOException exception) {
            Splatcraft.LOGGER.warn("Failed to load player ink skin overlay for {}", playerId, exception);
        }
    }

    public static void clearTextures() {
        Minecraft minecraft = Minecraft.getInstance();
        TEXTURES.values().forEach(minecraft.getTextureManager()::release);
        TEXTURES.clear();
    }

    public static void renderHand(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        ResourceLocation texture = TEXTURES.get(player.getUUID());
        if (texture == null || player.isSpectator() || player.isInvisible()) {
            return;
        }

        EntityRenderer<? super AbstractClientPlayer> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        ModelPart arm = event.getArm() == HumanoidArm.LEFT ? playerModel.leftArm : playerModel.rightArm;
        ModelPart sleeve = event.getArm() == HumanoidArm.LEFT ? playerModel.leftSleeve : playerModel.rightSleeve;
        int color = ClientInkColors.visibleArgb(SplatcraftPlayerInfoEvents.color(player));
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(texture));

        playerModel.attackTime = 0.0F;
        playerModel.crouching = false;
        playerModel.swimAmount = 0.0F;
        playerModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.scale(1.001F, 1.001F, 1.001F);
        arm.xRot = 0.0F;
        arm.render(poseStack, consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY, color);
        sleeve.xRot = 0.0F;
        sleeve.render(poseStack, consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
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
        ResourceLocation texture = TEXTURES.get(player.getUUID());
        if (texture == null || player.isSpectator() || player.isInvisible()) {
            return;
        }

        copyPlayerModelProperties(getParentModel(), model);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        model.renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ClientInkColors.visibleArgb(SplatcraftPlayerInfoEvents.color(player)));
    }

    private static void copyPlayerModelProperties(PlayerModel<AbstractClientPlayer> from, PlayerModel<AbstractClientPlayer> to) {
        from.copyPropertiesTo(to);
        to.jacket.copyFrom(from.jacket);
        to.rightSleeve.copyFrom(from.rightSleeve);
        to.leftSleeve.copyFrom(from.leftSleeve);
        to.rightPants.copyFrom(from.rightPants);
        to.leftPants.copyFrom(from.leftPants);

        to.jacket.visible = from.jacket.visible;
        to.rightSleeve.visible = from.rightSleeve.visible;
        to.leftSleeve.visible = from.leftSleeve.visible;
        to.rightPants.visible = from.rightPants.visible;
        to.leftPants.visible = from.leftPants.visible;
    }

    private static ResourceLocation textureLocation(UUID playerId) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, PATH + playerId);
    }
}
