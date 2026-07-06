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

public record InkSplashParticleOptions(float red, float green, float blue, float scale) implements ParticleOptions {
    public static final MapCodec<InkSplashParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(InkSplashParticleOptions::red),
            Codec.FLOAT.fieldOf("g").forGetter(InkSplashParticleOptions::green),
            Codec.FLOAT.fieldOf("b").forGetter(InkSplashParticleOptions::blue),
            Codec.FLOAT.fieldOf("scale").forGetter(InkSplashParticleOptions::scale)
    ).apply(instance, InkSplashParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InkSplashParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            InkSplashParticleOptions::red,
            ByteBufCodecs.FLOAT,
            InkSplashParticleOptions::green,
            ByteBufCodecs.FLOAT,
            InkSplashParticleOptions::blue,
            ByteBufCodecs.FLOAT,
            InkSplashParticleOptions::scale,
            InkSplashParticleOptions::new);

    public InkSplashParticleOptions(int color, float scale) {
        this(InkParticleColors.red(color), InkParticleColors.green(color), InkParticleColors.blue(color), scale);
    }

    @Override
    public ParticleType<InkSplashParticleOptions> getType() {
        return SplatcraftParticleTypes.INK_SPLASH.get();
    }
}
