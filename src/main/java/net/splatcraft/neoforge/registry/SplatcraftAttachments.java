package net.splatcraft.neoforge.registry;

import java.util.function.Supplier;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkOverlayInfo;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.worldink.WorldInk;

public final class SplatcraftAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Splatcraft.MOD_ID);
    private static final Supplier<PlayerInfo> PLAYER_INFO_FACTORY = PlayerInfo::new;

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerInfo>> PLAYER_INFO =
            ATTACHMENTS.register(
                    "player_info",
                    () -> AttachmentType.serializable(PLAYER_INFO_FACTORY)
                            .copyOnDeath()
                            .sync(PlayerInfo.STREAM_CODEC)
                            .build()
            );
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<WorldInk>> WORLD_INK =
            ATTACHMENTS.register(
                    "world_ink",
                    () -> AttachmentType.serializable(holder -> new WorldInk((ChunkAccess) holder))
                            .build()
            );
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<InkOverlayInfo>> INK_OVERLAY =
            ATTACHMENTS.register(
                    "ink_overlay",
                    () -> AttachmentType.serializable(InkOverlayInfo::new)
                            .sync(InkOverlayInfo.STREAM_CODEC)
                            .build()
            );

    private SplatcraftAttachments() {
    }

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}
