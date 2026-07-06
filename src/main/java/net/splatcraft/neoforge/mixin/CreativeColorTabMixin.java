package net.splatcraft.neoforge.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.registry.SplatcraftCreativeTabs;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeColorTabMixin {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private EditBox searchBox;

    @Shadow
    private float scrollOffs;

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void splatcraft$applyColorSearch(CallbackInfo callback) {
        if (selectedTab != SplatcraftCreativeTabs.COLORS.get() || this.searchBox.getValue().isEmpty()) {
            return;
        }

        CreativeModeInventoryScreen.ItemPickerMenu menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
        List<Integer> colors = splatcraft$matchingColors();
        if (colors.isEmpty()) {
            return;
        }

        menu.items.clear();
        boolean inverted = splatcraft$isInvertedSearch();
        for (Item item : SplatcraftItems.colorTabItems().stream().map(holder -> (Item) holder.get()).toList()) {
            for (int color : colors) {
                menu.items.add(splatcraft$coloredStack(item, color, inverted));
            }
        }

        this.scrollOffs = 0.0F;
        menu.scrollTo(0.0F);
    }

    @ModifyArg(
            method = "renderLabels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"
            ),
            index = 1
    )
    private Component splatcraft$colorSearchLabel(Component original) {
        return selectedTab == SplatcraftCreativeTabs.COLORS.get()
                ? Component.translatable("itemGroup.splatcraft_colors.label")
                : original;
    }

    @Unique
    private List<Integer> splatcraft$matchingColors() {
        SearchQuery query = splatcraft$normalizedSearch();
        if (query.value().startsWith("#")) {
            try {
                String hex = query.value().substring(1);
                return List.of(hex.isEmpty() ? 0 : Mth.clamp(Integer.parseInt(hex, 16), 0, 0xFFFFFF));
            } catch (NumberFormatException ignored) {
                return List.of();
            }
        }

        List<Integer> colors = new ArrayList<>();
        boolean exact = !query.value().isEmpty() && query.value().endsWith(".");
        String value = exact ? query.value().substring(0, query.value().length() - 1) : query.value();

        for (Map.Entry<ResourceLocation, Integer> entry : SplatcraftItems.creativeInkColors().entrySet()) {
            ResourceLocation id = entry.getKey();
            String colorName = Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath())
                    .getString()
                    .toLowerCase(Locale.ROOT);
            if (exact
                    ? id.toString().equals(value) || colorName.equals(value)
                    : id.toString().contains(value) || colorName.contains(value)) {
                colors.add(entry.getValue());
            }
        }

        if (colors.isEmpty()) {
            try {
                colors.add(Mth.clamp(Integer.parseInt(value), 0, 0xFFFFFF));
            } catch (NumberFormatException ignored) {
                // Keep vanilla search results when the query is neither a color name nor a numeric color.
            }
        }
        return colors;
    }

    @Unique
    private boolean splatcraft$isInvertedSearch() {
        return splatcraft$normalizedSearch().inverted();
    }

    @Unique
    private SearchQuery splatcraft$normalizedSearch() {
        String value = this.searchBox.getValue().toLowerCase(Locale.ROOT);
        boolean inverted = false;

        String invertedTemplate = ChatFormatting.stripFormatting(Component.translatable("ink_color.invert", "%s")
                .getString());
        if (invertedTemplate != null && !invertedTemplate.isEmpty()) {
            invertedTemplate = invertedTemplate.toLowerCase(Locale.ROOT);
            int argumentIndex = invertedTemplate.indexOf("%s");
            if (argumentIndex >= 0) {
                String prefix = invertedTemplate.substring(0, argumentIndex);
                String suffix = invertedTemplate.substring(argumentIndex + "%s".length());
                inverted = value.startsWith(prefix) && value.endsWith(suffix);
                if (inverted) {
                    value = value.substring(prefix.length(), value.length() - suffix.length());
                }
            }
        }

        if (!inverted && (value.startsWith("!") || value.startsWith("-"))) {
            value = value.substring(1);
            inverted = true;
        }
        return new SearchQuery(value, inverted);
    }

    @Unique
    private static ItemStack splatcraft$coloredStack(Item item, int color, boolean inverted) {
        ItemStack stack = new ItemStack(item);
        InkColorComponent.setColor(stack, color);
        InkColorComponent.setInverted(stack, inverted);
        InkColorComponent.setColorLocked(stack, true);
        return stack;
    }

    @Unique
    private record SearchQuery(String value, boolean inverted) {
    }
}
