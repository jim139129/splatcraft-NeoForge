package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;

public class SpawnShieldRenderer extends EntityRenderer<SpawnShieldEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/blocks/allowed_color_barrier_fancy.png");

    public SpawnShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            SpawnShieldEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        int activeTime = entity.activeTime();
        if (activeTime <= 0) {
            return;
        }

        float size = entity.shieldSize();
        float radius = size / 2.0F;
        int color = ClientInkColors.visibleColor(entity.color());
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = Math.round(255.0F * activeTime / SpawnShieldEntity.MAX_ACTIVE_TIME);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();

        quad(consumer, pose, -radius, size, -radius, 0, size, radius, size, -radius, size, size,
                radius, 0, -radius, size, 0, -radius, 0, -radius, 0, 0, red, green, blue, alpha, 0.0F, 0.0F, -1.0F);
        quad(consumer, pose, -radius, 0, radius, 0, 0, radius, 0, radius, size, 0,
                radius, size, radius, size, size, -radius, size, radius, 0, size, red, green, blue, alpha, 0.0F, 0.0F, 1.0F);
        quad(consumer, pose, -radius, 0, -radius, 0, 0, -radius, 0, radius, 0, size,
                -radius, size, radius, size, size, -radius, size, -radius, size, 0, red, green, blue, alpha, -1.0F, 0.0F, 0.0F);
        quad(consumer, pose, radius, 0, -radius, 0, 0, radius, size, -radius, size, 0,
                radius, size, radius, size, size, radius, 0, radius, 0, size, red, green, blue, alpha, 1.0F, 0.0F, 0.0F);
        quad(consumer, pose, -radius, 0, -radius, 0, 0, radius, 0, -radius, size, 0,
                radius, 0, radius, size, size, -radius, 0, radius, 0, size, red, green, blue, alpha, 0.0F, -1.0F, 0.0F);
        quad(consumer, pose, -radius, size, radius, 0, size, radius, size, radius, size, size,
                radius, size, -radius, size, 0, -radius, size, -radius, 0, 0, red, green, blue, alpha, 0.0F, 1.0F, 0.0F);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float u1,
            float v1,
            float x2,
            float y2,
            float z2,
            float u2,
            float v2,
            float x3,
            float y3,
            float z3,
            float u3,
            float v3,
            float x4,
            float y4,
            float z4,
            float u4,
            float v4,
            int red,
            int green,
            int blue,
            int alpha,
            float normalX,
            float normalY,
            float normalZ
    ) {
        vertex(consumer, pose, x1, y1, z1, u1, v1, red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(consumer, pose, x2, y2, z2, u2, v2, red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(consumer, pose, x3, y3, z3, u3, v3, red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(consumer, pose, x4, y4, z4, u4, v4, red, green, blue, alpha, normalX, normalY, normalZ);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float textureX,
            float textureY,
            int red,
            int green,
            int blue,
            int alpha,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(textureX, textureY)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    @Override
    public ResourceLocation getTextureLocation(SpawnShieldEntity entity) {
        return TEXTURE;
    }
}
