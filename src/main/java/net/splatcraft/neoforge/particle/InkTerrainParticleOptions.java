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

public record InkTerrainParticleOptions(float red, float green, float blue) implements ParticleOptions {
    public static final MapCodec<InkTerrainParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(InkTerrainParticleOptions::red),
            Codec.FLOAT.fieldOf("g").forGetter(InkTerrainParticleOptions::green),
            Codec.FLOAT.fieldOf("b").forGetter(InkTerrainParticleOptions::blue)
    ).apply(instance, InkTerrainParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InkTerrainParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            InkTerrainParticleOptions::red,
            ByteBufCodecs.FLOAT,
            InkTerrainParticleOptions::green,
            ByteBufCodecs.FLOAT,
            InkTerrainParticleOptions::blue,
            InkTerrainParticleOptions::new);

    public InkTerrainParticleOptions(int color) {
        this(InkParticleColors.red(color), InkParticleColors.green(color), InkParticleColors.blue(color));
    }

    @Override
    public ParticleType<InkTerrainParticleOptions> getType() {
        return SplatcraftParticleTypes.INK_TERRAIN.get();
    }
}
