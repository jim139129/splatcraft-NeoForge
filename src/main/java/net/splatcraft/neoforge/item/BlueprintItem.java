package net.splatcraft.neoforge.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.recipe.WeaponWorkbenchRecipe;
import net.splatcraft.neoforge.registry.SplatcraftRecipeSerializers;

public class BlueprintItem extends Item {
    private static final String ADVANCEMENTS_KEY = "Advancements";
    private static final String POOLS_KEY = "Pools";
    private static final String HIDE_TOOLTIP_KEY = "HideTooltip";

    private static final Map<String, String> POOL_TABS = Map.of(
            "shooters", "shooters",
            "blasters", "blasters",
            "rollers", "rollers",
            "chargers", "chargers",
            "sloshers", "buckets",
            "splatlings", "splatlings",
            "dualies", "dualies",
            "sub_weapons", "sub_weapons",
            "ink_tanks", "ink_tanks");

    public BlueprintItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag tag = customData(stack);
        if (tag.getBoolean(HIDE_TOOLTIP_KEY)) {
            return;
        }

        if (tag.contains(ADVANCEMENTS_KEY, Tag.TAG_LIST)) {
            tooltip.add(Component.translatable("item.splatcraft.blueprint.tooltip"));
            return;
        }

        if (tag.contains(POOLS_KEY, Tag.TAG_LIST)) {
            tooltip.add(Component.translatable("item.splatcraft.blueprint.tooltip"));
            for (String pool : stringList(tag, POOLS_KEY)) {
                tooltip.add(Component.translatable("item.splatcraft.blueprint.tooltip." + pool)
                        .withStyle(ChatFormatting.BLUE));
            }
            return;
        }

        tooltip.add(Component.translatable("item.splatcraft.blueprint.tooltip.empty"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return super.use(level, player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);
        List<AdvancementHolder> pool = advancementPool(level, stack);
        int count = pool.size();
        if (count <= 0) {
            player.displayClientMessage(Component.translatable("status.blueprint.invalid"), true);
            return super.use(level, player, hand);
        }

        pool.removeIf(advancement -> serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone());
        if (pool.isEmpty()) {
            player.displayClientMessage(Component.translatable("status.blueprint.already_unlocked" + (count > 1 ? "" : ".single")), true);
            return super.use(level, player, hand);
        }

        AdvancementHolder advancement = pool.get(level.random.nextInt(pool.size()));
        AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
        List<String> criteria = new ArrayList<>();
        progress.getRemainingCriteria().forEach(criteria::add);
        for (String criterion : criteria) {
            serverPlayer.getAdvancements().award(advancement, criterion);
        }

        advancement.value().display().ifPresent(display -> {
            if (!display.shouldShowToast()) {
                player.displayClientMessage(Component.translatable("status.blueprint.unlock", display.getTitle()), true);
            }
        });

        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    public static ItemStack addToAdvancementPool(ItemStack blueprint, Iterable<String> advancementIds) {
        CustomData.update(DataComponents.CUSTOM_DATA, blueprint, tag -> {
            ListTag pool = tag.contains(ADVANCEMENTS_KEY, Tag.TAG_LIST)
                    ? tag.getList(ADVANCEMENTS_KEY, Tag.TAG_STRING)
                    : new ListTag();
            for (String advancementId : advancementIds) {
                pool.add(StringTag.valueOf(advancementId));
            }
            tag.put(ADVANCEMENTS_KEY, pool);
        });
        return blueprint;
    }

    public static ItemStack setPoolFromWeaponType(ItemStack blueprint, String weaponType) {
        if (weaponType == null || weaponType.isBlank() || !isKnownPool(weaponType)) {
            return blueprint;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, blueprint, tag -> {
            ListTag pools = tag.contains(POOLS_KEY, Tag.TAG_LIST)
                    ? tag.getList(POOLS_KEY, Tag.TAG_STRING)
                    : new ListTag();
            pools.add(StringTag.valueOf(weaponType));
            tag.put(POOLS_KEY, pools);
        });
        return blueprint;
    }

    public static List<AdvancementHolder> advancementPool(Level level, ItemStack blueprint) {
        if (level.getServer() == null) {
            return List.of();
        }

        CompoundTag tag = customData(blueprint);
        List<AdvancementHolder> output = new ArrayList<>();

        for (String id : stringList(tag, ADVANCEMENTS_KEY)) {
            AdvancementHolder advancement = advancement(level, id);
            if (advancement != null) {
                output.add(advancement);
            }
        }

        Set<ResourceLocation> seen = new HashSet<>();
        for (AdvancementHolder advancement : output) {
            seen.add(advancement.id());
        }
        for (String pool : stringList(tag, POOLS_KEY)) {
            for (ResourceLocation advancementId : advancementsForPool(level, pool)) {
                if (seen.add(advancementId)) {
                    AdvancementHolder advancement = level.getServer().getAdvancements().get(advancementId);
                    if (advancement != null) {
                        output.add(advancement);
                    }
                }
            }
        }

        return output;
    }

    private static List<ResourceLocation> advancementsForPool(Level level, String pool) {
        return level.getRecipeManager().getAllRecipesFor(SplatcraftRecipeSerializers.WEAPON_WORKBENCH_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> poolMatches(pool, recipe))
                .flatMap(recipe -> recipe.recipes().stream())
                .filter(subtype -> isBlueprintPoolResult(subtype.result()))
                .map(WeaponWorkbenchRecipe.Subtype::advancement)
                .flatMap(Optional::stream)
                .toList();
    }

    private static boolean isBlueprintPoolResult(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(SplatcraftTags.Items.EXCLUDED_FROM_BLUEPRINT_POOL)
                && (stack.is(SplatcraftTags.Items.MAIN_WEAPONS)
                || stack.is(SplatcraftTags.Items.SUB_WEAPONS)
                || stack.is(SplatcraftTags.Items.INK_TANKS));
    }

    private static boolean poolMatches(String pool, WeaponWorkbenchRecipe recipe) {
        if ("wildcard".equals(pool)) {
            return true;
        }

        String tab = POOL_TABS.get(pool);
        return tab != null && recipe.tab().equals(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "weapon_workbench_tabs/" + tab));
    }

    private static boolean isKnownPool(String pool) {
        return "wildcard".equals(pool) || POOL_TABS.containsKey(pool);
    }

    private static AdvancementHolder advancement(Level level, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? null : level.getServer().getAdvancements().get(location);
    }

    private static List<String> stringList(CompoundTag tag, String key) {
        List<String> output = new ArrayList<>();
        if (tag.contains(key, Tag.TAG_LIST)) {
            for (Tag entry : tag.getList(key, Tag.TAG_STRING)) {
                output.add(entry.getAsString());
            }
        }
        return output;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
