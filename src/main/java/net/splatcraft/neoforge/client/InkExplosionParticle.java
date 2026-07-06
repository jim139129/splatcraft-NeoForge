package net.splatcraft.neoforge.client;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.splatcraft.neoforge.particle.InkExplosionParticleOptions;

public class InkExplosionParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected InkExplosionParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            InkExplosionParticleOptions options,
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
        this.lifetime = 6 + this.random.nextInt(4);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime
                || this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).getFluidState().is(FluidTags.WATER)) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<InkExplosionParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(
                InkExplosionParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new InkExplosionParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, this.sprites);
        }
    }
}
