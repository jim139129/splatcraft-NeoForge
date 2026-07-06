package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.blockentity.CrateBlockEntity;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.blockentity.InkVatBlockEntity;
import net.splatcraft.neoforge.blockentity.InkedBlockEntity;
import net.splatcraft.neoforge.blockentity.RemotePedestalBlockEntity;
import net.splatcraft.neoforge.blockentity.SpawnPadBlockEntity;
import net.splatcraft.neoforge.blockentity.StageBarrierBlockEntity;

public final class SplatcraftBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Splatcraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InkColorBlockEntity>> COLOR = register(
            "color",
            InkColorBlockEntity::new,
            SplatcraftBlocks.INK_STAINED_WOOL,
            SplatcraftBlocks.INK_STAINED_GLASS,
            SplatcraftBlocks.INK_STAINED_GLASS_PANE,
            SplatcraftBlocks.INK_STAINED_CARPET,
            SplatcraftBlocks.CANVAS,
            SplatcraftBlocks.CORALITE,
            SplatcraftBlocks.CORALITE_SLAB,
            SplatcraftBlocks.CORALITE_STAIRS,
            SplatcraftBlocks.SPLAT_SWITCH,
            SplatcraftBlocks.INKWELL
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InkedBlockEntity>> INKED_BLOCK = register(
            "inked_block",
            InkedBlockEntity::new,
            SplatcraftBlocks.INKED_BLOCK,
            SplatcraftBlocks.GLOWING_INKED_BLOCK,
            SplatcraftBlocks.CLEAR_INKED_BLOCK
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE = register(
            "crate",
            CrateBlockEntity::new,
            SplatcraftBlocks.CRATE,
            SplatcraftBlocks.SUNKEN_CRATE
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StageBarrierBlockEntity>> STAGE_BARRIER = register(
            "stage_barrier",
            StageBarrierBlockEntity::new,
            SplatcraftBlocks.STAGE_BARRIER,
            SplatcraftBlocks.STAGE_VOID
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ColoredBarrierBlockEntity>> COLOR_BARRIER = register(
            "color_barrier",
            ColoredBarrierBlockEntity::new,
            SplatcraftBlocks.ALLOWED_COLOR_BARRIER,
            SplatcraftBlocks.DENIED_COLOR_BARRIER
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InkVatBlockEntity>> INK_VAT = register(
            "ink_vat",
            InkVatBlockEntity::new,
            SplatcraftBlocks.INK_VAT
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RemotePedestalBlockEntity>> REMOTE_PEDESTAL = register(
            "remote_pedestal",
            RemotePedestalBlockEntity::new,
            SplatcraftBlocks.REMOTE_PEDESTAL
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpawnPadBlockEntity>> SPAWN_PAD = register(
            "spawn_pad",
            SpawnPadBlockEntity::new,
            SplatcraftBlocks.SPAWN_PAD
    );

    private SplatcraftBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    @SafeVarargs
    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            DeferredHolder<Block, ? extends Block>... blocks
    ) {
        return BLOCK_ENTITY_TYPES.register(name, () -> BlockEntityType.Builder.of(factory, resolve(blocks)).build(null));
    }

    @SafeVarargs
    private static Block[] resolve(DeferredHolder<Block, ? extends Block>... blocks) {
        Block[] resolved = new Block[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            resolved[i] = blocks[i].get();
        }
        return resolved;
    }
}
