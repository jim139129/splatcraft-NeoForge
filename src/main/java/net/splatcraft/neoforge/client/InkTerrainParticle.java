package net.splatcraft.neoforge.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.splatcraft.neoforge.particle.InkTerrainParticleOptions;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;

public class InkTerrainParticle extends TextureSheetParticle {
    private final BlockPos pos;
    private final float uo;
    private final float vo;

    protected InkTerrainParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            InkTerrainParticleOptions options
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pos = BlockPos.containing(x, y, z);
        this.setSprite(Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModelShaper()
                .getParticleIcon(SplatcraftBlocks.INKED_BLOCK.get().defaultBlockState()));
        this.gravity = 1.0F;
        int color = ClientInkColors.visibleColor(options.red(), options.green(), options.blue());
        this.rCol = 0.6F * ClientInkColors.red(color);
        this.gCol = 0.6F * ClientInkColors.green(color);
        this.bCol = 0.6F * ClientInkColors.blue(color);
        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public int getLightColor(float partialTick) {
        int light = super.getLightColor(partialTick);
        return light == 0 && this.level.hasChunkAt(this.pos) ? LevelRenderer.getLightColor(this.level, this.pos) : light;
    }

    public static class Provider implements ParticleProvider<InkTerrainParticleOptions> {
        @Nullable
        @Override
        public Particle createParticle(
                InkTerrainParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new InkTerrainParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options);
        }
    }
}
