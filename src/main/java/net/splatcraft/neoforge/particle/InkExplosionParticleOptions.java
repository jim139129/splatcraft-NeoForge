package net.splatcraft.neoforge.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.splatcraft.neoforge.registry.SplatcraftParticleTypes;

public record InkExplosionParticleOptions(float red, float green, float blue, float scale) implements ParticleOptions {
    public static final MapCodec<InkExplosionParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(InkExplosionParticleOptions::red),
            Codec.FLOAT.fieldOf("g").forGetter(InkExplosionParticleOptions::green),
            Codec.FLOAT.fieldOf("b").forGetter(InkExplosionParticleOptions::blue),
            Codec.FLOAT.fieldOf("scale").forGetter(InkExplosionParticleOptions::scale)
    ).apply(instance, InkExplosionParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InkExplosionParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            InkExplosionParticleOptions::red,
            ByteBufCodecs.FLOAT,
            InkExplosionParticleOptions::green,
            ByteBufCodecs.FLOAT,
            InkExplosionParticleOptions::blue,
            ByteBufCodecs.FLOAT,
            InkExplosionParticleOptions::scale,
            InkExplosionParticleOptions::new);

    public InkExplosionParticleOptions(int color, float scale) {
        this(InkParticleColors.red(color), InkParticleColors.green(color), InkParticleColors.blue(color), scale);
    }

    @Override
    public ParticleType<InkExplosionParticleOptions> getType() {
        return SplatcraftParticleTypes.INK_EXPLOSION.get();
    }
}
