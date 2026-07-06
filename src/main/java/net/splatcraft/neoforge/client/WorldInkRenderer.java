package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.worldink.WorldInk;
import net.splatcraft.neoforge.worldink.WorldInkStorage;

@EventBusSubscriber(modid = Splatcraft.MOD_ID, value = Dist.CLIENT)
public final class WorldInkRenderer {
    private static final ResourceLocation INK_TEXTURE = texture("inked_block");
    private static final ResourceLocation GLITTER_TEXTURE = texture("glitter");
    private static final double SURFACE_OFFSET = 0.002D;
    private static final int SEARCH_RADIUS_CHUNKS = 8;
    private static final int MAX_RENDERED_ENTRIES = 4096;

    private WorldInkRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        renderNearbyInk(level, minecraft.player.chunkPosition(), poseStack.last(), consumer);
        bufferSource.endBatch(RenderType.translucent());

        poseStack.popPose();
    }

    private static int renderNearbyInk(ClientLevel level, ChunkPos centerChunk, PoseStack.Pose pose, VertexConsumer consumer) {
        int rendered = 0;
        for (int chunkX = centerChunk.x - SEARCH_RADIUS_CHUNKS; chunkX <= centerChunk.x + SEARCH_RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = centerChunk.z - SEARCH_RADIUS_CHUNKS; chunkZ <= centerChunk.z + SEARCH_RADIUS_CHUNKS; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                rendered += renderChunkInk(level, chunkPos, pose, consumer, MAX_RENDERED_ENTRIES - rendered);
                if (rendered >= MAX_RENDERED_ENTRIES) {
                    return rendered;
                }
            }
        }
        return rendered;
    }

    private static int renderChunkInk(ClientLevel level, ChunkPos chunkPos, PoseStack.Pose pose, VertexConsumer consumer, int remaining) {
        return WorldInkStorage.getLoaded(level, chunkPos)
                .map(worldInk -> renderEntries(level, chunkPos, worldInk.inkInChunk(), pose, consumer, remaining))
                .orElse(0);
    }

    private static int renderEntries(
            ClientLevel level,
            ChunkPos chunkPos,
            Map<BlockPos, WorldInk.Entry> entries,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int remaining
    ) {
        int rendered = 0;
        for (Map.Entry<BlockPos, WorldInk.Entry> entry : entries.entrySet()) {
            if (rendered >= remaining) {
                return rendered;
            }
            BlockPos localPos = entry.getKey();
            WorldInk.Entry ink = entry.getValue();
            if (ink == null) {
                continue;
            }

            BlockPos worldPos = new BlockPos(
                    chunkPos.x * 16 + localPos.getX(),
                    localPos.getY(),
                    chunkPos.z * 16 + localPos.getZ());
            BlockState state = level.getBlockState(worldPos);
            if (state.isAir()) {
                continue;
            }

            renderBlockInk(level, worldPos, ink, pose, consumer);
            rendered++;
        }
        return rendered;
    }

    private static void renderBlockInk(ClientLevel level, BlockPos pos, WorldInk.Entry ink, PoseStack.Pose pose, VertexConsumer consumer) {
        int color = ClientInkColors.visibleColor(ink.color());
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        boolean glowing = PlayerInfo.GLOWING_INK_TYPE.equals(ink.type());
        boolean clear = PlayerInfo.CLEAR_INK_TYPE.equals(ink.type());
        int alpha = clear ? 96 : 210;
        TextureAtlasSprite inkSprite = sprite(INK_TEXTURE);
        TextureAtlasSprite glitterSprite = glowing ? sprite(GLITTER_TEXTURE) : null;

        for (Direction direction : Direction.values()) {
            if (WorldInkStorage.isInked(level, pos.relative(direction)) || isOccluded(level, pos.relative(direction))) {
                continue;
            }
            int light = glowing ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, pos.relative(direction));
            renderFace(pos, direction, pose, consumer, inkSprite, red, green, blue, alpha, light);
            if (glitterSprite != null) {
                renderFace(pos, direction, pose, consumer, glitterSprite, 255, 255, 255, 190, LightTexture.FULL_BRIGHT);
            }
        }
    }

    private static boolean isOccluded(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos);
    }

    private static void renderFace(
            BlockPos pos,
            Direction direction,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        double minX = pos.getX() - SURFACE_OFFSET;
        double minY = pos.getY() - SURFACE_OFFSET;
        double minZ = pos.getZ() - SURFACE_OFFSET;
        double maxX = pos.getX() + 1.0D + SURFACE_OFFSET;
        double maxY = pos.getY() + 1.0D + SURFACE_OFFSET;
        double maxZ = pos.getZ() + 1.0D + SURFACE_OFFSET;

        switch (direction) {
            case DOWN -> quad(consumer, pose, sprite,
                    minX, minY, maxZ, 0, 0,
                    maxX, minY, maxZ, 16, 0,
                    maxX, minY, minZ, 16, 16,
                    minX, minY, minZ, 0, 16,
                    red, green, blue, alpha, light, 0, -1, 0);
            case UP -> quad(consumer, pose, sprite,
                    minX, maxY, minZ, 0, 0,
                    maxX, maxY, minZ, 16, 0,
                    maxX, maxY, maxZ, 16, 16,
                    minX, maxY, maxZ, 0, 16,
                    red, green, blue, alpha, light, 0, 1, 0);
            case NORTH -> quad(consumer, pose, sprite,
                    maxX, minY, minZ, 0, 16,
                    maxX, maxY, minZ, 0, 0,
                    minX, maxY, minZ, 16, 0,
                    minX, minY, minZ, 16, 16,
                    red, green, blue, alpha, light, 0, 0, -1);
            case SOUTH -> quad(consumer, pose, sprite,
                    minX, minY, maxZ, 0, 16,
                    minX, maxY, maxZ, 0, 0,
                    maxX, maxY, maxZ, 16, 0,
                    maxX, minY, maxZ, 16, 16,
                    red, green, blue, alpha, light, 0, 0, 1);
            case WEST -> quad(consumer, pose, sprite,
                    minX, minY, minZ, 0, 16,
                    minX, maxY, minZ, 0, 0,
                    minX, maxY, maxZ, 16, 0,
                    minX, minY, maxZ, 16, 16,
                    red, green, blue, alpha, light, -1, 0, 0);
            case EAST -> quad(consumer, pose, sprite,
                    maxX, minY, maxZ, 0, 16,
                    maxX, maxY, maxZ, 0, 0,
                    maxX, maxY, minZ, 16, 0,
                    maxX, minY, minZ, 16, 16,
                    red, green, blue, alpha, light, 1, 0, 0);
        }
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            double x1,
            double y1,
            double z1,
            float u1,
            float v1,
            double x2,
            double y2,
            double z2,
            float u2,
            float v2,
            double x3,
            double y3,
            double z3,
            float u3,
            float v3,
            double x4,
            double y4,
            double z4,
            float u4,
            float v4,
            int red,
            int green,
            int blue,
            int alpha,
            int light,
            float normalX,
            float normalY,
            float normalZ
    ) {
        // Keep the winding outward-facing so translucent render types with culling enabled draw the ink surface.
        vertex(consumer, pose, x1, y1, z1, spriteU(sprite, u1), spriteV(sprite, v1), red, green, blue, alpha, light, normalX, normalY, normalZ);
        vertex(consumer, pose, x4, y4, z4, spriteU(sprite, u4), spriteV(sprite, v4), red, green, blue, alpha, light, normalX, normalY, normalZ);
        vertex(consumer, pose, x3, y3, z3, spriteU(sprite, u3), spriteV(sprite, v3), red, green, blue, alpha, light, normalX, normalY, normalZ);
        vertex(consumer, pose, x2, y2, z2, spriteU(sprite, u2), spriteV(sprite, v2), red, green, blue, alpha, light, normalX, normalY, normalZ);
    }

    private static float spriteU(TextureAtlasSprite sprite, float textureX) {
        return sprite.getU(textureX / 16.0F);
    }

    private static float spriteV(TextureAtlasSprite sprite, float textureY) {
        return sprite.getV(textureY / 16.0F);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            float textureX,
            float textureY,
            int red,
            int green,
            int blue,
            int alpha,
            int light,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setUv(textureX, textureY)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "blocks/" + path);
    }
}
