package net.splatcraft.neoforge.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.menu.InkVatMenu;

public class InkVatScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<InkVatMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/gui/inkwell_crafting.png");
    private static final int COLOR_SELECTION_X = 12;
    private static final int COLOR_SELECTION_Y = 16;
    private static final int COLOR_COLUMNS = 8;
    private static final int COLOR_ROWS = 2;
    private static final int VISIBLE_COLORS = COLOR_COLUMNS * COLOR_ROWS;
    private static final int COLOR_BUTTON_WIDTH = 19;
    private static final int COLOR_BUTTON_HEIGHT = 18;
    private static final int SCROLL_BAR_X = 15;
    private static final int SCROLL_BAR_Y = 55;
    private static final int SCROLL_BAR_TRACK_WIDTH = 146;
    private static final int SCROLLER_TRAVEL = 132;
    private static final int SCROLLER_WIDTH = 15;
    private static final int SCROLLER_HEIGHT = 10;

    private boolean scrolling;
    private float scrollOffs;
    private int startIndex;

    public InkVatScreen(InkVatMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 208;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 92;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        clampScroll();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderMissingSlotIcons(guiGraphics);
        renderScrollBar(guiGraphics, mouseX, mouseY);
        renderColorOptions(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleX = imageWidth / 2 - font.width(title) / 2;
        guiGraphics.drawString(font, title, titleX, titleLabelY, 4210752, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        int hoveredRecipeIndex = hoveredRecipeIndex(mouseX, mouseY);
        if (hoveredRecipeIndex != -1) {
            int color = menu.getRecipeColors().get(hoveredRecipeIndex);
            guiGraphics.renderTooltip(font, colorName(color), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        if (button == 0 && menu.hasColorOptions()) {
            int recipeIndex = hoveredRecipeIndex(mouseX, mouseY);
            if (recipeIndex != -1 && minecraft != null && minecraft.player != null && menu.clickMenuButton(minecraft.player, recipeIndex)) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, recipeIndex);
                }
                return true;
            }

            if (isHovering(SCROLL_BAR_X, SCROLL_BAR_Y, SCROLL_BAR_TRACK_WIDTH, SCROLLER_HEIGHT, mouseX, mouseY) && isScrollBarActive()) {
                scrolling = true;
                setScrollFromMouse(mouseX);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            scrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling && isScrollBarActive()) {
            setScrollFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isScrollBarActive()) {
            int offscreenColumns = getOffscreenColumns();
            scrollOffs = Mth.clamp(scrollOffs - (float) scrollY / (float) offscreenColumns, 0.0F, 1.0F);
            updateStartIndex();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderMissingSlotIcons(GuiGraphics guiGraphics) {
        if (menu.getSlot(0).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE, leftPos + 26, topPos + 70, 176, 0, 16, 16);
        }
        if (menu.getSlot(1).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE, leftPos + 46, topPos + 70, 192, 0, 16, 16);
        }
        if (menu.getSlot(2).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE, leftPos + 92, topPos + 82, 208, 0, 16, 16);
        }
        if (menu.getSlot(3).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE, leftPos + 36, topPos + 89, 224, 0, 16, 16);
        }
    }

    private void renderColorOptions(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        clampScroll();
        List<Integer> sortedIndexes = menu.getSortedRecipeIndexes();
        int lastVisibleIndex = Math.min(startIndex + VISIBLE_COLORS, sortedIndexes.size());
        for (int sortedIndex = startIndex; sortedIndex < lastVisibleIndex; sortedIndex++) {
            int localIndex = sortedIndex - startIndex;
            int recipeIndex = sortedIndexes.get(sortedIndex);
            int color = menu.getRecipeColors().get(recipeIndex);
            int x = leftPos + COLOR_SELECTION_X + localIndex / COLOR_ROWS * COLOR_BUTTON_WIDTH;
            int y = topPos + COLOR_SELECTION_Y + localIndex % COLOR_ROWS * COLOR_BUTTON_HEIGHT;
            float red = (float) (color >> 16 & 255) / 255.0F;
            float green = (float) (color >> 8 & 255) / 255.0F;
            float blue = (float) (color & 255) / 255.0F;

            guiGraphics.setColor(red, green, blue, 1.0F);
            guiGraphics.blit(TEXTURE, x, y, 34, 220, COLOR_BUTTON_WIDTH, COLOR_BUTTON_HEIGHT);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (menu.getSelectedRecipeIndex() == recipeIndex) {
                guiGraphics.blit(TEXTURE, x, y, 34, 238, COLOR_BUTTON_WIDTH, COLOR_BUTTON_HEIGHT);
            } else if (mouseX >= x && mouseY >= y && mouseX < x + COLOR_BUTTON_WIDTH && mouseY < y + COLOR_BUTTON_HEIGHT) {
                guiGraphics.fill(x + 1, y + 1, x + COLOR_BUTTON_WIDTH - 1, y + COLOR_BUTTON_HEIGHT - 1, 0x40FFFFFF);
            }
        }
    }

    private void renderScrollBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos + SCROLL_BAR_X + (int) (SCROLLER_TRAVEL * scrollOffs);
        int y = topPos + SCROLL_BAR_Y;
        if (!isScrollBarActive()) {
            guiGraphics.blit(TEXTURE, leftPos + SCROLL_BAR_X, y, 241, 10, SCROLLER_WIDTH, SCROLLER_HEIGHT);
            return;
        }

        int textureY = isHovering(SCROLL_BAR_X, SCROLL_BAR_Y, SCROLL_BAR_TRACK_WIDTH, SCROLLER_HEIGHT, mouseX, mouseY) || scrolling ? 20 : 0;
        guiGraphics.blit(TEXTURE, x, y, 241, textureY, SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }

    private int hoveredRecipeIndex(double mouseX, double mouseY) {
        clampScroll();
        List<Integer> sortedIndexes = menu.getSortedRecipeIndexes();
        int lastVisibleIndex = Math.min(startIndex + VISIBLE_COLORS, sortedIndexes.size());
        for (int sortedIndex = startIndex; sortedIndex < lastVisibleIndex; sortedIndex++) {
            int localIndex = sortedIndex - startIndex;
            int x = COLOR_SELECTION_X + localIndex / COLOR_ROWS * COLOR_BUTTON_WIDTH;
            int y = COLOR_SELECTION_Y + localIndex % COLOR_ROWS * COLOR_BUTTON_HEIGHT;
            if (isHovering(x, y, COLOR_BUTTON_WIDTH, COLOR_BUTTON_HEIGHT, mouseX, mouseY)) {
                return sortedIndexes.get(sortedIndex);
            }
        }
        return -1;
    }

    private void setScrollFromMouse(double mouseX) {
        scrollOffs = Mth.clamp((float) (mouseX - leftPos - SCROLL_BAR_X) / (float) SCROLLER_TRAVEL, 0.0F, 1.0F);
        updateStartIndex();
    }

    private void updateStartIndex() {
        startIndex = Math.round(scrollOffs * getOffscreenColumns()) * COLOR_ROWS;
        int maxStartIndex = Math.max(0, menu.getSortedRecipeIndexes().size() - VISIBLE_COLORS);
        if (startIndex > maxStartIndex) {
            startIndex = maxStartIndex - maxStartIndex % COLOR_ROWS;
        }
    }

    private boolean isScrollBarActive() {
        return menu.getSortedRecipeIndexes().size() > VISIBLE_COLORS;
    }

    private int getOffscreenColumns() {
        return Math.max(1, (menu.getSortedRecipeIndexes().size() + COLOR_ROWS - 1) / COLOR_ROWS - COLOR_COLUMNS);
    }

    private void clampScroll() {
        if (!isScrollBarActive()) {
            scrollOffs = 0.0F;
            startIndex = 0;
        } else {
            updateStartIndex();
        }
    }

    private static Component colorName(int color) {
        return InkColorData.builtInId(color)
                .<Component>map(id -> Component.translatable("ink_color." + id.getNamespace() + "." + id.getPath()))
                .orElseGet(() -> Component.literal(String.format("#%06X", color)));
    }
}
