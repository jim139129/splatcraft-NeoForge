package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.menu.InkVatMenu;
import net.splatcraft.neoforge.menu.WeaponWorkbenchMenu;

public final class SplatcraftMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Splatcraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<InkVatMenu>> INK_VAT =
            MENU_TYPES.register("ink_vat", () -> IMenuTypeExtension.create(InkVatMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<WeaponWorkbenchMenu>> WEAPON_WORKBENCH =
            MENU_TYPES.register("weapon_workbench", () -> IMenuTypeExtension.create(WeaponWorkbenchMenu::new));

    private SplatcraftMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
