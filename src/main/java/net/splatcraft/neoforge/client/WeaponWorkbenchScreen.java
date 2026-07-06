package net.splatcraft.neoforge.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.menu.WeaponWorkbenchMenu;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.recipe.WeaponWorkbenchRecipe;
import net.splatcraft.neoforge.recipe.WeaponWorkbenchTabRecipe;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;

public class WeaponWorkbenchScreen extends AbstractContainerScreen<WeaponWorkbenchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/gui/weapon_crafting.png");
    private static final int TAB_SIZE = 20;
    private static final int WEAPON_X = 17;
    private static final int WEAPON_Y = 34;
    private static final int SELECTED_WEAPON_X = 80;
    private static final int SELECTED_WEAPON_Y = 66;
    private static final int INGREDIENT_X = 17;
    private static final int INGREDIENT_Y = 108;
    private static final int CRAFT_X = 71;
    private static final int CRAFT_Y = 93;
    private static final int CRAFT_WIDTH = 34;
    private static final int CRAFT_HEIGHT = 12;
    private static final int RIGHT_ARROW_U = 231;
    private static final int LEFT_ARROW_U = 239;
    private static final int ARROW_V_NORMAL = 12;
    private static final int ARROW_V_HOVER = 24;
    private static final int ARROW_V_DISABLED = 36;

    private final Player player;
    private int tabPos;
    private int recipePage;
    private int typePos;
    private int subtypePos;
    private int ingredientPage;
    private int tickTime;
    private boolean craftButtonPressed;

    public WeaponWorkbenchScreen(WeaponWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
        this.imageHeight = 226;
        this.titleLabelX = 8;
        this.titleLabelY = this.imageHeight - 92;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 92;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        tickTime++;
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs = clientVisibleTabs();
        clampState(tabs);
        List<RecipeHolder<WeaponWorkbenchRecipe>> recipes = currentRecipes(tabs);
        renderWorkbenchTooltips(guiGraphics, tabs, recipes, selectedSubtype(recipes), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs = clientVisibleTabs();
        clampState(tabs);
        List<RecipeHolder<WeaponWorkbenchRecipe>> recipes = currentRecipes(tabs);
        WeaponWorkbenchRecipe.Subtype subtype = selectedSubtype(recipes);

        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 22, 4210752, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);

        renderTabs(guiGraphics, tabs, mouseX, mouseY);
        renderRecipeList(guiGraphics, recipes, mouseX, mouseY);
        renderSelectedWeapon(guiGraphics, subtype);
        renderIngredients(guiGraphics, subtype);
        renderArrows(guiGraphics, recipes, subtype, mouseX, mouseY);
        renderCraftButton(guiGraphics, recipes, subtype, mouseX, mouseY);
        renderEmptyText(guiGraphics, tabs, recipes);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs = clientVisibleTabs();
        clampState(tabs);
        List<RecipeHolder<WeaponWorkbenchRecipe>> recipes = currentRecipes(tabs);
        WeaponWorkbenchRecipe.Subtype subtype = selectedSubtype(recipes);

        int clickedTab = hoveredTab(tabs, mouseX, mouseY);
        if (clickedTab != -1 && clickedTab != tabPos) {
            tabPos = clickedTab;
            recipePage = 0;
            typePos = 0;
            subtypePos = 0;
            ingredientPage = 0;
            playButtonSound();
            return true;
        }

        int recipeIndex = hoveredRecipeIndex(recipes, mouseX, mouseY);
        if (recipeIndex != -1 && recipeIndex != typePos) {
            typePos = recipeIndex;
            subtypePos = 0;
            ingredientPage = 0;
            playButtonSound();
            return true;
        }

        if (recipes.size() > WeaponWorkbenchMenu.RECIPES_PER_PAGE) {
            int maxPage = maxRecipePage(recipes);
            if (recipePage < maxPage && isHovering(162, 36, 7, 11, mouseX, mouseY)) {
                recipePage++;
                typePos = Math.min(recipePage * WeaponWorkbenchMenu.RECIPES_PER_PAGE, recipes.size() - 1);
                subtypePos = 0;
                ingredientPage = 0;
                playButtonSound();
                return true;
            }
            if (recipePage > 0 && isHovering(7, 36, 7, 11, mouseX, mouseY)) {
                recipePage--;
                typePos = recipePage * WeaponWorkbenchMenu.RECIPES_PER_PAGE;
                subtypePos = 0;
                ingredientPage = 0;
                playButtonSound();
                return true;
            }
        }

        int subtypeCount = selectedRecipe(recipes) == null ? 0 : availableSubtypes(selectedRecipe(recipes).value()).size();
        if (subtypeCount > 1) {
            if (isHovering(126, 67, 7, 11, mouseX, mouseY) || isHovering(107, 66, 14, 14, mouseX, mouseY)) {
                subtypePos = (subtypePos + 1) % subtypeCount;
                ingredientPage = 0;
                playButtonSound();
                return true;
            }
            if (isHovering(43, 67, 7, 11, mouseX, mouseY) || isHovering(55, 66, 14, 14, mouseX, mouseY)) {
                subtypePos = (subtypePos - 1 + subtypeCount) % subtypeCount;
                ingredientPage = 0;
                playButtonSound();
                return true;
            }
        }

        if (subtype != null && subtype.ingredients().size() > WeaponWorkbenchMenu.INGREDIENTS_PER_PAGE) {
            int maxPage = maxIngredientPage(subtype);
            if (ingredientPage < maxPage && isHovering(162, 110, 7, 11, mouseX, mouseY)) {
                ingredientPage++;
                playButtonSound();
                return true;
            }
            if (ingredientPage > 0 && isHovering(7, 110, 7, 11, mouseX, mouseY)) {
                ingredientPage--;
                playButtonSound();
                return true;
            }
        }

        if (subtype != null && canCraft(subtype) && isHovering(CRAFT_X, CRAFT_Y, CRAFT_WIDTH, CRAFT_HEIGHT, mouseX, mouseY)) {
            craftButtonPressed = true;
            playButtonSound();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && craftButtonPressed) {
            craftButtonPressed = false;
            List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs = clientVisibleTabs();
            clampState(tabs);
            List<RecipeHolder<WeaponWorkbenchRecipe>> recipes = currentRecipes(tabs);
            RecipeHolder<WeaponWorkbenchRecipe> recipe = selectedRecipe(recipes);
            WeaponWorkbenchRecipe.Subtype subtype = selectedSubtype(recipes);
            if (recipe != null && subtype != null && canCraft(subtype) && isHovering(CRAFT_X, CRAFT_Y, CRAFT_WIDTH, CRAFT_HEIGHT, mouseX, mouseY)) {
                int buttonId = menu.craftButtonId(player.level(), recipe, recipe.value().recipes().indexOf(subtype));
                if (buttonId != -1 && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                }
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderTabs(GuiGraphics guiGraphics, List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs, int mouseX, int mouseY) {
        for (int index = 0; index < tabs.size(); index++) {
            int x = tabCenterX(tabs, index);
            int y = -5;
            int textureY = tabPos == index ? 8 : 28;
            guiGraphics.blit(TEXTURE, x - 10, y, 211, textureY, TAB_SIZE, TAB_SIZE);
            renderTabIcon(guiGraphics, tabs.get(index).value().icon(), x - 8, y + 2);
        }
    }

    private void renderTabIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y) {
        Item itemIcon = BuiltInRegistries.ITEM.get(icon);
        if (itemIcon != Items.AIR) {
            guiGraphics.renderItem(new ItemStack(itemIcon), x, y);
        } else {
            guiGraphics.blit(icon, x, y, 16, 16, 0, 0, 256, 256, 256, 256);
        }
    }

    private void renderRecipeList(GuiGraphics guiGraphics, List<RecipeHolder<WeaponWorkbenchRecipe>> recipes, int mouseX, int mouseY) {
        int start = recipePage * WeaponWorkbenchMenu.RECIPES_PER_PAGE;
        int end = Math.min(start + WeaponWorkbenchMenu.RECIPES_PER_PAGE, recipes.size());
        for (int index = start; index < end; index++) {
            int localIndex = index - start;
            int x = WEAPON_X + localIndex * 18;
            int y = WEAPON_Y;
            ItemStack stack = displayStack(recipes.get(index).value().getResultItem(minecraft.level.registryAccess()));
            guiGraphics.renderItem(stack, x, y);
            if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                guiGraphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            }
        }

        int selectedLocal = typePos - start;
        if (selectedLocal >= 0 && selectedLocal < WeaponWorkbenchMenu.RECIPES_PER_PAGE) {
            guiGraphics.blit(TEXTURE, 13 + selectedLocal * 18, 46, 246, 40, 8, 8);
        }
    }

    private void renderSelectedWeapon(GuiGraphics guiGraphics, WeaponWorkbenchRecipe.Subtype subtype) {
        if (subtype == null) {
            return;
        }
        ItemStack stack = displayStack(subtype.result());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(SELECTED_WEAPON_X, SELECTED_WEAPON_Y, 0);
        guiGraphics.pose().scale(1.5F, 1.5F, 1.0F);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderIngredients(GuiGraphics guiGraphics, WeaponWorkbenchRecipe.Subtype subtype) {
        if (subtype == null) {
            return;
        }

        int start = ingredientPage * WeaponWorkbenchMenu.INGREDIENTS_PER_PAGE;
        int end = Math.min(start + WeaponWorkbenchMenu.INGREDIENTS_PER_PAGE, subtype.ingredients().size());
        for (int index = start; index < end; index++) {
            int localIndex = index - start;
            int x = INGREDIENT_X + localIndex * 18;
            int y = INGREDIENT_Y;
            WeaponWorkbenchRecipe.StackedIngredient ingredient = subtype.ingredients().get(index);
            ItemStack stack = ingredient.displayStack(tickTime);
            boolean hasMaterial = menu.hasIngredients(player, List.of(ingredient));
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(font, stack, x, y, ingredient.count() == 1 ? null : String.valueOf(ingredient.count()));
            if (!hasMaterial) {
                guiGraphics.fill(x, y, x + 16, y + 16, 0x60FF0000);
            }
        }
    }

    private void renderArrows(GuiGraphics guiGraphics, List<RecipeHolder<WeaponWorkbenchRecipe>> recipes,
                              WeaponWorkbenchRecipe.Subtype subtype, int mouseX, int mouseY) {
        if (recipes.size() > WeaponWorkbenchMenu.RECIPES_PER_PAGE) {
            renderArrow(guiGraphics, 162, 36, true, recipePage < maxRecipePage(recipes), mouseX, mouseY);
            renderArrow(guiGraphics, 7, 36, false, recipePage > 0, mouseX, mouseY);
        }

        RecipeHolder<WeaponWorkbenchRecipe> recipe = selectedRecipe(recipes);
        int subtypeCount = recipe == null ? 0 : availableSubtypes(recipe.value()).size();
        renderArrow(guiGraphics, 126, 67, true, subtypeCount > 1, mouseX, mouseY);
        renderArrow(guiGraphics, 43, 67, false, subtypeCount > 1, mouseX, mouseY);

        if (subtype != null && subtype.ingredients().size() > WeaponWorkbenchMenu.INGREDIENTS_PER_PAGE) {
            renderArrow(guiGraphics, 162, 110, true, ingredientPage < maxIngredientPage(subtype), mouseX, mouseY);
            renderArrow(guiGraphics, 7, 110, false, ingredientPage > 0, mouseX, mouseY);
        }
    }

    private void renderArrow(GuiGraphics guiGraphics, int x, int y, boolean right, boolean enabled, int mouseX, int mouseY) {
        int v = !enabled ? ARROW_V_DISABLED : isHovering(x, y, 7, 11, mouseX, mouseY) ? ARROW_V_HOVER : ARROW_V_NORMAL;
        guiGraphics.blit(TEXTURE, x, y, right ? RIGHT_ARROW_U : LEFT_ARROW_U, v, 7, 11);
    }

    private void renderCraftButton(GuiGraphics guiGraphics, List<RecipeHolder<WeaponWorkbenchRecipe>> recipes,
                                   WeaponWorkbenchRecipe.Subtype subtype, int mouseX, int mouseY) {
        boolean craftable = subtype != null && canCraft(subtype);
        int v = 0;
        if (craftable && craftButtonPressed) {
            v = 12;
        } else if (craftable) {
            v = isHovering(CRAFT_X, CRAFT_Y, CRAFT_WIDTH, CRAFT_HEIGHT, mouseX, mouseY) ? 24 : 36;
        }

        guiGraphics.blit(TEXTURE, CRAFT_X, CRAFT_Y, 177, v, CRAFT_WIDTH, CRAFT_HEIGHT);
        Component craft = Component.translatable("gui.ammo_knights_workbench.craft");
        guiGraphics.drawString(font, craft, imageWidth / 2 - font.width(craft) / 2, 95, craftable ? 0xEFEFEF : 0x999999, false);
    }

    private void renderEmptyText(GuiGraphics guiGraphics, List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs,
                                 List<RecipeHolder<WeaponWorkbenchRecipe>> recipes) {
        if (!tabs.isEmpty() && !recipes.isEmpty()) {
            return;
        }
        Component emptyText = Component.translatable("gui.ammo_knights_workbench.empty");
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(emptyText, 106);
        int y = 73 - lines.size() * font.lineHeight / 2;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, imageWidth / 2 - font.width(line) / 2, y, 0xFFFFFF, false);
            y += font.lineHeight;
        }
    }

    private void renderWorkbenchTooltips(GuiGraphics guiGraphics, List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs,
                                         List<RecipeHolder<WeaponWorkbenchRecipe>> recipes,
                                         WeaponWorkbenchRecipe.Subtype subtype, int mouseX, int mouseY) {
        int tabIndex = hoveredTab(tabs, mouseX, mouseY);
        if (tabIndex != -1) {
            RecipeHolder<WeaponWorkbenchTabRecipe> tab = tabs.get(tabIndex);
            Component name = tab.value().name().equals(Component.empty())
                    ? Component.translatable("weaponTab." + tab.id())
                    : tab.value().name();
            guiGraphics.renderTooltip(font, name, mouseX, mouseY);
            return;
        }

        int recipeIndex = hoveredRecipeIndex(recipes, mouseX, mouseY);
        if (recipeIndex != -1) {
            RecipeHolder<WeaponWorkbenchRecipe> recipe = recipes.get(recipeIndex);
            ItemStack stack = displayStack(recipe.value().getResultItem(minecraft.level.registryAccess()));
            guiGraphics.renderTooltip(font, recipeName(recipe, stack), mouseX, mouseY);
            return;
        }

        if (subtype == null) {
            return;
        }

        int ingredientIndex = hoveredIngredientIndex(subtype, mouseX, mouseY);
        if (ingredientIndex != -1) {
            ItemStack stack = subtype.ingredients().get(ingredientIndex).displayStack(tickTime);
            guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
            return;
        }

        if (isHovering(74, 59, 28, 28, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, displayStack(subtype.result()), mouseX, mouseY);
        }
    }

    private ItemStack displayStack(ItemStack stack) {
        ItemStack display = stack.copy();
        InkColorComponent.setColor(display, SplatcraftPlayerInfoEvents.color(player));
        return display;
    }

    private static Component recipeName(RecipeHolder<WeaponWorkbenchRecipe> recipe, ItemStack fallback) {
        String key = "weaponRecipe." + recipe.id();
        return I18n.exists(key) ? Component.translatable(key) : fallback.getHoverName();
    }

    private List<RecipeHolder<WeaponWorkbenchRecipe>> currentRecipes(List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs) {
        if (tabs.isEmpty()) {
            return List.of();
        }
        return clientRecipesForTab(tabs.get(tabPos).id());
    }

    private RecipeHolder<WeaponWorkbenchRecipe> selectedRecipe(List<RecipeHolder<WeaponWorkbenchRecipe>> recipes) {
        if (recipes.isEmpty() || typePos < 0 || typePos >= recipes.size()) {
            return null;
        }
        return recipes.get(typePos);
    }

    private WeaponWorkbenchRecipe.Subtype selectedSubtype(List<RecipeHolder<WeaponWorkbenchRecipe>> recipes) {
        RecipeHolder<WeaponWorkbenchRecipe> recipe = selectedRecipe(recipes);
        if (recipe == null) {
            return null;
        }
        List<WeaponWorkbenchRecipe.Subtype> subtypes = availableSubtypes(recipe.value());
        if (subtypes.isEmpty()) {
            return null;
        }
        subtypePos = Math.min(subtypePos, subtypes.size() - 1);
        return subtypes.get(subtypePos);
    }

    private int hoveredTab(List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs, double mouseX, double mouseY) {
        for (int index = 0; index < tabs.size(); index++) {
            int x = tabCenterX(tabs, index) - 10;
            if (isHovering(x, -5, TAB_SIZE, TAB_SIZE, mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private int hoveredRecipeIndex(List<RecipeHolder<WeaponWorkbenchRecipe>> recipes, double mouseX, double mouseY) {
        int start = recipePage * WeaponWorkbenchMenu.RECIPES_PER_PAGE;
        int end = Math.min(start + WeaponWorkbenchMenu.RECIPES_PER_PAGE, recipes.size());
        for (int index = start; index < end; index++) {
            int localIndex = index - start;
            if (isHovering(WEAPON_X + localIndex * 18, WEAPON_Y, 16, 16, mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private int hoveredIngredientIndex(WeaponWorkbenchRecipe.Subtype subtype, double mouseX, double mouseY) {
        int start = ingredientPage * WeaponWorkbenchMenu.INGREDIENTS_PER_PAGE;
        int end = Math.min(start + WeaponWorkbenchMenu.INGREDIENTS_PER_PAGE, subtype.ingredients().size());
        for (int index = start; index < end; index++) {
            int localIndex = index - start;
            if (isHovering(INGREDIENT_X + localIndex * 18, INGREDIENT_Y, 16, 16, mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private int tabCenterX(List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs, int index) {
        return imageWidth / 2 - (tabs.size() - 1) * 11 + index * 22;
    }

    private boolean canCraft(WeaponWorkbenchRecipe.Subtype subtype) {
        return menu.hasIngredients(player, subtype.ingredients());
    }

    private List<WeaponWorkbenchRecipe.Subtype> availableSubtypes(WeaponWorkbenchRecipe recipe) {
        return recipe.recipes().stream()
                .filter(subtype -> isClientAvailable(subtype, recipe.recipes()))
                .toList();
    }

    private boolean isClientAvailable(WeaponWorkbenchRecipe.Subtype subtype, List<WeaponWorkbenchRecipe.Subtype> siblings) {
        if (subtype.requiresOther()) {
            for (WeaponWorkbenchRecipe.Subtype sibling : siblings) {
                if (sibling != subtype && !isClientAvailable(sibling, List.of())) {
                    return false;
                }
            }
        }
        return subtype.advancement().isEmpty() || ClientAdvancementAccess.isDone(subtype.advancement().get());
    }

    private int maxRecipePage(List<RecipeHolder<WeaponWorkbenchRecipe>> recipes) {
        return Math.max(0, (recipes.size() - 1) / WeaponWorkbenchMenu.RECIPES_PER_PAGE);
    }

    private int maxIngredientPage(WeaponWorkbenchRecipe.Subtype subtype) {
        return Math.max(0, (subtype.ingredients().size() - 1) / WeaponWorkbenchMenu.INGREDIENTS_PER_PAGE);
    }

    private void clampState(List<RecipeHolder<WeaponWorkbenchTabRecipe>> tabs) {
        tabPos = clamp(tabPos, 0, Math.max(0, tabs.size() - 1));
        List<RecipeHolder<WeaponWorkbenchRecipe>> recipes = currentRecipes(tabs);
        recipePage = clamp(recipePage, 0, maxRecipePage(recipes));
        typePos = clamp(typePos, 0, Math.max(0, recipes.size() - 1));
        if (typePos < recipePage * WeaponWorkbenchMenu.RECIPES_PER_PAGE
                || typePos >= recipePage * WeaponWorkbenchMenu.RECIPES_PER_PAGE + WeaponWorkbenchMenu.RECIPES_PER_PAGE) {
            recipePage = typePos / WeaponWorkbenchMenu.RECIPES_PER_PAGE;
        }
        WeaponWorkbenchRecipe.Subtype subtype = selectedSubtype(recipes);
        ingredientPage = subtype == null ? 0 : clamp(ingredientPage, 0, maxIngredientPage(subtype));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void playButtonSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private List<RecipeHolder<WeaponWorkbenchTabRecipe>> clientVisibleTabs() {
        return player.level().getRecipeManager().getAllRecipesFor(SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TAB_TYPE.get()).stream()
                .sorted(Comparator.comparingInt((RecipeHolder<WeaponWorkbenchTabRecipe> recipe) -> recipe.value().pos())
                        .thenComparing(recipe -> recipe.id().toString()))
                .filter(tab -> !tab.value().hidden() || !clientRecipesForTab(tab.id()).isEmpty())
                .toList();
    }

    private List<RecipeHolder<WeaponWorkbenchRecipe>> clientRecipesForTab(ResourceLocation tabId) {
        return player.level().getRecipeManager().getAllRecipesFor(SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TYPE.get()).stream()
                .sorted(Comparator.comparingInt((RecipeHolder<WeaponWorkbenchRecipe> recipe) -> recipe.value().pos())
                        .thenComparing(recipe -> recipe.id().toString()))
                .filter(recipe -> recipe.value().tab().equals(tabId))
                .filter(recipe -> !availableSubtypes(recipe.value()).isEmpty())
                .toList();
    }
}
