package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.particle.SquidSoulParticleOptions;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SquidSoulParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected SquidSoulParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SquidSoulParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        int color = ClientInkColors.visibleColor(options.red(), options.green(), options.blue());
        this.rCol = Math.max(0.018F, ClientInkColors.red(color) - 0.018F);
        this.gCol = Math.max(0.018F, ClientInkColors.green(color) - 0.018F);
        this.bCol = Math.max(0.018F, ClientInkColors.blue(color) - 0.018F);
        this.gravity = 0.15F;
        this.lifetime = 20;
        this.quadSize = 0.3F;
        this.hasPhysics = false;
        this.sprites = sprites;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.yd += 0.04D * (double) this.gravity;
        this.move(0.0D, this.yd, 0.0D);
        this.yd *= 0.98D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float xOffset = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float yOffset = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float zOffset = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        if (this.roll != 0.0F) {
            rotation.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        }

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float quadSize = this.getQuadSize(partialTicks);
        for (Vector3f vertex : vertices) {
            vertex.rotate(rotation).mul(quadSize).add(xOffset, yOffset, zOffset);
        }

        for (int i = 0; i < 3; i++) {
            float red = i == 1 ? this.rCol : 1.0F;
            float green = i == 1 ? this.gCol : 1.0F;
            float blue = i == 1 ? this.bCol : 1.0F;
            float alpha = this.alpha;
            if (this.age > this.lifetime - 5) {
                alpha = (1.0F - Math.max(0.0F, this.age - this.lifetime + 5.0F) - partialTicks) * 0.2F;
            }

            this.setSprite(this.sprites.get(i + 1, 3));
            float u0 = this.getU0();
            float u1 = this.getU1();
            float v0 = this.getV0();
            float v1 = this.getV1();
            int light = 15728880;

            buffer.addVertex(vertices[0]).setUv(u1, v1).setColor(red, green, blue, alpha).setLight(light);
            buffer.addVertex(vertices[1]).setUv(u1, v0).setColor(red, green, blue, alpha).setLight(light);
            buffer.addVertex(vertices[2]).setUv(u0, v0).setColor(red, green, blue, alpha).setLight(light);
            buffer.addVertex(vertices[3]).setUv(u0, v1).setColor(red, green, blue, alpha).setLight(light);
        }
    }

    public static class Provider implements ParticleProvider<SquidSoulParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(
                SquidSoulParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new SquidSoulParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, this.sprites);
        }
    }
}
