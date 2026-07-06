package net.splatcraft.neoforge.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

final class ComponentSerialization {
    static final Codec<Component> CODEC = net.minecraft.network.chat.ComponentSerialization.CODEC;

    static final Codec<ComponentHolder> HOLDER_CODEC = Codec.either(Codec.STRING, CODEC)
            .xmap(value -> value.map(ComponentHolder::translation, ComponentHolder::literal),
                    holder -> holder.translationKey.isEmpty()
                            ? com.mojang.datafixers.util.Either.right(holder.component)
                            : com.mojang.datafixers.util.Either.left(holder.translationKey));

    static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC;

    static final StreamCodec<RegistryFriendlyByteBuf, ComponentHolder> HOLDER_STREAM_CODEC = StreamCodec.of(
            (buffer, holder) -> {
                buffer.writeUtf(holder.translationKey);
                STREAM_CODEC.encode(buffer, holder.component);
            },
            buffer -> new ComponentHolder(buffer.readUtf(), STREAM_CODEC.decode(buffer))
    );

    private ComponentSerialization() {
    }

    record ComponentHolder(String translationKey, Component component) {
        static ComponentHolder empty() {
            return new ComponentHolder("", Component.empty());
        }

        static ComponentHolder translation(String translationKey) {
            return new ComponentHolder(translationKey, Component.translatable(translationKey));
        }

        static ComponentHolder literal(Component component) {
            return new ComponentHolder("", component);
        }
    }
}
