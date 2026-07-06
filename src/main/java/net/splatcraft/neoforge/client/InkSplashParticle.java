package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.splatcraft.neoforge.particle.InkSplashParticleOptions;

public class InkSplashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected InkSplashParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            InkSplashParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        int color = ClientInkColors.visibleColor(options.red(), options.green(), options.blue());
        this.rCol = Math.max(0.018F, ClientInkColors.red(color) - 0.018F);
        this.gCol = Math.max(0.018F, ClientInkColors.green(color) - 0.018F);
        this.bCol = Math.max(0.018F, ClientInkColors.blue(color) - 0.018F);
        this.quadSize = 0.66F * (this.random.nextFloat() * 0.5F + 0.5F) * options.scale();
        this.gravity = 0.0F;
        this.lifetime = 5;
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).getFluidState().is(FluidTags.WATER)) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.getCameraType() == CameraType.FIRST_PERSON
                && minecraft.player != null
                && this.distanceToSqr(minecraft.player, this.x, this.y, this.z) < 0.2D) {
            return;
        }
        super.render(buffer, renderInfo, partialTicks);
    }

    private double distanceToSqr(Entity entity, double x, double y, double z) {
        double dx = entity.getX() - x;
        double dy = entity.getEyeY() - y;
        double dz = entity.getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static class Provider implements ParticleProvider<InkSplashParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(
                InkSplashParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new InkSplashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, this.sprites);
        }
    }
}
