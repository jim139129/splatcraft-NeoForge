package net.splatcraft.neoforge.network.payload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.item.SubWeaponItem;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;

public record UseSubWeaponPayload(int slot, boolean pressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UseSubWeaponPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "use_sub_weapon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseSubWeaponPayload> STREAM_CODEC = StreamCodec.of(
            UseSubWeaponPayload::encode,
            UseSubWeaponPayload::decode
    );

    private static final Map<UUID, ActiveUse> ACTIVE_USES = new HashMap<>();

    public static void sendToServer(int slot, boolean pressed) {
        PacketDistributor.sendToServer(new UseSubWeaponPayload(slot, pressed));
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ActiveUse activeUse = ACTIVE_USES.get(player.getUUID());
        if (activeUse == null) {
            return;
        }

        SubWeaponUse use = resolveActiveUse(player, activeUse);
        if (use == null) {
            ACTIVE_USES.remove(player.getUUID());
            return;
        }

        int useTime = activeUse.useTime(player);
        if (useTime >= use.subWeapon().holdTime()) {
            ACTIVE_USES.remove(player.getUUID());
            finishUse(player, use, useTime);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_USES.remove(event.getEntity().getUUID());
    }

    public static void handle(UseSubWeaponPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleServer(payload, context));
    }

    private static void handleServer(UseSubWeaponPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || player.containerMenu != player.inventoryMenu) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (payload.slot < 0 || payload.slot >= inventory.getContainerSize()) {
            return;
        }

        ItemStack stack = inventory.getItem(payload.slot);
        if (!(stack.getItem() instanceof SubWeaponItem<?> subWeapon)) {
            return;
        }

        if (!payload.pressed) {
            ActiveUse activeUse = ACTIVE_USES.remove(player.getUUID());
            if (activeUse == null || activeUse.slot() != payload.slot) {
                return;
            }

            SubWeaponUse use = resolveActiveUse(player, activeUse);
            if (use != null) {
                finishUse(player, use, activeUse.useTime(player));
            }
            return;
        }

        if (SplatcraftPlayerInfoEvents.isSquid(player)) {
            SplatcraftPlayerInfoEvents.setSquid(player, false);
        }
        if (!subWeapon.canStartUseFromStack(player.level(), player, stack)) {
            return;
        }
        if (subWeapon.holdTime() <= 0) {
            subWeapon.useFromStack(player.level(), player, stack);
            inventory.setChanged();
            return;
        }
        ACTIVE_USES.put(player.getUUID(), new ActiveUse(payload.slot, stack.getItem(), player.level().getGameTime()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static UseSubWeaponPayload decode(RegistryFriendlyByteBuf buffer) {
        return new UseSubWeaponPayload(buffer.readVarInt(), buffer.readBoolean());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, UseSubWeaponPayload payload) {
        buffer.writeVarInt(payload.slot);
        buffer.writeBoolean(payload.pressed);
    }

    private static SubWeaponUse resolveActiveUse(ServerPlayer player, ActiveUse activeUse) {
        if (player.isSpectator() || player.containerMenu != player.inventoryMenu) {
            return null;
        }

        Inventory inventory = player.getInventory();
        if (activeUse.slot() < 0 || activeUse.slot() >= inventory.getContainerSize()) {
            return null;
        }

        ItemStack stack = inventory.getItem(activeUse.slot());
        if (stack.getItem() != activeUse.item() || !(stack.getItem() instanceof SubWeaponItem<?> subWeapon)) {
            return null;
        }
        return new SubWeaponUse(activeUse.slot(), stack, subWeapon);
    }

    private static void finishUse(ServerPlayer player, SubWeaponUse use, int useTime) {
        use.subWeapon().useFromStack(player.level(), player, use.stack(), useTime);
        player.getInventory().setChanged();
    }

    private record ActiveUse(int slot, Item item, long startTick) {
        private int useTime(ServerPlayer player) {
            long elapsed = player.level().getGameTime() - startTick;
            return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) elapsed);
        }
    }

    private record SubWeaponUse(int slot, ItemStack stack, SubWeaponItem<?> subWeapon) {
    }
}
