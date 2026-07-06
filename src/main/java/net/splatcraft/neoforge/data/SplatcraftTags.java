package net.splatcraft.neoforge.data;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.item.InkTankItem;

public final class SplatcraftTags {
    private SplatcraftTags() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, path);
    }

    public static final class Blocks {
        public static final TagKey<Block> BARRIER_BARS = create("barrier_bars");
        public static final TagKey<Block> CLEARS_INK = create("clears_ink");
        public static final TagKey<Block> DETERS_INK = create("deters_ink");
        public static final TagKey<Block> INK_PASSTHROUGH = create("ink_passthrough");
        public static final TagKey<Block> INKABLE_TRANSPARENTS = create("inkable_transparents");
        public static final TagKey<Block> INKED_BLOCKS = create("inked_blocks");
        public static final TagKey<Block> INKPROOF = create("inkproof");
        public static final TagKey<Block> RENDER_INK_AS_CUBE = create("render_ink_as_cube");
        public static final TagKey<Block> SCAN_TURF_IGNORED = create("scan_turf_ignored");
        public static final TagKey<Block> SCAN_TURF_SCORED = create("scan_turf_scored");
        public static final TagKey<Block> SQUID_PASSTHROUGH = create("squid_passthrough");

        public static final TagKey<Block> UNINKABLE_BLOCKS = INKPROOF;
        public static final TagKey<Block> BLOCKS_INK = DETERS_INK;
        public static final TagKey<Block> INK_CLEARING_BLOCKS = CLEARS_INK;
        public static final TagKey<Block> RENDER_AS_CUBE = RENDER_INK_AS_CUBE;

        private Blocks() {
        }

        private static TagKey<Block> create(String path) {
            return BlockTags.create(id(path));
        }
    }

    public static final class Items {
        public static final Map<InkTankItem, TagKey<Item>> INK_TANK_WHITELIST = new HashMap<>();
        public static final Map<InkTankItem, TagKey<Item>> INK_TANK_BLACKLIST = new HashMap<>();

        public static final TagKey<Item> ARMORED_INK_TANK_WHITELIST = create("armored_ink_tank_whitelist");
        public static final TagKey<Item> BLASTERS = create("blasters");
        public static final TagKey<Item> BRUSHES = create("brushes");
        public static final TagKey<Item> CHARGERS = create("chargers");
        public static final TagKey<Item> CLASSIC_INK_TANK_WHITELIST = create("classic_ink_tank_whitelist");
        public static final TagKey<Item> DUALIES = create("dualies");
        public static final TagKey<Item> EXCLUDED_FROM_BLUEPRINT_POOL = create("excluded_from_blueprint_pool");
        public static final TagKey<Item> FILTERS = create("filters");
        public static final TagKey<Item> INK_BANDS = create("ink_bands");
        public static final TagKey<Item> INK_TANK_JR_BLACKLIST = create("ink_tank_jr_blacklist");
        public static final TagKey<Item> INK_TANK_JR_WHITELIST = create("ink_tank_jr_whitelist");
        public static final TagKey<Item> INK_TANK_WHITELIST_TAG = create("ink_tank_whitelist");
        public static final TagKey<Item> INK_TANKS = create("ink_tanks");
        public static final TagKey<Item> INK_VAT_MATERIALS = create("ink_vat_materials");
        public static final TagKey<Item> MAIN_WEAPONS = create("main_weapons");
        public static final TagKey<Item> MATCH_ITEMS = create("match_items");
        public static final TagKey<Item> REMOTES = create("remotes");
        public static final TagKey<Item> REVEALS_BARRIERS = create("reveals_barriers");
        public static final TagKey<Item> ROLLERS = create("rollers");
        public static final TagKey<Item> SHOOTERS = create("shooters");
        public static final TagKey<Item> SLOSHERS = create("sloshers");
        public static final TagKey<Item> SPLATLINGS = create("splatlings");
        public static final TagKey<Item> SUB_WEAPONS = create("sub_weapons");

        public static final TagKey<Item> BLUEPRINT_EXCLUDED = EXCLUDED_FROM_BLUEPRINT_POOL;

        private Items() {
        }

        public static void putInkTankTags(InkTankItem tank, String name) {
            INK_TANK_WHITELIST.computeIfAbsent(tank, ignored -> create(name + "_whitelist"));
            INK_TANK_BLACKLIST.computeIfAbsent(tank, ignored -> create(name + "_blacklist"));
        }

        private static TagKey<Item> create(String path) {
            return ItemTags.create(id(path));
        }
    }

    public static final class EntityTypes {
        public static final TagKey<EntityType<?>> BYPASSES_SPAWN_SHIELD = create("bypasses_spawn_shield");
        public static final TagKey<EntityType<?>> SUB_WEAPONS = create("sub_weapons");

        private EntityTypes() {
        }

        private static TagKey<EntityType<?>> create(String path) {
            return TagKey.create(Registries.ENTITY_TYPE, id(path));
        }
    }

    public static final class InkColors {
        public static final ResourceLocation STARTER_COLORS = id("starter_colors");

        private InkColors() {
        }
    }
}
