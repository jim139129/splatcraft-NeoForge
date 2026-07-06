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

public record SquidSoulParticleOptions(float red, float green, float blue) implements ParticleOptions {
    public static final MapCodec<SquidSoulParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(SquidSoulParticleOptions::red),
            Codec.FLOAT.fieldOf("g").forGetter(SquidSoulParticleOptions::green),
            Codec.FLOAT.fieldOf("b").forGetter(SquidSoulParticleOptions::blue)
    ).apply(instance, SquidSoulParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SquidSoulParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            SquidSoulParticleOptions::red,
            ByteBufCodecs.FLOAT,
            SquidSoulParticleOptions::green,
            ByteBufCodecs.FLOAT,
            SquidSoulParticleOptions::blue,
            SquidSoulParticleOptions::new);

    public SquidSoulParticleOptions(int color) {
        this(InkParticleColors.red(color), InkParticleColors.green(color), InkParticleColors.blue(color));
    }

    @Override
    public ParticleType<SquidSoulParticleOptions> getType() {
        return SplatcraftParticleTypes.SQUID_SOUL.get();
    }
}
