package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorComponent;

public final class SplatcraftCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Splatcraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERAL = CREATIVE_TABS.register("general", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.splatcraft_general"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> SplatcraftItems.SARDINIUM.get().getDefaultInstance())
            .displayItems((parameters, output) -> SplatcraftItems.generalTabItems().forEach(item -> SplatcraftItems.addCreativeTabItem(item, output)))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS = CREATIVE_TABS.register("weapons", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.splatcraft_weapons"))
            .withTabsBefore(GENERAL.getKey())
            .icon(() -> SplatcraftItems.SPLATTERSHOT.get().getDefaultInstance())
            .displayItems((parameters, output) -> SplatcraftItems.weaponTabItems().forEach(item -> SplatcraftItems.addCreativeTabItem(item, output)))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COLORS = CREATIVE_TABS.register("colors", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.splatcraft_colors"))
            .withTabsBefore(WEAPONS.getKey())
            .icon(() -> InkColorComponent.setColorAndLock(new ItemStack(SplatcraftItems.INKWELL.get()), 0xDF641A, true))
            .withSearchBar()
            .displayItems((parameters, output) -> SplatcraftItems.colorTabItems().forEach(item -> SplatcraftItems.addColorCreativeTabItem(item, output)))
            .build());

    private SplatcraftCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
