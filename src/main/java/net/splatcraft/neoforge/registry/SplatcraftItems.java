package net.splatcraft.neoforge.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.dispenser.PlaceBlockDispenseBehavior;
import net.splatcraft.neoforge.entity.sub.AbstractSubWeaponEntity;
import net.splatcraft.neoforge.item.BlueprintItem;
import net.splatcraft.neoforge.item.ColorChangerItem;
import net.splatcraft.neoforge.item.ColoredArmorItem;
import net.splatcraft.neoforge.item.ColoredBlockItem;
import net.splatcraft.neoforge.item.FilterItem;
import net.splatcraft.neoforge.item.InkBandItem;
import net.splatcraft.neoforge.item.InkDisruptorItem;
import net.splatcraft.neoforge.item.InkTankItem;
import net.splatcraft.neoforge.item.PowerEggCanItem;
import net.splatcraft.neoforge.item.SubWeaponItem;
import net.splatcraft.neoforge.item.SquidBumperItem;
import net.splatcraft.neoforge.item.TurfScannerItem;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.item.WeaponItem.WeaponClass;
import net.splatcraft.neoforge.item.WaxApplicatorItem;

public final class SplatcraftItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Splatcraft.MOD_ID);

    private static final List<DeferredItem<? extends Item>> GENERAL_TAB_ITEMS = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> WEAPON_TAB_ITEMS = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> ALL_WEAPON_ITEMS = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> COLOR_TAB_ITEMS = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> INK_COLORED_ITEMS = new ArrayList<>();
    private static final List<DeferredItem<ColoredBlockItem>> COLORED_BLOCK_ITEMS = new ArrayList<>();
    private static final List<String> BLUEPRINT_SEARCH_POOLS = List.of(
            "shooters",
            "blasters",
            "rollers",
            "chargers",
            "sloshers",
            "splatlings",
            "dualies",
            "sub_weapons",
            "ink_tanks");

    public static final DeferredItem<? extends BlockItem> SARDINIUM_BLOCK = blockItem("sardinium_block", SplatcraftBlocks.SARDINIUM_BLOCK, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> RAW_SARDINIUM_BLOCK = blockItem("raw_sardinium_block", SplatcraftBlocks.RAW_SARDINIUM_BLOCK, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> SARDINIUM_ORE = blockItem("sardinium_ore", SplatcraftBlocks.SARDINIUM_ORE, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> POWER_EGG_BLOCK = blockItem("power_egg_block", SplatcraftBlocks.POWER_EGG_BLOCK, TabList.GENERAL);
    public static final DeferredItem<ColoredBlockItem> CORALITE = coloredBlockItem("coralite", SplatcraftBlocks.CORALITE, TabList.COLOR, false, item -> item.clearsToSelf());
    public static final DeferredItem<ColoredBlockItem> CORALITE_SLAB = coloredBlockItem("coralite_slab", SplatcraftBlocks.CORALITE_SLAB, TabList.COLOR, false, item -> item.clearsToSelf());
    public static final DeferredItem<ColoredBlockItem> CORALITE_STAIRS = coloredBlockItem("coralite_stairs", SplatcraftBlocks.CORALITE_STAIRS, TabList.COLOR, false, item -> item.clearsToSelf());
    public static final DeferredItem<? extends BlockItem> INK_VAT = blockItem("ink_vat", SplatcraftBlocks.INK_VAT, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> AMMO_KNIGHTS_WORKBENCH = blockItem("ammo_knights_workbench", SplatcraftBlocks.AMMO_KNIGHTS_WORKBENCH, TabList.GENERAL);
    public static final DeferredItem<ColoredBlockItem> REMOTE_PEDESTAL = coloredBlockItem("remote_pedestal", SplatcraftBlocks.REMOTE_PEDESTAL, TabList.GENERAL, true);
    public static final DeferredItem<? extends BlockItem> EMPTY_INKWELL = blockItem("empty_inkwell", SplatcraftBlocks.EMPTY_INKWELL, TabList.GENERAL);
    public static final DeferredItem<ColoredBlockItem> INKWELL = coloredBlockItem("inkwell", SplatcraftBlocks.INKWELL, TabList.COLOR, true, new Item.Properties().stacksTo(16), item -> item.clearsTo(EMPTY_INKWELL::get).addStarterColors());
    public static final DeferredItem<ColoredBlockItem> INK_STAINED_WOOL = coloredBlockItem("ink_stained_wool", SplatcraftBlocks.INK_STAINED_WOOL, TabList.COLOR, true, item -> item.clearsTo(Items.WHITE_WOOL));
    public static final DeferredItem<ColoredBlockItem> INK_STAINED_CARPET = coloredBlockItem("ink_stained_carpet", SplatcraftBlocks.INK_STAINED_CARPET, TabList.COLOR, true, item -> item.clearsTo(Items.WHITE_CARPET));
    public static final DeferredItem<ColoredBlockItem> INK_STAINED_GLASS = coloredBlockItem("ink_stained_glass", SplatcraftBlocks.INK_STAINED_GLASS, TabList.COLOR, true, item -> item.clearsTo(Items.GLASS));
    public static final DeferredItem<ColoredBlockItem> INK_STAINED_GLASS_PANE = coloredBlockItem("ink_stained_glass_pane", SplatcraftBlocks.INK_STAINED_GLASS_PANE, TabList.COLOR, true, item -> item.clearsTo(Items.GLASS_PANE));
    public static final DeferredItem<ColoredBlockItem> CANVAS = coloredBlockItem("canvas", SplatcraftBlocks.CANVAS, TabList.COLOR, false);
    public static final DeferredItem<? extends BlockItem> SPLAT_SWITCH = blockItem("splat_switch", SplatcraftBlocks.SPLAT_SWITCH, TabList.GENERAL);
    public static final DeferredItem<ColoredBlockItem> SPAWN_PAD = coloredBlockItem("spawn_pad", SplatcraftBlocks.SPAWN_PAD, TabList.COLOR, true, new Item.Properties().stacksTo(1));
    public static final DeferredItem<? extends BlockItem> GRATE = blockItem("grate", SplatcraftBlocks.GRATE, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> GRATE_RAMP = blockItem("grate_ramp", SplatcraftBlocks.GRATE_RAMP, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> BARRIER_BAR = blockItem("barrier_bar", SplatcraftBlocks.BARRIER_BAR, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> CAUTION_BARRIER_BAR = blockItem("caution_barrier_bar", SplatcraftBlocks.CAUTION_BARRIER_BAR, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> PLATED_BARRIER_BAR = blockItem("plated_barrier_bar", SplatcraftBlocks.PLATED_BARRIER_BAR, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> TARP = blockItem("tarp", SplatcraftBlocks.TARP, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> GLASS_COVER = blockItem("glass_cover", SplatcraftBlocks.GLASS_COVER, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> CRATE = blockItem("crate", SplatcraftBlocks.CRATE, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> SUNKEN_CRATE = blockItem("sunken_crate", SplatcraftBlocks.SUNKEN_CRATE, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> AMMO_KNIGHTS_DEBRIS = blockItem("ammo_knights_debris", SplatcraftBlocks.AMMO_KNIGHTS_DEBRIS, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> STAGE_BARRIER = blockItem("stage_barrier", SplatcraftBlocks.STAGE_BARRIER, TabList.GENERAL);
    public static final DeferredItem<? extends BlockItem> STAGE_VOID = blockItem("stage_void", SplatcraftBlocks.STAGE_VOID, TabList.GENERAL);
    public static final DeferredItem<ColoredBlockItem> ALLOWED_COLOR_BARRIER = coloredBlockItem("allowed_color_barrier", SplatcraftBlocks.ALLOWED_COLOR_BARRIER, TabList.COLOR, true);
    public static final DeferredItem<ColoredBlockItem> DENIED_COLOR_BARRIER = coloredBlockItem("denied_color_barrier", SplatcraftBlocks.DENIED_COLOR_BARRIER, TabList.COLOR, true);

    public static final DeferredItem<Item> RAW_SARDINIUM = simple("raw_sardinium");
    public static final DeferredItem<Item> SARDINIUM = simple("sardinium");
    public static final DeferredItem<Item> POWER_EGG = simple("power_egg");
    public static final DeferredItem<PowerEggCanItem> POWER_EGG_CAN = powerEggCan("power_egg_can");
    public static final DeferredItem<Item> AMMO_KNIGHTS_SCRAP = simple("ammo_knights_scrap");
    public static final DeferredItem<BlueprintItem> BLUEPRINT = blueprint("blueprint");
    public static final DeferredItem<Item> TONI_KENSA_PIN = simple("toni_kensa_pin", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<FilterItem> FILTER = filter("filter");
    public static final DeferredItem<FilterItem> PASTEL_FILTER = filter("pastel_filter");
    public static final DeferredItem<FilterItem> ORGANIC_FILTER = filter("organic_filter");
    public static final DeferredItem<FilterItem> NEON_FILTER = filter("neon_filter");
    public static final DeferredItem<FilterItem> ENCHANTED_FILTER = filter("enchanted_filter", new Item.Properties().rarity(Rarity.UNCOMMON), true, false);
    public static final DeferredItem<FilterItem> OVERGROWN_FILTER = filter("overgrown_filter");
    public static final DeferredItem<FilterItem> MIDNIGHT_FILTER = filter("midnight_filter");
    public static final DeferredItem<FilterItem> CREATIVE_FILTER = filter("creative_filter", new Item.Properties().rarity(Rarity.RARE), false, true);
    public static final DeferredItem<TurfScannerItem> TURF_SCANNER = turfScanner("turf_scanner");
    public static final DeferredItem<InkDisruptorItem> INK_DISRUPTOR = inkDisruptor("ink_disruptor");
    public static final DeferredItem<ColorChangerItem> COLOR_CHANGER = colorChanger("color_changer");
    public static final DeferredItem<SquidBumperItem> SQUID_BUMPER = squidBumper("squid_bumper");
    public static final DeferredItem<InkBandItem> SPLATFEST_BAND = inkBand("splatfest_band");
    public static final DeferredItem<InkBandItem> CLEAR_INK_BAND = inkBand("clear_ink_band");
    public static final DeferredItem<WaxApplicatorItem> WAX_APPLICATOR = waxApplicator("wax_applicator");
    public static final DeferredItem<ColoredArmorItem> INK_CLOTH_HELMET = coloredArmor("ink_cloth_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ColoredArmorItem> INK_CLOTH_CHESTPLATE = coloredArmor("ink_cloth_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ColoredArmorItem> INK_CLOTH_LEGGINGS = coloredArmor("ink_cloth_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ColoredArmorItem> INK_CLOTH_BOOTS = coloredArmor("ink_cloth_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<InkTankItem> INK_TANK = inkTank("ink_tank", 100);
    public static final DeferredItem<InkTankItem> CLASSIC_INK_TANK = inkTank("classic_ink_tank", 100);
    public static final DeferredItem<InkTankItem> INK_TANK_JR = inkTank("ink_tank_jr", 110);
    public static final DeferredItem<InkTankItem> ARMORED_INK_TANK = inkTank("armored_ink_tank", 85);

    public static final DeferredItem<WeaponItem> SPLATTERSHOT = weapon("splattershot", "splattershot", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> TENTATEK_SPLATTERSHOT = weapon("tentatek_splattershot", "splattershot", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> WASABI_SPLATTERSHOT = weapon("wasabi_splattershot", "splattershot", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> ANCIENT_SPLATTERSHOT = hiddenWeapon("ancient_splattershot", "splattershot", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> SPLATTERSHOT_JR = weapon("splattershot_jr", "splattershot_jr", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> KENSA_SPLATTERSHOT_JR = weapon("kensa_splattershot_jr", "splattershot_jr", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> AEROSPRAY_MG = weapon("aerospray_mg", "aerospray", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> AEROSPRAY_RG = weapon("aerospray_rg", "aerospray", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> GAL_52 = weapon("52_gal", "52_gal", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> GAL_52_DECO = weapon("52_gal_deco", "52_gal", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> KENSA_52_GAL = weapon("kensa_52_gal", "52_gal", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> GAL_96 = weapon("96_gal", "96_gal", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> GAL_96_DECO = weapon("96_gal_deco", "96_gal", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> N_ZAP85 = weapon("n-zap85", "n-zap", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> N_ZAP89 = weapon("n-zap89", "n-zap", WeaponClass.SHOOTER);
    public static final DeferredItem<WeaponItem> JET_SQUELCHER = weapon("jet_squelcher", "jet_squelcher", WeaponClass.SHOOTER);

    public static final DeferredItem<WeaponItem> BLASTER = weapon("blaster", "blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> GRIM_BLASTER = weapon("grim_blaster", "blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> RANGE_BLASTER = weapon("range_blaster", "range_blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> GRIM_RANGE_BLASTER = weapon("grim_range_blaster", "range_blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> CLASH_BLASTER = weapon("clash_blaster", "clash_blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> CLASH_BLASTER_NEO = weapon("clash_blaster_neo", "clash_blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> LUNA_BLASTER = weapon("luna_blaster", "luna_blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> RAPID_BLASTER = weapon("rapid_blaster", "rapid_blaster", WeaponClass.BLASTER);
    public static final DeferredItem<WeaponItem> RAPID_BLASTER_PRO = weapon("rapid_blaster_pro", "rapid_blaster_pro", WeaponClass.BLASTER);

    public static final DeferredItem<WeaponItem> SPLAT_ROLLER = weapon("splat_roller", "splat_roller", WeaponClass.ROLLER);
    public static final DeferredItem<WeaponItem> KRAK_ON_SPLAT_ROLLER = weapon("krak_on_splat_roller", "splat_roller", WeaponClass.ROLLER);
    public static final DeferredItem<WeaponItem> COROCORO_SPLAT_ROLLER = weapon("corocoro_splat_roller", "splat_roller", WeaponClass.ROLLER);
    public static final DeferredItem<WeaponItem> CARBON_ROLLER = weapon("carbon_roller", "carbon_roller", WeaponClass.ROLLER);
    public static final DeferredItem<WeaponItem> DYNAMO_ROLLER = weapon("dynamo_roller", "dynamo_roller", WeaponClass.ROLLER);
    public static final DeferredItem<WeaponItem> INKBRUSH = weapon("inkbrush", "inkbrush", WeaponClass.ROLLER);
    public static final DeferredItem<WeaponItem> OCTOBRUSH = weapon("octobrush", "octobrush", WeaponClass.ROLLER);
    public static final DeferredItem<WeaponItem> KENSA_OCTOBRUSH = weapon("kensa_octobrush", "octobrush", WeaponClass.ROLLER);

    public static final DeferredItem<WeaponItem> SPLAT_CHARGER = weapon("splat_charger", "splat_charger", WeaponClass.CHARGER);
    public static final DeferredItem<WeaponItem> BENTO_SPLAT_CHARGER = weapon("bento_splat_charger", "splat_charger", WeaponClass.CHARGER);
    public static final DeferredItem<WeaponItem> KELP_SPLAT_CHARGER = weapon("kelp_splat_charger", "splat_charger", WeaponClass.CHARGER);
    public static final DeferredItem<WeaponItem> E_LITER_4K = weapon("e_liter_4k", "e_liter", WeaponClass.CHARGER);
    public static final DeferredItem<WeaponItem> E_LITER_3K = weapon("e_liter_3k", "e_liter", WeaponClass.CHARGER);
    public static final DeferredItem<WeaponItem> BAMBOOZLER_14_MK1 = weapon("bamboozler_14_mk1", "bamboozler_14", WeaponClass.CHARGER);
    public static final DeferredItem<WeaponItem> BAMBOOZLER_14_MK2 = weapon("bamboozler_14_mk2", "bamboozler_14", WeaponClass.CHARGER);
    public static final DeferredItem<WeaponItem> CLASSIC_SQUIFFER = weapon("classic_squiffer", "squiffer", WeaponClass.CHARGER);

    public static final DeferredItem<WeaponItem> SPLAT_DUALIES = weapon("splat_dualies", "splat_dualies", WeaponClass.DUALIE);
    public static final DeferredItem<WeaponItem> ENPERRY_SPLAT_DUALIES = weapon("enperry_splat_dualies", "splat_dualies", WeaponClass.DUALIE);
    public static final DeferredItem<WeaponItem> DUALIE_SQUELCHERS = weapon("dualie_squelchers", "dualie_squelchers", WeaponClass.DUALIE);
    public static final DeferredItem<WeaponItem> GLOOGA_DUALIES = weapon("glooga_dualies", "glooga_dualies", WeaponClass.DUALIE);
    public static final DeferredItem<WeaponItem> GLOOGA_DUALIES_DECO = weapon("glooga_dualies_deco", "glooga_dualies", WeaponClass.DUALIE);
    public static final DeferredItem<WeaponItem> KENSA_GLOOGA_DUALIES = weapon("kensa_glooga_dualies", "glooga_dualies", WeaponClass.DUALIE);

    public static final DeferredItem<WeaponItem> SLOSHER = weapon("slosher", "slosher", WeaponClass.SLOSHER);
    public static final DeferredItem<WeaponItem> CLASSIC_SLOSHER = weapon("classic_slosher", "slosher", WeaponClass.SLOSHER);
    public static final DeferredItem<WeaponItem> SODA_SLOSHER = weapon("soda_slosher", "slosher", WeaponClass.SLOSHER);
    public static final DeferredItem<WeaponItem> TRI_SLOSHER = weapon("tri_slosher", "tri_slosher", WeaponClass.SLOSHER);
    public static final DeferredItem<WeaponItem> EXPLOSHER = weapon("explosher", "explosher", WeaponClass.SLOSHER);

    public static final DeferredItem<WeaponItem> MINI_SPLATLING = weapon("mini_splatling", "mini_splatling", WeaponClass.SPLATLING);
    public static final DeferredItem<WeaponItem> REFURBISHED_MINI_SPLATLING = weapon("refurbished_mini_splatling", "mini_splatling", WeaponClass.SPLATLING);
    public static final DeferredItem<WeaponItem> HEAVY_SPLATLING = weapon("heavy_splatling", "heavy_splatling", WeaponClass.SPLATLING);
    public static final DeferredItem<WeaponItem> HEAVY_SPLATLING_DECO = weapon("heavy_splatling_deco", "heavy_splatling", WeaponClass.SPLATLING);
    public static final DeferredItem<WeaponItem> HEAVY_SPLATLING_REMIX = weapon("heavy_splatling_remix", "heavy_splatling", WeaponClass.SPLATLING);
    public static final DeferredItem<WeaponItem> CLASSIC_HEAVY_SPLATLING = weapon("classic_heavy_splatling", "heavy_splatling", WeaponClass.SPLATLING);
    public static final DeferredItem<WeaponItem> NAUTILUS_47 = weapon("nautilus_47", "nautilus", WeaponClass.SPLATLING);
    public static final DeferredItem<WeaponItem> NAUTILUS_79 = weapon("nautilus_79", "nautilus", WeaponClass.SPLATLING);

    public static final DeferredItem<SubWeaponItem<?>> SPLAT_BOMB = subWeapon("splat_bomb", "splat_bomb", () -> SplatcraftEntities.SPLAT_BOMB.get());
    public static final DeferredItem<SubWeaponItem<?>> SPLAT_BOMB_2 = hiddenSubWeapon("splat_bomb_2", "splat_bomb", () -> SplatcraftEntities.SPLAT_BOMB.get());
    public static final DeferredItem<SubWeaponItem<?>> BURST_BOMB = subWeapon("burst_bomb", "burst_bomb", () -> SplatcraftEntities.BURST_BOMB.get());
    public static final DeferredItem<SubWeaponItem<?>> SUCTION_BOMB = subWeapon("suction_bomb", "suction_bomb", () -> SplatcraftEntities.SUCTION_BOMB.get());
    public static final DeferredItem<SubWeaponItem<?>> CURLING_BOMB = subWeapon("curling_bomb", "curling_bomb", () -> SplatcraftEntities.CURLING_BOMB.get());

    static {
        alias("inked_wool", "ink_stained_wool");
        alias("inked_carpet", "ink_stained_carpet");
        alias("inked_glass", "ink_stained_glass");
        alias("inked_glass_pane", "ink_stained_glass_pane");
        alias("weapon_workbench", "ammo_knights_workbench");
        alias("ink_polisher", "wax_applicator");
    }

    private SplatcraftItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static void registerDispenserBehaviors(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SubWeaponItem.DispenseBehavior behavior = new SubWeaponItem.DispenseBehavior();
            DispenserBlock.registerBehavior(SPLAT_BOMB.get(), behavior);
            DispenserBlock.registerBehavior(SPLAT_BOMB_2.get(), behavior);
            DispenserBlock.registerBehavior(BURST_BOMB.get(), behavior);
            DispenserBlock.registerBehavior(SUCTION_BOMB.get(), behavior);
            DispenserBlock.registerBehavior(CURLING_BOMB.get(), behavior);

            PlaceBlockDispenseBehavior placeBlock = new PlaceBlockDispenseBehavior();
            DispenserBlock.registerBehavior(EMPTY_INKWELL.get(), placeBlock);
            DispenserBlock.registerBehavior(INKWELL.get(), placeBlock);

            registerInkColorCauldron(INK_CLOTH_HELMET.get(), true, true);
            registerInkColorCauldron(INK_CLOTH_CHESTPLATE.get(), true, true);
            registerInkColorCauldron(INK_CLOTH_LEGGINGS.get(), true, true);
            registerInkColorCauldron(INK_CLOTH_BOOTS.get(), true, true);
            registerInkColorCauldron(INK_TANK.get(), true, false);
            registerInkColorCauldron(CLASSIC_INK_TANK.get(), true, false);
            registerInkColorCauldron(INK_TANK_JR.get(), true, false);
            registerInkColorCauldron(ARMORED_INK_TANK.get(), true, false);
            ALL_WEAPON_ITEMS.forEach(item -> registerInkColorCauldron(item.get(), false, false, true));
            COLORED_BLOCK_ITEMS.forEach(item -> registerColoredBlockCauldron(item.get()));
        });
    }

    private static void registerInkColorCauldron(Item item, boolean clearColor, boolean alsoCleanVanillaDye) {
        registerInkColorCauldron(item, clearColor, alsoCleanVanillaDye, false);
    }

    private static void registerInkColorCauldron(Item item, boolean clearColor, boolean alsoCleanVanillaDye, boolean skipWhenShiftDown) {
        CauldronInteraction.WATER.map().put(item, (state, level, pos, player, hand, stack) -> {
            if (!InkColorComponent.isColorLocked(stack) || skipWhenShiftDown && player.isShiftKeyDown()) {
                return alsoCleanVanillaDye
                        ? CauldronInteraction.DYED_ITEM.interact(state, level, pos, player, hand, stack)
                        : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                InkColorComponent.setColorLocked(stack, false);
                if (clearColor) {
                    InkColorComponent.setColor(stack, -1);
                }
                if (alsoCleanVanillaDye) {
                    stack.remove(DataComponents.DYED_COLOR);
                    player.awardStat(Stats.CLEAN_ARMOR);
                } else {
                    player.awardStat(Stats.USE_CAULDRON);
                }
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    private static void registerColoredBlockCauldron(ColoredBlockItem item) {
        item.clearItem().ifPresent(clearItem -> CauldronInteraction.WATER.map().put(item, (state, level, pos, player, hand, stack) -> {
            if (clearItem == item && InkColorComponent.color(stack).isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                ItemStack clearStack = new ItemStack(clearItem, 1);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, clearStack, false));
                player.awardStat(Stats.USE_CAULDRON);
                if (!player.hasInfiniteMaterials()) {
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }));
    }

    public static ItemStack inkCoatingResult(ItemStack stack, int color) {
        Item item = stack.getItem();
        for (DeferredItem<ColoredBlockItem> holder : COLORED_BLOCK_ITEMS) {
            ColoredBlockItem colored = holder.get();
            if (colored.clearItem().filter(clearItem -> clearItem == item && clearItem != colored).isPresent()) {
                return InkColorComponent.setColorAndLock(new ItemStack(colored, stack.getCount()), color, true);
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<DeferredItem<? extends Item>> generalTabItems() {
        return Collections.unmodifiableList(GENERAL_TAB_ITEMS);
    }

    public static List<DeferredItem<? extends Item>> weaponTabItems() {
        return Collections.unmodifiableList(WEAPON_TAB_ITEMS);
    }

    public static List<DeferredItem<? extends Item>> colorTabItems() {
        return Collections.unmodifiableList(COLOR_TAB_ITEMS);
    }

    public static List<DeferredItem<? extends Item>> inkColoredItems() {
        return Collections.unmodifiableList(INK_COLORED_ITEMS);
    }

    public static Map<ResourceLocation, Integer> creativeInkColors() {
        LinkedHashMap<ResourceLocation, Integer> colors = new LinkedHashMap<>(InkColorData.builtInColors());
        colors.remove(id("color_lock_friendly"));
        colors.remove(id("color_lock_hostile"));
        return Collections.unmodifiableMap(colors);
    }

    public static void addCreativeTabItem(DeferredItem<? extends Item> item, CreativeModeTab.Output output) {
        Item stackItem = item.get();
        if (stackItem instanceof ColoredBlockItem coloredBlockItem) {
            coloredBlockItem.fillCreativeTab(output);
        } else if (stackItem instanceof BlueprintItem) {
            output.accept(BlueprintItem.setPoolFromWeaponType(new ItemStack(stackItem), "wildcard"));
        } else if (stackItem instanceof SquidBumperItem) {
            output.accept(new ItemStack(stackItem));
            output.accept(InkColorComponent.setInverted(new ItemStack(stackItem), true));
        } else {
            output.accept(stackItem);
        }
    }

    public static void addColorCreativeTabItem(DeferredItem<? extends Item> item, CreativeModeTab.Output output) {
        Item stackItem = item.get();
        for (int color : creativeInkColors().values()) {
            ItemStack stack = new ItemStack(stackItem);
            InkColorComponent.setColor(stack, color);
            InkColorComponent.setColorLocked(stack, true);
            output.accept(stack);
        }

        if (!(stackItem instanceof ColoredBlockItem coloredBlockItem) || coloredBlockItem.matchesColor()) {
            output.accept(InkColorComponent.setInverted(new ItemStack(stackItem), true));
        }
    }

    public static void addCreativeTabSearchItems(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CreativeModeTabs.SEARCH)) {
            return;
        }

        for (String pool : BLUEPRINT_SEARCH_POOLS) {
            event.accept(
                    BlueprintItem.setPoolFromWeaponType(new ItemStack(BLUEPRINT.get()), pool),
                    CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }

        for (DeferredItem<? extends Item> item : WEAPON_TAB_ITEMS) {
            Item stackItem = item.get();
            if (stackItem instanceof SubWeaponItem<?>) {
                event.accept(
                        SubWeaponItem.setSingleUse(new ItemStack(stackItem)),
                        CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
            }
        }
    }

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<?> block, TabList tabList) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(name, block);
        track(item, tabList);
        return item;
    }

    private static DeferredItem<ColoredBlockItem> coloredBlockItem(String name, DeferredBlock<?> block, TabList tabList, boolean matchColor) {
        return coloredBlockItem(name, block, tabList, matchColor, new Item.Properties());
    }

    private static DeferredItem<ColoredBlockItem> coloredBlockItem(
            String name,
            DeferredBlock<?> block,
            TabList tabList,
            boolean matchColor,
            UnaryOperator<ColoredBlockItem> setup
    ) {
        return coloredBlockItem(name, block, tabList, matchColor, new Item.Properties(), setup);
    }

    private static DeferredItem<ColoredBlockItem> coloredBlockItem(
            String name,
            DeferredBlock<?> block,
            TabList tabList,
            boolean matchColor,
            Item.Properties properties
    ) {
        return coloredBlockItem(name, block, tabList, matchColor, properties, UnaryOperator.identity());
    }

    private static DeferredItem<ColoredBlockItem> coloredBlockItem(
            String name,
            DeferredBlock<?> block,
            TabList tabList,
            boolean matchColor,
            Item.Properties properties,
            UnaryOperator<ColoredBlockItem> setup
    ) {
        DeferredItem<ColoredBlockItem> item = ITEMS.register(
                name,
                () -> setup.apply(new ColoredBlockItem(block.get(), properties, matchColor)));
        COLORED_BLOCK_ITEMS.add(item);
        INK_COLORED_ITEMS.add(item);
        track(item, tabList);
        return item;
    }

    private static DeferredItem<Item> simple(String name) {
        return simple(name, new Item.Properties());
    }

    private static DeferredItem<Item> simple(String name, Item.Properties properties) {
        DeferredItem<Item> item = ITEMS.registerItem(name, Item::new, properties);
        GENERAL_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<InkTankItem> inkTank(String name, int capacity) {
        DeferredItem<InkTankItem> item = ITEMS.register(
                name,
                () -> new InkTankItem(capacity, name, inkTankMaterial(name), new Item.Properties()));
        INK_COLORED_ITEMS.add(item);
        WEAPON_TAB_ITEMS.add(item);
        return item;
    }

    private static Holder<ArmorMaterial> inkTankMaterial(String name) {
        return switch (name) {
            case "classic_ink_tank" -> SplatcraftArmorMaterials.CLASSIC_INK_TANK;
            case "ink_tank_jr" -> SplatcraftArmorMaterials.INK_TANK_JR;
            case "armored_ink_tank" -> SplatcraftArmorMaterials.ARMORED_INK_TANK;
            default -> SplatcraftArmorMaterials.INK_TANK;
        };
    }

    private static DeferredItem<SquidBumperItem> squidBumper(String name) {
        DeferredItem<SquidBumperItem> item = ITEMS.register(name, () -> new SquidBumperItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        COLOR_TAB_ITEMS.add(item);
        INK_COLORED_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<PowerEggCanItem> powerEggCan(String name) {
        DeferredItem<PowerEggCanItem> item = ITEMS.register(name, () -> new PowerEggCanItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<BlueprintItem> blueprint(String name) {
        DeferredItem<BlueprintItem> item = ITEMS.register(name, () -> new BlueprintItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<FilterItem> filter(String name) {
        return filter(name, new Item.Properties(), false, false);
    }

    private static DeferredItem<FilterItem> filter(String name, Item.Properties properties, boolean glowing, boolean omni) {
        DeferredItem<FilterItem> item = ITEMS.register(name, () -> new FilterItem(properties, glowing, omni));
        GENERAL_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<InkBandItem> inkBand(String name) {
        DeferredItem<InkBandItem> item = ITEMS.register(name, () -> new InkBandItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        INK_COLORED_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<WaxApplicatorItem> waxApplicator(String name) {
        DeferredItem<WaxApplicatorItem> item = ITEMS.register(name, () -> new WaxApplicatorItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<ColoredArmorItem> coloredArmor(String name, ArmorItem.Type type) {
        DeferredItem<ColoredArmorItem> item = ITEMS.register(
                name,
                () -> new ColoredArmorItem(SplatcraftArmorMaterials.INK_CLOTH, type, new Item.Properties()));
        INK_COLORED_ITEMS.add(item);
        WEAPON_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<InkDisruptorItem> inkDisruptor(String name) {
        DeferredItem<InkDisruptorItem> item = ITEMS.register(name, () -> new InkDisruptorItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<ColorChangerItem> colorChanger(String name) {
        DeferredItem<ColorChangerItem> item = ITEMS.register(name, () -> new ColorChangerItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        INK_COLORED_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<TurfScannerItem> turfScanner(String name) {
        DeferredItem<TurfScannerItem> item = ITEMS.register(name, () -> new TurfScannerItem(new Item.Properties()));
        GENERAL_TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<WeaponItem> weapon(String name, String settingsId, WeaponClass weaponClass) {
        DeferredItem<WeaponItem> item = ITEMS.register(name, () -> new WeaponItem(settingsId, weaponClass, new Item.Properties()));
        trackWeapon(item, true);
        return item;
    }

    private static DeferredItem<WeaponItem> hiddenWeapon(String name, String settingsId, WeaponClass weaponClass) {
        DeferredItem<WeaponItem> item = ITEMS.register(name, () -> new WeaponItem(settingsId, weaponClass, new Item.Properties()));
        trackWeapon(item, false);
        return item;
    }

    private static <T extends AbstractSubWeaponEntity> DeferredItem<SubWeaponItem<?>> subWeapon(
            String name,
            String settingsId,
            Supplier<EntityType<T>> entityType
    ) {
        DeferredItem<SubWeaponItem<?>> item = ITEMS.register(name, () -> new SubWeaponItem<>(settingsId, entityType, new Item.Properties()));
        trackWeapon(item, true);
        return item;
    }

    private static <T extends AbstractSubWeaponEntity> DeferredItem<SubWeaponItem<?>> hiddenSubWeapon(
            String name,
            String settingsId,
            Supplier<EntityType<T>> entityType
    ) {
        DeferredItem<SubWeaponItem<?>> item = ITEMS.register(name, () -> new SubWeaponItem<>(settingsId, entityType, new Item.Properties()));
        trackWeapon(item, false);
        return item;
    }

    private static void trackWeapon(DeferredItem<? extends Item> item, boolean visible) {
        ALL_WEAPON_ITEMS.add(item);
        INK_COLORED_ITEMS.add(item);
        if (visible) {
            WEAPON_TAB_ITEMS.add(item);
        }
    }

    private static void track(DeferredItem<? extends Item> item, TabList tabList) {
        switch (tabList) {
            case GENERAL -> GENERAL_TAB_ITEMS.add(item);
            case WEAPON -> WEAPON_TAB_ITEMS.add(item);
            case COLOR -> COLOR_TAB_ITEMS.add(item);
        }
    }

    private static void alias(String fromPath, String toPath) {
        ITEMS.addAlias(id(fromPath), id(toPath));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, path);
    }

    private enum TabList {
        GENERAL,
        WEAPON,
        COLOR
    }
}
