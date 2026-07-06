package net.splatcraft.neoforge.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.splatcraft.neoforge.network.payload.DodgeRollPayload;
import net.splatcraft.neoforge.network.payload.GameRulesPayload;
import net.splatcraft.neoforge.network.payload.PlayerColorsPayload;
import net.splatcraft.neoforge.network.payload.PlayerSetSquidPayload;
import net.splatcraft.neoforge.network.payload.PlayerSkinOverlayPayload;
import net.splatcraft.neoforge.network.payload.PlayerSkinOverlayUploadPayload;
import net.splatcraft.neoforge.network.payload.StageListPayload;
import net.splatcraft.neoforge.network.payload.UpdateBlockColorPayload;
import net.splatcraft.neoforge.network.payload.UseSubWeaponPayload;
import net.splatcraft.neoforge.network.payload.WeaponSettingsPayload;
import net.splatcraft.neoforge.network.payload.WorldInkPayload;

public final class SplatcraftNetwork {
    public static final String NETWORK_VERSION = "1";

    private SplatcraftNetwork() {
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToServer(UpdateBlockColorPayload.TYPE, UpdateBlockColorPayload.STREAM_CODEC, UpdateBlockColorPayload::handle)
                .playToServer(PlayerSetSquidPayload.TYPE, PlayerSetSquidPayload.STREAM_CODEC, PlayerSetSquidPayload::handle)
                .playToServer(UseSubWeaponPayload.TYPE, UseSubWeaponPayload.STREAM_CODEC, UseSubWeaponPayload::handle)
                .playToServer(DodgeRollPayload.TYPE, DodgeRollPayload.STREAM_CODEC, DodgeRollPayload::handle)
                .playToServer(PlayerSkinOverlayUploadPayload.TYPE, PlayerSkinOverlayUploadPayload.STREAM_CODEC, PlayerSkinOverlayUploadPayload::handle)
                .playToClient(GameRulesPayload.TYPE, GameRulesPayload.STREAM_CODEC, GameRulesPayload::handle)
                .playToClient(PlayerColorsPayload.TYPE, PlayerColorsPayload.STREAM_CODEC, PlayerColorsPayload::handle)
                .playToClient(WeaponSettingsPayload.TYPE, WeaponSettingsPayload.STREAM_CODEC, WeaponSettingsPayload::handle)
                .playToClient(StageListPayload.TYPE, StageListPayload.STREAM_CODEC, StageListPayload::handle)
                .playToClient(WorldInkPayload.TYPE, WorldInkPayload.STREAM_CODEC, WorldInkPayload::handle)
                .playToClient(PlayerSkinOverlayPayload.TYPE, PlayerSkinOverlayPayload.STREAM_CODEC, PlayerSkinOverlayPayload::handle);
    }
}
