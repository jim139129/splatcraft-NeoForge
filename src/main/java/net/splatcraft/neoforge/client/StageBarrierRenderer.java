package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.SplatcraftConfig;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.blockentity.StageBarrierBlockEntity;
import net.splatcraft.neoforge.data.SplatcraftTags;

public class StageBarrierRenderer implements BlockEntityRenderer<StageBarrierBlockEntity> {
    public StageBarrierRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            StageBarrierBlockEntity barrier,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        int activeTime = visibleActiveTime(barrier);
        if (activeTime <= 0) {
            return;
        }

        int color = barrierColor(barrier);
        int alpha = Mth.clamp(Math.round(255.0F * activeTime / StageBarrierBlockEntity.MAX_ACTIVE_TIME), 0, 255);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture(barrier.getBlockState())));
        PoseStack.Pose pose = poseStack.last();

        for (Direction direction : Direction.values()) {
            if (shouldRenderSide(barrier, direction)) {
                renderFace(consumer, pose, direction, color, alpha);
            }
        }
    }

    @Override
    public int getViewDistance() {
        return SplatcraftConfig.BARRIER_RENDER_DISTANCE.get();
    }

    private static int visibleActiveTime(StageBarrierBlockEntity barrier) {
        if (canRevealForEditing(barrier.getBlockPos())) {
            return StageBarrierBlockEntity.MAX_ACTIVE_TIME;
        }
        return barrier.getActiveTime();
    }

    private static boolean canRevealForEditing(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isCreative()) {
            return false;
        }

        int renderDistance = SplatcraftConfig.BARRIER_RENDER_DISTANCE.get();
        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > renderDistance * renderDistance) {
            return false;
        }
        return !SplatcraftConfig.HOLD_BARRIER_TO_RENDER.get()
                || player.getMainHandItem().is(SplatcraftTags.Items.REVEALS_BARRIERS)
                || player.getOffhandItem().is(SplatcraftTags.Items.REVEALS_BARRIERS);
    }

    private static boolean shouldRenderSide(StageBarrierBlockEntity barrier, Direction side) {
        Level level = barrier.getLevel();
        if (level == null) {
            return false;
        }

        BlockPos pos = barrier.getBlockPos();
        BlockPos relativePos = pos.relative(side);
        BlockState relativeState = level.getBlockState(relativePos);

        if (isColoredBarrier(barrier.getBlockState()) && isColoredBarrier(relativeState)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null
                    || !(barrier instanceof ColoredBarrierBlockEntity current)
                    || !(level.getBlockEntity(relativePos) instanceof ColoredBarrierBlockEntity relative)) {
                return false;
            }
            return current.canAllowThrough(player) != relative.canAllowThrough(player);
        }

        if (isStageBarrier(relativeState)) {
            return false;
        }
        return !relativeState.isSolidRender(level, relativePos);
    }

    private static boolean isStageBarrier(BlockState state) {
        return state.getBlock() instanceof BlockStateCompatBlocks.StageBarrierBlockEntityBlock;
    }

    private static boolean isColoredBarrier(BlockState state) {
        return state.getBlock() instanceof BlockStateCompatBlocks.ColorBarrierBlockEntityBlock;
    }

    private static int barrierColor(StageBarrierBlockEntity barrier) {
        if (!(barrier instanceof ColoredBarrierBlockEntity colored)) {
            return 0xFFFFFF;
        }

        int color = colored.getColor();
        if (color < 0) {
            return 0xFFFFFF;
        }
        return ClientInkColors.visibleColor(colored.isInverted() ? 0xFFFFFF - color : color);
    }

    private static ResourceLocation texture(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = blockId.getPath();
        if (Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST) {
            path += "_fancy";
        }
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/blocks/" + path + ".png");
    }

    private static void renderFace(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Direction direction,
            int color,
            int alpha
    ) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        switch (direction) {
            case DOWN -> quad(consumer, pose,
                    0, 0, 0, 0, 0,
                    1, 0, 0, 1, 0,
                    1, 0, 1, 1, 1,
                    0, 0, 1, 0, 1,
                    red, green, blue, alpha, 0, -1, 0);
            case UP -> quad(consumer, pose,
                    0, 1, 1, 0, 1,
                    1, 1, 1, 1, 1,
                    1, 1, 0, 1, 0,
                    0, 1, 0, 0, 0,
                    red, green, blue, alpha, 0, 1, 0);
            case NORTH -> quad(consumer, pose,
                    0, 1, 0, 0, 1,
                    1, 1, 0, 1, 1,
                    1, 0, 0, 1, 0,
                    0, 0, 0, 0, 0,
                    red, green, blue, alpha, 0, 0, -1);
            case SOUTH -> quad(consumer, pose,
                    0, 0, 1, 0, 0,
                    1, 0, 1, 1, 0,
                    1, 1, 1, 1, 1,
                    0, 1, 1, 0, 1,
                    red, green, blue, alpha, 0, 0, 1);
            case WEST -> quad(consumer, pose,
                    0, 0, 0, 0, 0,
                    0, 0, 1, 0, 1,
                    0, 1, 1, 1, 1,
                    0, 1, 0, 1, 0,
                    red, green, blue, alpha, -1, 0, 0);
            case EAST -> quad(consumer, pose,
                    1, 0, 0, 0, 0,
                    1, 1, 0, 1, 0,
                    1, 1, 1, 1, 1,
                    1, 0, 1, 0, 1,
                    red, green, blue, alpha, 1, 0, 0);
        }
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
}
