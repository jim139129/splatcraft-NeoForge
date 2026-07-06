package net.splatcraft.neoforge.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.particle.InkExplosionParticleOptions;
import net.splatcraft.neoforge.particle.InkSplashParticleOptions;
import net.splatcraft.neoforge.particle.InkTerrainParticleOptions;
import net.splatcraft.neoforge.particle.SquidSoulParticleOptions;

public final class SplatcraftParticleTypes {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Splatcraft.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<InkSplashParticleOptions>> INK_SPLASH =
            particle("ink_splash", InkSplashParticleOptions.CODEC, InkSplashParticleOptions.STREAM_CODEC);
    public static final DeferredHolder<ParticleType<?>, ParticleType<InkExplosionParticleOptions>> INK_EXPLOSION =
            particle("ink_explosion", InkExplosionParticleOptions.CODEC, InkExplosionParticleOptions.STREAM_CODEC);
    public static final DeferredHolder<ParticleType<?>, ParticleType<SquidSoulParticleOptions>> SQUID_SOUL =
            particle("squid_soul", SquidSoulParticleOptions.CODEC, SquidSoulParticleOptions.STREAM_CODEC);
    public static final DeferredHolder<ParticleType<?>, ParticleType<InkTerrainParticleOptions>> INK_TERRAIN =
            particle("ink_terrain", InkTerrainParticleOptions.CODEC, InkTerrainParticleOptions.STREAM_CODEC);

    private SplatcraftParticleTypes() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    private static <T extends ParticleOptions> DeferredHolder<ParticleType<?>, ParticleType<T>> particle(
            String name,
            MapCodec<T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
    ) {
        return PARTICLE_TYPES.register(name, () -> new ParticleType<T>(false) {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        });
    }
}
