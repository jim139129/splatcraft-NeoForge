package net.splatcraft.neoforge.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.block.SplatcraftBlockProperties;

public final class SplatcraftBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Splatcraft.MOD_ID);

    public static final DeferredBlock<Block> INKED_BLOCK = BLOCKS.register("inked_block", () -> new BlockStateCompatBlocks.InkedBlockEntityBlock(SplatcraftBlockProperties.inked().noLootTable()));
    public static final DeferredBlock<Block> GLOWING_INKED_BLOCK = BLOCKS.register("glowing_inked_block", () -> new BlockStateCompatBlocks.InkedBlockEntityBlock(SplatcraftBlockProperties.lightInk().noLootTable()));
    public static final DeferredBlock<Block> CLEAR_INKED_BLOCK = BLOCKS.register("clear_inked_block", () -> new BlockStateCompatBlocks.InkedBlockEntityBlock(SplatcraftBlockProperties.glass().noLootTable()));

    public static final DeferredBlock<Block> SARDINIUM_BLOCK = block("sardinium_block", SplatcraftBlockProperties.metal());
    public static final DeferredBlock<Block> RAW_SARDINIUM_BLOCK = block("raw_sardinium_block", SplatcraftBlockProperties.metal());
    public static final DeferredBlock<Block> SARDINIUM_ORE = block("sardinium_ore", SplatcraftBlockProperties.stone());
    public static final DeferredBlock<Block> POWER_EGG_BLOCK = block("power_egg_block", SplatcraftBlockProperties.lightInk());

    public static final DeferredBlock<Block> CORALITE = BLOCKS.register("coralite", () -> new BlockStateCompatBlocks.ColoredBlockEntityBlock(SplatcraftBlockProperties.stone()));
    public static final DeferredBlock<Block> CORALITE_SLAB = BLOCKS.register("coralite_slab", () -> new BlockStateCompatBlocks.ColoredSlabBlockEntityBlock(SplatcraftBlockProperties.stone()));
    public static final DeferredBlock<Block> CORALITE_STAIRS = BLOCKS.register("coralite_stairs", () -> coloredStairs(CORALITE.get(), SplatcraftBlockProperties.stone()));

    public static final DeferredBlock<Block> INK_VAT = BLOCKS.register("ink_vat", () -> new BlockStateCompatBlocks.InkVatBlockEntityBlock(SplatcraftBlockProperties.metal().noOcclusion()));
    public static final DeferredBlock<Block> AMMO_KNIGHTS_WORKBENCH = BLOCKS.register("ammo_knights_workbench", () -> new BlockStateCompatBlocks.WeaponWorkbenchBlock(SplatcraftBlockProperties.metal().noOcclusion()));
    public static final DeferredBlock<Block> REMOTE_PEDESTAL = BLOCKS.register("remote_pedestal", () -> new BlockStateCompatBlocks.RemotePedestalBlockEntityBlock(SplatcraftBlockProperties.metal().noOcclusion()));

    public static final DeferredBlock<Block> EMPTY_INKWELL = BLOCKS.register("empty_inkwell", () -> new BlockStateCompatBlocks.EmptyInkwellBlock(SplatcraftBlockProperties.glass().noOcclusion()));
    public static final DeferredBlock<Block> INKWELL = BLOCKS.register("inkwell", () -> new BlockStateCompatBlocks.InkwellBlock(SplatcraftBlockProperties.glass().noOcclusion().lightLevel(state -> 5)));
    public static final DeferredBlock<Block> INK_STAINED_WOOL = BLOCKS.register("ink_stained_wool", () -> new BlockStateCompatBlocks.InkColorBlockEntityBlock(SplatcraftBlockProperties.soft()));
    public static final DeferredBlock<Block> INK_STAINED_CARPET = BLOCKS.register("ink_stained_carpet", () -> new BlockStateCompatBlocks.InkColorCarpetBlock(SplatcraftBlockProperties.soft()));
    public static final DeferredBlock<Block> INK_STAINED_GLASS = BLOCKS.register("ink_stained_glass", () -> new BlockStateCompatBlocks.InkColorTransparentBlock(SplatcraftBlockProperties.glass()));
    public static final DeferredBlock<Block> INK_STAINED_GLASS_PANE = BLOCKS.register("ink_stained_glass_pane", () -> new BlockStateCompatBlocks.InkColorIronBarsBlock(SplatcraftBlockProperties.glass()));
    public static final DeferredBlock<Block> CANVAS = BLOCKS.register("canvas", () -> new BlockStateCompatBlocks.CanvasBlock(SplatcraftBlockProperties.soft()));

    public static final DeferredBlock<Block> SPLAT_SWITCH = BLOCKS.register("splat_switch", () -> new BlockStateCompatBlocks.SplatSwitchBlockEntityBlock(SplatcraftBlockProperties.switchLike().noOcclusion()));
    public static final DeferredBlock<Block> SPAWN_PAD = BLOCKS.register("spawn_pad", () -> new BlockStateCompatBlocks.SpawnPadBlockEntityBlock(SplatcraftBlockProperties.metal().noOcclusion().lightLevel(state -> 7)));
    public static final DeferredBlock<Block> SPAWN_PAD_EDGE = BLOCKS.register("spawn_pad_edge", () -> new BlockStateCompatBlocks.SpawnPadEdgeBlock(SplatcraftBlockProperties.metal().noLootTable().noOcclusion()));
    public static final DeferredBlock<Block> GRATE = BLOCKS.register("grate", () -> new BlockStateCompatBlocks.GrateBlock(SplatcraftBlockProperties.metal().noOcclusion()));
    public static final DeferredBlock<Block> GRATE_RAMP = BLOCKS.register("grate_ramp", () -> new BlockStateCompatBlocks.GrateRampBlock(SplatcraftBlockProperties.metal().noOcclusion()));
    public static final DeferredBlock<Block> BARRIER_BAR = BLOCKS.register("barrier_bar", () -> new BlockStateCompatBlocks.BarrierBarBlock(SplatcraftBlockProperties.metal().noOcclusion()));
    public static final DeferredBlock<Block> CAUTION_BARRIER_BAR = BLOCKS.register("caution_barrier_bar", () -> new BlockStateCompatBlocks.BarrierBarBlock(SplatcraftBlockProperties.metal().noOcclusion()));
    public static final DeferredBlock<Block> PLATED_BARRIER_BAR = BLOCKS.register("plated_barrier_bar", () -> new BlockStateCompatBlocks.BarrierBarBlock(SplatcraftBlockProperties.metal().noOcclusion()));
    public static final DeferredBlock<Block> TARP = BLOCKS.register("tarp", () -> new BlockStateCompatBlocks.TarpBlock(SplatcraftBlockProperties.soft().noOcclusion()));
    public static final DeferredBlock<Block> GLASS_COVER = BLOCKS.register("glass_cover", () -> new BlockStateCompatBlocks.GlassCoverBlock(SplatcraftBlockProperties.glass()));

    public static final DeferredBlock<Block> CRATE = BLOCKS.register("crate", () -> new BlockStateCompatBlocks.CrateBlockEntityBlock(SplatcraftBlockProperties.soft().noOcclusion()));
    public static final DeferredBlock<Block> SUNKEN_CRATE = BLOCKS.register("sunken_crate", () -> new BlockStateCompatBlocks.CrateBlockEntityBlock(SplatcraftBlockProperties.soft().noOcclusion()));
    public static final DeferredBlock<Block> AMMO_KNIGHTS_DEBRIS = BLOCKS.register("ammo_knights_debris", () -> new BlockStateCompatBlocks.DebrisBlock(SplatcraftBlockProperties.metal().noOcclusion().lightLevel(state -> 1)));

    public static final DeferredBlock<Block> STAGE_BARRIER = BLOCKS.register("stage_barrier", () -> new BlockStateCompatBlocks.StageBarrierBlockEntityBlock(SplatcraftBlockProperties.barrier()));
    public static final DeferredBlock<Block> STAGE_VOID = BLOCKS.register("stage_void", () -> new BlockStateCompatBlocks.StageVoidBlockEntityBlock(SplatcraftBlockProperties.barrier().lightLevel(state -> 2)));
    public static final DeferredBlock<Block> ALLOWED_COLOR_BARRIER = BLOCKS.register("allowed_color_barrier", () -> new BlockStateCompatBlocks.ColorBarrierBlockEntityBlock(SplatcraftBlockProperties.barrier()));
    public static final DeferredBlock<Block> DENIED_COLOR_BARRIER = BLOCKS.register("denied_color_barrier", () -> new BlockStateCompatBlocks.ColorBarrierBlockEntityBlock(SplatcraftBlockProperties.barrier()));

    static {
        alias("inked_wool", "ink_stained_wool");
        alias("inked_carpet", "ink_stained_carpet");
        alias("inked_glass", "ink_stained_glass");
        alias("inked_glass_pane", "ink_stained_glass_pane");
        alias("weapon_workbench", "ammo_knights_workbench");
        alias("inked_stairs", "inked_block");
        alias("inked_slab", "inked_block");
        alias("tall_inked_block", "inked_block");
        alias("glowing_inked_stairs", "inked_block");
        alias("glowing_inked_slab", "inked_block");
        alias("tall_glowing_inked_block", "inked_block");
        alias("tall_clear_inked_block", "inked_block");
    }

    private SplatcraftBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static DeferredBlock<Block> block(String name, BlockBehaviour.Properties properties) {
        return BLOCKS.register(name, () -> SplatcraftBlockProperties.block(properties));
    }

    private static StairBlock coloredStairs(Block baseBlock, BlockBehaviour.Properties properties) {
        return new BlockStateCompatBlocks.ColoredStairBlockEntityBlock(baseBlock.defaultBlockState(), properties);
    }

    private static void alias(String fromPath, String toPath) {
        BLOCKS.addAlias(id(fromPath), id(toPath));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, path);
    }
}
