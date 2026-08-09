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
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.worldink.WorldInk;
import net.splatcraft.neoforge.worldink.WorldInkStorage;

// Registered manually in SplatcraftClient; an @EventBusSubscriber annotation here would
// register the handler a second time and render all ink twice per frame.
public final class WorldInkRenderer {
    private static final ResourceLocation INK_TEXTURE = texture("inked_block");
    private static final ResourceLocation GLITTER_TEXTURE = texture("glitter");
    private static final double SURFACE_OFFSET = 0.002D;
    private static final int SEARCH_RADIUS_CHUNKS = 8;

    private WorldInkRenderer() {
    }

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
        // Vertices are emitted camera-relative (see renderFace): the large world->camera subtraction happens in
        // double precision before the float cast, so we must NOT also bake -camera into the pose matrix.
        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        renderNearbyInk(level, minecraft.player.chunkPosition(), camera, event.getFrustum(), poseStack.last(), consumer);
        bufferSource.endBatch(RenderType.translucent());
    }

    private static void renderNearbyInk(ClientLevel level, ChunkPos centerChunk, Vec3 camera, Frustum frustum, PoseStack.Pose pose, VertexConsumer consumer) {
        for (int chunkX = centerChunk.x - SEARCH_RADIUS_CHUNKS; chunkX <= centerChunk.x + SEARCH_RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = centerChunk.z - SEARCH_RADIUS_CHUNKS; chunkZ <= centerChunk.z + SEARCH_RADIUS_CHUNKS; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                // Skip chunks outside the view frustum so off-screen ink costs nothing. This replaces the old
                // fixed 4096-entry cap, which truncated in scan order and made distant ink vanish/pop.
                if (!frustum.isVisible(chunkBounds(level, chunkPos))) {
                    continue;
                }
                renderChunkInk(level, chunkPos, camera, pose, consumer);
            }
        }
    }

    private static AABB chunkBounds(ClientLevel level, ChunkPos chunkPos) {
        return new AABB(
                chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 1);
    }

    private static void renderChunkInk(ClientLevel level, ChunkPos chunkPos, Vec3 camera, PoseStack.Pose pose, VertexConsumer consumer) {
        WorldInkStorage.getLoaded(level, chunkPos)
                .ifPresent(worldInk -> renderEntries(level, chunkPos, worldInk.inkInChunk(), camera, pose, consumer));
    }

    private static void renderEntries(
            ClientLevel level,
            ChunkPos chunkPos,
            Map<BlockPos, WorldInk.Entry> entries,
            Vec3 camera,
            PoseStack.Pose pose,
            VertexConsumer consumer
    ) {
        for (Map.Entry<BlockPos, WorldInk.Entry> entry : entries.entrySet()) {
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

            renderBlockInk(level, worldPos, state, ink, camera, pose, consumer);
        }
    }

    private static void renderBlockInk(ClientLevel level, BlockPos pos, BlockState state, WorldInk.Entry ink, Vec3 camera, PoseStack.Pose pose, VertexConsumer consumer) {
        int color = ClientInkColors.visibleColor(ink.color());
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        boolean glowing = PlayerInfo.GLOWING_INK_TYPE.equals(ink.type());
        boolean clear = PlayerInfo.CLEAR_INK_TYPE.equals(ink.type());
        int alpha = clear ? 96 : 210;
        TextureAtlasSprite inkSprite = sprite(INK_TEXTURE);
        TextureAtlasSprite glitterSprite = glowing ? sprite(GLITTER_TEXTURE) : null;

        // Trace the block's actual outline so slabs/stairs/grates receive ink on their real surfaces instead of
        // a floating full-cube shell. An empty shape (rare non-collidable blocks) falls back to a unit cube.
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }

        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            for (Direction direction : Direction.values()) {
                // Neighbor ink/occlusion only hides a face that sits on the block boundary; interior faces of a
                // partial shape (e.g. a slab's top at y=0.5) are always exposed and must render regardless.
                if (isBoundaryFace(direction, x1, y1, z1, x2, y2, z2)) {
                    BlockPos neighbor = pos.relative(direction);
                    if (WorldInkStorage.isInked(level, neighbor) || isOccluded(level, neighbor)) {
                        continue;
                    }
                }
                int light = glowing ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, pos.relative(direction));
                renderFace(pos, camera, direction, x1, y1, z1, x2, y2, z2, pose, consumer, inkSprite, red, green, blue, alpha, light);
                if (glitterSprite != null) {
                    renderFace(pos, camera, direction, x1, y1, z1, x2, y2, z2, pose, consumer, glitterSprite, 255, 255, 255, 190, LightTexture.FULL_BRIGHT);
                }
            }
        });
    }

    private static boolean isBoundaryFace(Direction direction, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (direction) {
            case DOWN -> y1 <= 0.0D;
            case UP -> y2 >= 1.0D;
            case NORTH -> z1 <= 0.0D;
            case SOUTH -> z2 >= 1.0D;
            case WEST -> x1 <= 0.0D;
            case EAST -> x2 >= 1.0D;
        };
    }

    private static boolean isOccluded(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos);
    }

    private static void renderFace(
            BlockPos pos,
            Vec3 camera,
            Direction direction,
            double boxMinX,
            double boxMinY,
            double boxMinZ,
            double boxMaxX,
            double boxMaxY,
            double boxMaxZ,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        // Subtract the camera in double precision, then cast to float. Doing this here (rather than via a
        // -camera translation baked into the float pose matrix) keeps SURFACE_OFFSET meaningful far from origin.
        double baseX = pos.getX() - camera.x;
        double baseY = pos.getY() - camera.y;
        double baseZ = pos.getZ() - camera.z;
        double minX = baseX + boxMinX - SURFACE_OFFSET;
        double minY = baseY + boxMinY - SURFACE_OFFSET;
        double minZ = baseZ + boxMinZ - SURFACE_OFFSET;
        double maxX = baseX + boxMaxX + SURFACE_OFFSET;
        double maxY = baseY + boxMaxY + SURFACE_OFFSET;
        double maxZ = baseZ + boxMaxZ + SURFACE_OFFSET;

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
