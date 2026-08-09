package net.splatcraft.neoforge.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public final class SplatcraftCapabilities {
    private SplatcraftCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        // Exposing the Ink Vat as a sided IItemHandler makes hoppers/pipes use the capability's
        // simulate-then-commit extraction instead of the raw WorldlyContainer#removeItem path.
        // The vanilla Container path calls removeItem (which consumes ingredients) before it knows
        // whether the destination can accept the inkwell, and destroys a set of ingredients on every
        // rejected pull; the capability path only commits when the inkwell is actually taken.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                SplatcraftBlockEntities.INK_VAT.get(),
                (inkVat, side) -> side == null ? null : new SidedInvWrapper(inkVat, side));
    }
}
