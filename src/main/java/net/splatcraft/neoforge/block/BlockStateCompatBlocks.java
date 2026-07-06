package net.splatcraft.neoforge.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.blockentity.CrateBlockEntity;
import net.splatcraft.neoforge.blockentity.InkedBlockEntity;
import net.splatcraft.neoforge.blockentity.InkVatBlockEntity;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.blockentity.RemotePedestalBlockEntity;
import net.splatcraft.neoforge.blockentity.SpawnPadBlockEntity;
import net.splatcraft.neoforge.blockentity.StageBarrierBlockEntity;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;
import net.splatcraft.neoforge.entity.SplatcraftEntityState;
import net.splatcraft.neoforge.menu.WeaponWorkbenchMenu;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftDamageTypes;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.registry.SplatcraftSounds;
import net.splatcraft.neoforge.worldink.InkBlockUtils;
import net.splatcraft.neoforge.worldink.WorldInkStorage;

public final class BlockStateCompatBlocks {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty COLORED = BooleanProperty.create("colored");
    public static final BooleanProperty INKED = BooleanProperty.create("inked");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty CRATE_STATE = IntegerProperty.create("state", 0, 4);
    private static final VoxelShape INKWELL_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),
            Block.box(1.0D, 12.0D, 1.0D, 14.0D, 13.0D, 14.0D),
            Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    );

    private BlockStateCompatBlocks() {
    }

    private static Iterable<BlockPos> spawnPadEdgePositions(BlockPos pos) {
        java.util.List<BlockPos> positions = new java.util.ArrayList<>(8);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            positions.add(pos.relative(direction));
            positions.add(pos.relative(direction).relative(direction.getCounterClockWise()));
        }
        return positions;
    }

    public static BlockPos spawnPadParent(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos parentPos = pos.relative(direction);
            if (level.getBlockState(parentPos).is(SplatcraftBlocks.SPAWN_PAD.get())) {
                return parentPos;
            }
            parentPos = pos.relative(direction).relative(direction.getClockWise());
            if (level.getBlockState(parentPos).is(SplatcraftBlocks.SPAWN_PAD.get())) {
                return parentPos;
            }
        }
        return null;
    }

    private static void removeSpawnPadEdges(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        for (BlockPos edgePos : spawnPadEdgePositions(pos)) {
            if (level.getBlockState(edgePos).is(SplatcraftBlocks.SPAWN_PAD_EDGE.get())) {
                level.setBlock(edgePos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static boolean isSquidPassthroughCollision(CollisionContext context) {
        return context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() != null
                && SplatcraftEntityState.isSquid(entityContext.getEntity());
    }

    private static ItemStack cloneWithColor(ItemStack stack, BlockGetter level, BlockPos pos, BlockState state, boolean lockColor) {
        if ((state.hasProperty(COLORED) && !state.getValue(COLORED))
                || (state.hasProperty(INKED) && !state.getValue(INKED))) {
            return stack;
        }

        if (level.getBlockEntity(pos) instanceof InkColorBlockEntity colorBlock) {
            InkColorComponent.setColor(stack, colorBlock.getColor());
            InkColorComponent.setColorLocked(stack, lockColor);
            InkColorComponent.setInverted(stack, colorBlock.isInverted());
        } else if (level.getBlockEntity(pos) instanceof ColoredBarrierBlockEntity barrier) {
            InkColorComponent.setColor(stack, barrier.getColor());
            InkColorComponent.setColorLocked(stack, lockColor);
            InkColorComponent.setInverted(stack, barrier.isInverted());
        }
        return stack;
    }

    private static BlockState waterloggedState(BlockState state, BlockPlaceContext context) {
        return state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    private static void scheduleWaterTick(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    private static FluidState fluidState(BlockState state, FluidState fallback) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : fallback;
    }

    private static boolean isTouchingInkClearingFluid(BlockGetter level, BlockPos pos) {
        return net.splatcraft.neoforge.worldink.InkBlockUtils.isTouchingInkClearingFluid(level, pos);
    }

    private static Integer beaconColor(BlockGetter level, BlockPos pos) {
        int color = -1;
        if (level.getBlockEntity(pos) instanceof InkColorBlockEntity colorBlock) {
            color = colorBlock.effectiveColor();
        }
        return color < 0 ? 0xFFFFFF : color & 0xFFFFFF;
    }

    public static class ColoredBlock extends Block {
        public ColoredBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(COLORED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(ColoredBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(COLORED);
        }
    }

    public static class InkColorBlockEntityBlock extends Block implements EntityBlock {
        public InkColorBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(InkColorBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }
    }

    public static class ColoredBlockEntityBlock extends ColoredBlock implements EntityBlock {
        public ColoredBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(ColoredBlockEntityBlock::new);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null
                    ? null
                    : state.setValue(COLORED, InkColorComponent.color(context.getItemInHand()).isPresent());
        }

        @Override
        public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
            if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof InkColorBlockEntity colorBlock
                    && colorBlock.getColor() != InkColorComponent.color(context.getItemInHand()).orElse(-1)) {
                return false;
            }
            return super.canBeReplaced(state, context);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, false);
        }
    }

    public static class InkColorCarpetBlock extends CarpetBlock implements EntityBlock {
        public InkColorCarpetBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<? extends CarpetBlock> codec() {
            return simpleCodec(InkColorCarpetBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }
    }

    public static class EmptyInkwellBlock extends TransparentBlock implements SimpleWaterloggedBlock {
        public EmptyInkwellBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends TransparentBlock> codec() {
            return simpleCodec(EmptyInkwellBlock::new);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return waterloggedState(this.defaultBlockState(), context);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(WATERLOGGED);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return INKWELL_SHAPE;
        }

        @Override
        public PushReaction getPistonPushReaction(BlockState state) {
            return PushReaction.DESTROY;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return false;
        }
    }

    public static class InkColorTransparentBlock extends TransparentBlock implements EntityBlock {
        public InkColorTransparentBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends TransparentBlock> codec() {
            return simpleCodec(InkColorTransparentBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        public Integer getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
            return beaconColor(level, pos);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level,
                BlockState state,
                BlockEntityType<T> blockEntityType
        ) {
            return this == SplatcraftBlocks.INKWELL.get() && blockEntityType == SplatcraftBlockEntities.COLOR.get()
                    ? (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    tickInkwell(tickerLevel, tickerPos, (InkColorBlockEntity) blockEntity)
                    : null;
        }

        private static void tickInkwell(Level level, BlockPos pos, InkColorBlockEntity inkwell) {
            if (level.isClientSide) {
                return;
            }

            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.above()))) {
                ItemStack coated = net.splatcraft.neoforge.registry.SplatcraftItems.inkCoatingResult(entity.getItem(), inkwell.getColor());
                if (!coated.isEmpty()) {
                    entity.setItem(coated);
                }
            }
        }
    }

    public static class InkwellBlock extends InkColorTransparentBlock implements SimpleWaterloggedBlock {
        public InkwellBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends TransparentBlock> codec() {
            return simpleCodec(InkwellBlock::new);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return waterloggedState(this.defaultBlockState(), context);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(WATERLOGGED);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return INKWELL_SHAPE;
        }

        @Override
        public PushReaction getPistonPushReaction(BlockState state) {
            return PushReaction.DESTROY;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return false;
        }
    }

    public static class InkColorIronBarsBlock extends IronBarsBlock implements EntityBlock {
        public InkColorIronBarsBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<? extends IronBarsBlock> codec() {
            return simpleCodec(InkColorIronBarsBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        public Integer getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
            return beaconColor(level, pos);
        }
    }

    public static class ColoredSlabBlock extends SlabBlock {
        public ColoredSlabBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(COLORED, false));
        }

        @Override
        public MapCodec<? extends SlabBlock> codec() {
            return simpleCodec(ColoredSlabBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(COLORED);
        }
    }

    public static class ColoredSlabBlockEntityBlock extends ColoredSlabBlock implements EntityBlock {
        public ColoredSlabBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<? extends SlabBlock> codec() {
            return simpleCodec(ColoredSlabBlockEntityBlock::new);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null
                    ? null
                    : state.setValue(COLORED, InkColorComponent.color(context.getItemInHand()).isPresent());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, false);
        }
    }

    public static class ColoredStairBlock extends StairBlock {
        public ColoredStairBlock(BlockState baseState, BlockBehaviour.Properties properties) {
            super(baseState, properties);
            this.registerDefaultState(this.defaultBlockState().setValue(COLORED, false));
        }

        @Override
        public MapCodec<? extends StairBlock> codec() {
            return StairBlock.CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(COLORED);
        }
    }

    public static class ColoredStairBlockEntityBlock extends ColoredStairBlock implements EntityBlock {
        public ColoredStairBlockEntityBlock(BlockState baseState, BlockBehaviour.Properties properties) {
            super(baseState, properties);
        }

        @Override
        public MapCodec<? extends StairBlock> codec() {
            return StairBlock.CODEC;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null
                    ? null
                    : state.setValue(COLORED, InkColorComponent.color(context.getItemInHand()).isPresent());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, false);
        }
    }

    public static class InkedBooleanBlock extends Block {
        public InkedBooleanBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(INKED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(InkedBooleanBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(INKED);
        }
    }

    public static class InkedBooleanBlockEntityBlock extends InkedBooleanBlock implements EntityBlock {
        public InkedBooleanBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(InkedBooleanBlockEntityBlock::new);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null
                    ? null
                    : state.setValue(INKED, InkColorComponent.color(context.getItemInHand()).isPresent());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }
    }

    public static class CanvasBlock extends InkedBooleanBlockEntityBlock {
        public CanvasBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(CanvasBlock::new);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            if (!level.isClientSide()
                    && state.getValue(INKED)
                    && level instanceof Level actualLevel
                    && isTouchingInkClearingFluid(level, currentPos)) {
                clear(actualLevel, currentPos, state);
                return actualLevel.getBlockState(currentPos);
            }
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        public static net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult ink(Level level, BlockPos pos, int color) {
            if (level.isClientSide || isTouchingInkClearingFluid(level, pos)) {
                return net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult.FAIL;
            }

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CanvasBlock)
                    || !(level.getBlockEntity(pos) instanceof InkColorBlockEntity colorBlock)) {
                return net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult.FAIL;
            }

            int previousColor = colorBlock.getColor();
            colorBlock.setColor(color);
            BlockState inkedState = state.setValue(INKED, true);
            if (state != inkedState) {
                level.setBlock(pos, inkedState, Block.UPDATE_ALL_IMMEDIATE);
            }
            level.sendBlockUpdated(pos, state, inkedState, Block.UPDATE_ALL_IMMEDIATE);
            return state.getValue(INKED) && previousColor == color
                    ? net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult.ALREADY_INKED
                    : net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult.SUCCESS;
        }

        public static boolean clear(Level level, BlockPos pos) {
            return clear(level, pos, level.getBlockState(pos));
        }

        private static boolean clear(Level level, BlockPos pos, BlockState state) {
            if (!(state.getBlock() instanceof CanvasBlock)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof InkColorBlockEntity colorBlock) {
                colorBlock.setColor(-1);
            }
            if (!state.getValue(INKED)) {
                return false;
            }
            BlockState clearedState = state.setValue(INKED, false);
            level.setBlock(pos, clearedState, Block.UPDATE_ALL_IMMEDIATE);
            level.sendBlockUpdated(pos, state, clearedState, Block.UPDATE_ALL_IMMEDIATE);
            return true;
        }
    }

    public static class InkedBlockEntityBlock extends Block implements EntityBlock {
        public InkedBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(InkedBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.INKED_BLOCK.get().create(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level,
                BlockState state,
                BlockEntityType<T> blockEntityType
        ) {
            return blockEntityType == SplatcraftBlockEntities.INKED_BLOCK.get()
                    ? (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    tickLegacyInkedBlock(tickerLevel, tickerPos, tickerState, (InkedBlockEntity) blockEntity)
                    : null;
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            BlockState savedState = savedState(level, pos);
            return savedState == null
                    ? ItemStack.EMPTY
                    : savedState.getBlock().getCloneItemStack(level, pos, savedState);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            BlockState savedState = savedState(level, pos);
            if (savedState == null) {
                return super.getShape(state, level, pos, context);
            }
            VoxelShape savedShape = savedState.getShape(level, pos, context);
            return savedShape.isEmpty() ? super.getShape(state, level, pos, context) : savedShape;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            BlockState savedState = savedState(level, pos);
            if (savedState == null) {
                return super.getCollisionShape(state, level, pos, context);
            }
            VoxelShape savedShape = savedState.getCollisionShape(level, pos, context);
            return savedShape.isEmpty() ? super.getCollisionShape(state, level, pos, context) : savedShape;
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            BlockState savedState = savedState(level, pos);
            if (savedState == null) {
                return super.getVisualShape(state, level, pos, context);
            }
            VoxelShape savedShape = savedState.getVisualShape(level, pos, context);
            return savedShape.isEmpty() ? super.getVisualShape(state, level, pos, context) : savedShape;
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            BlockState savedState = savedState(level, pos);
            return savedState == null
                    ? super.getDestroyProgress(state, player, level, pos)
                    : savedState.getDestroyProgress(player, level, pos);
        }

        private static BlockState savedState(BlockGetter level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof InkedBlockEntity inkedBlock)) {
                return null;
            }
            BlockState savedState = inkedBlock.getSavedState();
            if (savedState == null || savedState.isAir() || savedState.getBlock() instanceof InkedBlockEntityBlock) {
                return null;
            }
            return savedState;
        }

        private static void tickLegacyInkedBlock(Level level, BlockPos pos, BlockState state, InkedBlockEntity inkedBlock) {
            if (level.isClientSide) {
                return;
            }

            BlockState savedState = savedState(level, pos);
            if (savedState == null) {
                return;
            }

            int color = inkedBlock.getColor();
            int savedColor = inkedBlock.getSavedColor();
            int permanentColor = inkedBlock.getPermanentColor();
            net.minecraft.resources.ResourceLocation inkType = inkType(state);
            net.minecraft.resources.ResourceLocation permanentInkType = inkedBlock.getPermanentInkType();

            level.setBlock(pos, savedState, 2);
            if (permanentColor != -1) {
                WorldInkStorage.setPermanentInk(level, pos, permanentColor, permanentInkType);
            }
            InkBlockUtils.inkBlock(level, pos, color, 0.0F, inkType);
            restoreSavedColor(level, pos, savedColor);
        }

        private static void restoreSavedColor(Level level, BlockPos pos, int savedColor) {
            if (savedColor == -1) {
                return;
            }

            BlockEntity restoredBlockEntity = level.getBlockEntity(pos);
            if (restoredBlockEntity instanceof InkColorBlockEntity colorBlock) {
                colorBlock.setColor(savedColor);
                sendBlockUpdate(colorBlock);
            } else if (restoredBlockEntity instanceof ColoredBarrierBlockEntity barrier) {
                barrier.setColor(savedColor);
                sendBlockUpdate(barrier);
            }
        }

        private static void sendBlockUpdate(BlockEntity blockEntity) {
            if (blockEntity.getLevel() != null) {
                blockEntity.getLevel().sendBlockUpdated(
                        blockEntity.getBlockPos(),
                        blockEntity.getBlockState(),
                        blockEntity.getBlockState(),
                        2);
            }
        }

        private static net.minecraft.resources.ResourceLocation inkType(BlockState state) {
            if (state.is(SplatcraftBlocks.GLOWING_INKED_BLOCK.get())) {
                return PlayerInfo.GLOWING_INK_TYPE;
            }
            if (state.is(SplatcraftBlocks.CLEAR_INKED_BLOCK.get())) {
                return PlayerInfo.CLEAR_INK_TYPE;
            }
            return PlayerInfo.NORMAL_INK_TYPE;
        }
    }

    public static class CrateStateBlock extends Block {
        public CrateStateBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(CRATE_STATE, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(CrateStateBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(CRATE_STATE);
        }
    }

    public static class CrateBlockEntityBlock extends CrateStateBlock implements EntityBlock {
        public static final ResourceLocation STORAGE_SUNKEN_CRATE = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "storage/sunken_crate");

        public CrateBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(CrateBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            CrateBlockEntity crate = SplatcraftBlockEntities.CRATE.get().create(pos, state);
            if (crate != null) {
                crate.initializeHealth(state);
                if (state.is(SplatcraftBlocks.SUNKEN_CRATE.get())) {
                    crate.setLootTable(STORAGE_SUNKEN_CRATE);
                }
            }
            return crate;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            CompoundTag tag = crateBlockEntityTag(stack);
            boolean sunken = stack.is(SplatcraftBlocks.SUNKEN_CRATE.get().asItem());
            if (!sunken && tag.isEmpty()) {
                return;
            }

            if (sunken || tag.contains("LootTable", Tag.TAG_STRING)) {
                tooltip.add(Component.translatable("block.splatcraft.crate.loot"));
            } else if (tag.contains("Items", Tag.TAG_LIST)) {
                appendStoredItemTooltip(tag, context, tooltip);
            }
        }

        private static void appendStoredItemTooltip(CompoundTag tag, Item.TooltipContext context, List<Component> tooltip) {
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, context.registries());
            int shown = 0;
            int total = 0;
            for (ItemStack itemStack : items) {
                if (itemStack.isEmpty()) {
                    continue;
                }

                total++;
                if (shown <= 4) {
                    shown++;
                    MutableComponent component = itemStack.getHoverName().copy();
                    component.append(" x").append(String.valueOf(itemStack.getCount()));
                    tooltip.add(component);
                }
            }

            if (total - shown > 0) {
                tooltip.add(Component.translatable("container.shulkerBox.more", total - shown).withStyle(ChatFormatting.ITALIC));
            }
        }

        private static CompoundTag crateBlockEntityTag(ItemStack stack) {
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData != null) {
                return blockEntityData.copyTag();
            }

            CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return customData.contains("BlockEntityTag", Tag.TAG_COMPOUND)
                    ? customData.getCompound("BlockEntityTag")
                    : customData;
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            if (level.getBlockEntity(currentPos) instanceof CrateBlockEntity crate) {
                return state.setValue(CRATE_STATE, crate.stateIndex());
            }
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return !state.is(SplatcraftBlocks.SUNKEN_CRATE.get());
        }

        @Override
        protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            if (state.is(SplatcraftBlocks.SUNKEN_CRATE.get()) || !(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
                return 0;
            }
            ItemStack stack = crate.getItem(0);
            return (int) Math.ceil(stack.getCount() / (float) stack.getMaxStackSize() * 15.0F);
        }

        @Override
        public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack) {
            player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);

            if (!level.isClientSide && shouldDropCrateContents(level, blockEntity, stack)) {
                ((CrateBlockEntity) blockEntity).dropInventory();
            } else {
                dropResources(state, level, pos, blockEntity, player, stack);
            }
        }

        @Override
        public java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            ServerLevel level = builder.getLevel();
            BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
            if (shouldDropCrateContents(level, blockEntity, tool == null ? ItemStack.EMPTY : tool)) {
                return ((CrateBlockEntity) blockEntity).getDrops(level);
            }
            return super.getDrops(state, builder);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
                if (crate.getMaxHealth() <= 0.0F) {
                    crate.initializeHealth(state);
                }
                if (state.is(SplatcraftBlocks.SUNKEN_CRATE.get()) && !crate.hasLoot()) {
                    crate.setLootTable(STORAGE_SUNKEN_CRATE);
                }
            }
        }

        private static boolean shouldDropCrateContents(Level level, BlockEntity blockEntity, ItemStack tool) {
            return blockEntity instanceof CrateBlockEntity
                    && level.getGameRules().getBoolean(SplatcraftGameRules.DROP_CRATE_LOOT)
                    && !hasSilkTouch(level, tool);
        }

        private static boolean hasSilkTouch(Level level, ItemStack tool) {
            if (tool.isEmpty()) {
                return false;
            }

            Holder<Enchantment> silkTouch = level.registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH);
            return EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) > 0;
        }
    }

    public static class HorizontalFacingBlock extends HorizontalDirectionalBlock {
        public HorizontalFacingBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(HorizontalFacingBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }

    public static class SquidPassthroughHorizontalFacingBlock extends HorizontalFacingBlock {
        public SquidPassthroughHorizontalFacingBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(SquidPassthroughHorizontalFacingBlock::new);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return isSquidPassthroughCollision(context)
                    ? Shapes.empty()
                    : super.getCollisionShape(state, level, pos, context);
        }
    }

    public static class GrateBlock extends DirectionFacingBlock implements SimpleWaterloggedBlock {
        private static final VoxelShape DOWN_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);
        private static final VoxelShape UP_SHAPE = Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
        private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
        private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
        private static final VoxelShape EAST_SHAPE = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

        public GrateBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.DOWN).setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(GrateBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = this.defaultBlockState();
            Direction direction = context.getClickedFace();

            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                state = state.setValue(FACING, direction.getOpposite());
            } else if (!context.replacingClickedOnBlock() && direction.getAxis().isHorizontal()) {
                state = state.setValue(FACING, context.getClickLocation().y - (double) context.getClickedPos().getY() > 0.5D ? Direction.UP : Direction.DOWN);
            } else {
                state = state.setValue(FACING, direction == Direction.UP ? Direction.DOWN : Direction.UP);
            }

            return waterloggedState(state, context);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case DOWN -> DOWN_SHAPE;
                case UP -> UP_SHAPE;
                case NORTH -> NORTH_SHAPE;
                case SOUTH -> SOUTH_SHAPE;
                case WEST -> WEST_SHAPE;
                case EAST -> EAST_SHAPE;
            };
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return isSquidPassthroughCollision(context)
                    ? Shapes.empty()
                    : super.getCollisionShape(state, level, pos, context);
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return pathComputationType == PathComputationType.WATER && state.getValue(WATERLOGGED);
        }
    }

    public static class GrateRampBlock extends HorizontalFacingBlock implements SimpleWaterloggedBlock {
        private static final VoxelShape RAMP_START = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 3.0D, 16.0D);
        private static final VoxelShape RAMP_END = Block.box(13.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        private static final VoxelShape RAMP_SEGMENT = Block.box(1.0D, 2.0D, 0.0D, 4.0D, 5.0D, 16.0D);
        private static final VoxelShape[] RAMP_SHAPES = createRampVoxelShapes();

        public GrateRampBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(GrateRampBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockPos pos = context.getClickedPos();
            Direction direction = context.getClickedFace();
            boolean flip = direction != Direction.DOWN
                    && (direction == Direction.UP || !(context.getClickLocation().y - (double) pos.getY() <= 0.5D));
            BlockState state = this.defaultBlockState().setValue(
                    FACING,
                    flip ? context.getHorizontalDirection().getOpposite() : context.getHorizontalDirection()
            );
            return waterloggedState(state, context);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return RAMP_SHAPES[state.getValue(FACING).ordinal() - 2];
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return isSquidPassthroughCollision(context)
                    ? Shapes.empty()
                    : super.getCollisionShape(state, level, pos, context);
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return false;
        }

        private static VoxelShape[] createRampVoxelShapes() {
            VoxelShape[] segments = new VoxelShape[8];
            for (int i = 0; i < 6; i++) {
                segments[i] = RAMP_SEGMENT.move(0.125D * i, 0.125D * i, 0.0D);
            }
            segments[6] = RAMP_START;
            segments[7] = RAMP_END;
            return createRampVoxelShapes(segments);
        }

        private static VoxelShape[] createRampVoxelShapes(VoxelShape... shapes) {
            VoxelShape[] result = new VoxelShape[4];
            for (int i = 0; i < 4; i++) {
                result[i] = Shapes.empty();
                Direction direction = Direction.from2DDataValue(i);
                for (VoxelShape shape : shapes) {
                    result[i] = Shapes.or(result[i], rotateRampShape(direction, shape));
                }
            }
            return result;
        }

        private static VoxelShape rotateRampShape(Direction facing, VoxelShape shape) {
            AABB bounds = shape.bounds();
            return switch (facing) {
                case SOUTH -> Shapes.create(new AABB(1.0D - bounds.maxZ, bounds.minY, bounds.minX, 1.0D - bounds.minZ, bounds.maxY, bounds.maxX));
                case EAST -> Shapes.create(new AABB(1.0D - bounds.maxX, bounds.minY, 1.0D - bounds.maxZ, 1.0D - bounds.minX, bounds.maxY, 1.0D - bounds.minZ));
                case WEST -> Shapes.create(new AABB(bounds.minZ, bounds.minY, 1.0D - bounds.maxX, bounds.maxZ, bounds.maxY, 1.0D - bounds.minX));
                default -> shape;
            };
        }
    }

    public static class BarrierBarBlock extends Block implements SimpleWaterloggedBlock {
        public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
        public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
        public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;

        private static final VoxelShape NORTH_TOP_STRAIGHT = Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 3.0D);
        private static final VoxelShape SOUTH_TOP_STRAIGHT = rotateBarrierShape(Direction.SOUTH, NORTH_TOP_STRAIGHT);
        private static final VoxelShape WEST_TOP_STRAIGHT = rotateBarrierShape(Direction.WEST, NORTH_TOP_STRAIGHT);
        private static final VoxelShape EAST_TOP_STRAIGHT = rotateBarrierShape(Direction.EAST, NORTH_TOP_STRAIGHT);
        private static final VoxelShape NORTH_BOTTOM_STRAIGHT = mirrorBarrierShapeY(NORTH_TOP_STRAIGHT);
        private static final VoxelShape SOUTH_BOTTOM_STRAIGHT = mirrorBarrierShapeY(SOUTH_TOP_STRAIGHT);
        private static final VoxelShape WEST_BOTTOM_STRAIGHT = mirrorBarrierShapeY(WEST_TOP_STRAIGHT);
        private static final VoxelShape EAST_BOTTOM_STRAIGHT = mirrorBarrierShapeY(EAST_TOP_STRAIGHT);

        private static final VoxelShape NORTH_TOP_CORNER = Block.box(0.0D, 13.0D, 0.0D, 3.0D, 16.0D, 3.0D);
        private static final VoxelShape SOUTH_TOP_CORNER = rotateBarrierShape(Direction.SOUTH, NORTH_TOP_CORNER);
        private static final VoxelShape WEST_TOP_CORNER = rotateBarrierShape(Direction.WEST, NORTH_TOP_CORNER);
        private static final VoxelShape EAST_TOP_CORNER = rotateBarrierShape(Direction.EAST, NORTH_TOP_CORNER);
        private static final VoxelShape NORTH_BOTTOM_CORNER = mirrorBarrierShapeY(NORTH_TOP_CORNER);
        private static final VoxelShape SOUTH_BOTTOM_CORNER = mirrorBarrierShapeY(SOUTH_TOP_CORNER);
        private static final VoxelShape WEST_BOTTOM_CORNER = mirrorBarrierShapeY(WEST_TOP_CORNER);
        private static final VoxelShape EAST_BOTTOM_CORNER = mirrorBarrierShapeY(EAST_TOP_CORNER);

        private static final VoxelShape[] TOP_SHAPES = new VoxelShape[]{
                NORTH_TOP_STRAIGHT,
                SOUTH_TOP_STRAIGHT,
                WEST_TOP_STRAIGHT,
                EAST_TOP_STRAIGHT,
                NORTH_TOP_CORNER,
                SOUTH_TOP_CORNER,
                WEST_TOP_CORNER,
                EAST_TOP_CORNER
        };
        private static final VoxelShape[] BOTTOM_SHAPES = new VoxelShape[]{
                NORTH_BOTTOM_STRAIGHT,
                SOUTH_BOTTOM_STRAIGHT,
                WEST_BOTTOM_STRAIGHT,
                EAST_BOTTOM_STRAIGHT,
                NORTH_BOTTOM_CORNER,
                SOUTH_BOTTOM_CORNER,
                WEST_BOTTOM_CORNER,
                EAST_BOTTOM_CORNER
        };

        public BarrierBarBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(HALF, Half.BOTTOM)
                    .setValue(SHAPE, StairsShape.STRAIGHT)
                    .setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(BarrierBarBlock::new);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            BlockPos pos = context.getClickedPos();
            BlockState state = waterloggedState(this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection())
                    .setValue(
                            HALF,
                            clickedFace != Direction.DOWN
                                    && (clickedFace == Direction.UP || !(context.getClickLocation().y - (double) pos.getY() > 0.5D))
                                    ? Half.BOTTOM
                                    : Half.TOP
                    ), context);
            return state.setValue(SHAPE, getShapeProperty(state, context.getLevel(), pos));
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return direction.getAxis().isHorizontal()
                    ? state.setValue(SHAPE, getShapeProperty(state, level, currentPos))
                    : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, HALF, SHAPE, WATERLOGGED);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            VoxelShape[] shapeArray = state.getValue(HALF) == Half.TOP ? TOP_SHAPES : BOTTOM_SHAPES;
            int directionIndex = state.getValue(FACING).ordinal() - 2;
            int clockwiseIndex = state.getValue(FACING).getClockWise().ordinal() - 2;
            int counterClockwiseIndex = state.getValue(FACING).getCounterClockWise().ordinal() - 2;

            return switch (state.getValue(SHAPE)) {
                case STRAIGHT -> shapeArray[directionIndex];
                case OUTER_LEFT -> shapeArray[directionIndex + 4];
                case OUTER_RIGHT -> shapeArray[clockwiseIndex + 4];
                case INNER_LEFT -> Shapes.or(shapeArray[directionIndex], shapeArray[counterClockwiseIndex]);
                case INNER_RIGHT -> Shapes.or(shapeArray[directionIndex], shapeArray[clockwiseIndex]);
            };
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            Direction direction = state.getValue(FACING);
            StairsShape shape = state.getValue(SHAPE);
            switch (mirror) {
                case LEFT_RIGHT:
                    if (direction.getAxis() == Direction.Axis.Z) {
                        return switch (shape) {
                            case INNER_LEFT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                            case INNER_RIGHT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                            case OUTER_LEFT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                            case OUTER_RIGHT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                            case STRAIGHT -> this.rotate(state, Rotation.CLOCKWISE_180);
                        };
                    }
                    break;
                case FRONT_BACK:
                    if (direction.getAxis() == Direction.Axis.X) {
                        return switch (shape) {
                            case INNER_LEFT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                            case INNER_RIGHT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                            case OUTER_LEFT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                            case OUTER_RIGHT -> this.rotate(state, Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                            case STRAIGHT -> this.rotate(state, Rotation.CLOCKWISE_180);
                        };
                    }
                    break;
                default:
                    break;
            }
            return super.mirror(state, mirror);
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return false;
        }

        private static StairsShape getShapeProperty(BlockState state, BlockGetter level, BlockPos pos) {
            Direction direction = state.getValue(FACING);
            BlockState frontState = level.getBlockState(pos.relative(direction));
            if (isBar(frontState) && state.getValue(HALF) == frontState.getValue(HALF)) {
                Direction frontDirection = frontState.getValue(FACING);
                if (frontDirection.getAxis() != direction.getAxis()
                        && isDifferentBar(state, level, pos, frontDirection.getOpposite())) {
                    return frontDirection == direction.getCounterClockWise()
                            ? StairsShape.OUTER_LEFT
                            : StairsShape.OUTER_RIGHT;
                }
            }

            BlockState backState = level.getBlockState(pos.relative(direction.getOpposite()));
            if (isBar(backState) && state.getValue(HALF) == backState.getValue(HALF)) {
                Direction backDirection = backState.getValue(FACING);
                if (backDirection.getAxis() != direction.getAxis()
                        && isDifferentBar(state, level, pos, backDirection)) {
                    return backDirection == direction.getCounterClockWise()
                            ? StairsShape.INNER_LEFT
                            : StairsShape.INNER_RIGHT;
                }
            }

            return StairsShape.STRAIGHT;
        }

        private static boolean isDifferentBar(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            BlockState neighborState = level.getBlockState(pos.relative(face));
            return !isBar(neighborState)
                    || neighborState.getValue(FACING) != state.getValue(FACING)
                    || neighborState.getValue(HALF) != state.getValue(HALF);
        }

        public static boolean isBar(BlockState state) {
            return state.getBlock() instanceof BarrierBarBlock;
        }

        private static VoxelShape rotateBarrierShape(Direction facing, VoxelShape shape) {
            AABB bounds = shape.bounds();
            return switch (facing) {
                case EAST -> Shapes.create(new AABB(1.0D - bounds.maxZ, bounds.minY, bounds.minX, 1.0D - bounds.minZ, bounds.maxY, bounds.maxX));
                case SOUTH -> Shapes.create(new AABB(1.0D - bounds.maxX, bounds.minY, 1.0D - bounds.maxZ, 1.0D - bounds.minX, bounds.maxY, 1.0D - bounds.minZ));
                case WEST -> Shapes.create(new AABB(bounds.minZ, bounds.minY, 1.0D - bounds.maxX, bounds.maxZ, bounds.maxY, 1.0D - bounds.minX));
                default -> shape;
            };
        }

        private static VoxelShape mirrorBarrierShapeY(VoxelShape shape) {
            AABB bounds = shape.bounds();
            return Shapes.create(new AABB(bounds.minX, 1.0D - bounds.maxY, bounds.minZ, bounds.maxX, 1.0D - bounds.minY, bounds.maxZ));
        }
    }

    public static class TarpBlock extends Block implements SimpleWaterloggedBlock {
        public static final BooleanProperty UP = BlockStateProperties.UP;
        public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
        public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
        public static final BooleanProperty EAST = BlockStateProperties.EAST;
        public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
        public static final BooleanProperty WEST = BlockStateProperties.WEST;

        private static final VoxelShape UP_SHAPE = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        private static final VoxelShape DOWN_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
        private static final VoxelShape EAST_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
        private static final VoxelShape WEST_SHAPE = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
        private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
        private final java.util.Map<BlockState, VoxelShape> stateToShapeMap;

        public TarpBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState()
                    .setValue(WATERLOGGED, false)
                    .setValue(DOWN, true)
                    .setValue(UP, false)
                    .setValue(NORTH, false)
                    .setValue(EAST, false)
                    .setValue(SOUTH, false)
                    .setValue(WEST, false));
            this.stateToShapeMap = this.stateDefinition.getPossibleStates().stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(java.util.function.Function.identity(), TarpBlock::getShapeForState));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(TarpBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(UP, DOWN, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
            BlockState state = clickedState.is(this)
                    ? clickedState
                    : waterloggedState(this.defaultBlockState().setValue(DOWN, false), context);

            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(context.getClickedFace().getOpposite()), true);
            for (Direction direction : Direction.values()) {
                if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                    return state;
                }
            }
            return state.setValue(DOWN, true);
        }

        @Override
        public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
            for (Direction direction : Direction.values()) {
                if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                    return context.getItemInHand().is(this.asItem())
                            && !state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(context.getClickedFace().getOpposite()));
                }
            }
            return true;
        }

        @Override
        public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack) {
            player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);

            for (Direction direction : Direction.values()) {
                if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                    dropResources(state, level, pos, blockEntity, player, stack);
                }
            }
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.stateToShapeMap.get(state);
        }

        private static VoxelShape getShapeForState(BlockState state) {
            VoxelShape shape = Shapes.empty();

            if (state.getValue(UP)) {
                shape = UP_SHAPE;
            }
            if (state.getValue(DOWN)) {
                shape = Shapes.or(shape, DOWN_SHAPE);
            }
            if (state.getValue(NORTH)) {
                shape = Shapes.or(shape, SOUTH_SHAPE);
            }
            if (state.getValue(SOUTH)) {
                shape = Shapes.or(shape, NORTH_SHAPE);
            }
            if (state.getValue(EAST)) {
                shape = Shapes.or(shape, WEST_SHAPE);
            }
            if (state.getValue(WEST)) {
                shape = Shapes.or(shape, EAST_SHAPE);
            }

            return shape;
        }
    }

    public static class GlassCoverBlock extends TarpBlock {
        public GlassCoverBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(GlassCoverBlock::new);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0F;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }
    }

    public static class DebrisBlock extends HorizontalFacingBlock implements SimpleWaterloggedBlock {
        private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 2.4D, 16.0D, 8.0D, 15.2D);
        private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 0.8D, 16.0D, 8.0D, 13.6D);
        private static final VoxelShape WEST_SHAPE = Block.box(2.4D, 0.0D, 0.0D, 15.2D, 8.0D, 16.0D);
        private static final VoxelShape EAST_SHAPE = Block.box(0.8D, 0.0D, 0.0D, 13.6D, 8.0D, 16.0D);

        public DebrisBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(DebrisBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return waterloggedState(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()), context);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case NORTH -> NORTH_SHAPE;
                case SOUTH -> SOUTH_SHAPE;
                case WEST -> WEST_SHAPE;
                case EAST -> EAST_SHAPE;
                default -> Shapes.block();
            };
        }

        @Override
        public PushReaction getPistonPushReaction(BlockState state) {
            return PushReaction.DESTROY;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return false;
        }
    }

    public static class InkVatBlockEntityBlock extends ActiveHorizontalFacingBlock implements EntityBlock {
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

        public InkVatBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(InkVatBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.INK_VAT.get().create(pos, state);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(POWERED);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof InkVatBlockEntity inkVat) {
                player.openMenu(inkVat, pos);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        @Override
        protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof InkVatBlockEntity inkVat) {
                net.minecraft.world.Containers.dropContents(level, pos, inkVat);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        protected int getAnalogOutputSignal(BlockState blockState, net.minecraft.world.level.Level level, BlockPos pos) {
            return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
            boolean powered = level.hasNeighborSignal(pos);
            boolean wasPowered = state.getValue(POWERED);
            if (powered == wasPowered) {
                return;
            }

            if (powered && level.getBlockEntity(pos) instanceof InkVatBlockEntity inkVat) {
                inkVat.onRedstonePulse();
            }
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level,
                BlockState state,
                BlockEntityType<T> blockEntityType
        ) {
            return blockEntityType == SplatcraftBlockEntities.INK_VAT.get()
                    ? (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    InkVatBlockEntity.tick(tickerLevel, tickerPos, tickerState, (InkVatBlockEntity) blockEntity)
                    : null;
        }
    }

    public static class WeaponWorkbenchBlock extends HorizontalFacingBlock {
        private static final VoxelShape BOTTOM_LEFT = Block.box(2.0D, 0.0D, 0.0D, 5.0D, 4.0D, 16.0D);
        private static final VoxelShape BOTTOM_RIGHT = Block.box(11.0D, 0.0D, 0.0D, 14.0D, 4.0D, 16.0D);
        private static final VoxelShape BASE = Block.box(1.0D, 1.0D, 1.0D, 15.0D, 16.0D, 15.0D);
        private static final VoxelShape DETAIL = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 10.0D, 16.0D);
        private static final VoxelShape HANDLE = Block.box(5.0D, 11.0D, 0.0D, 11.0D, 12.0D, 1.0D);
        private static final VoxelShape[] SHAPES = createWorkbenchShapes(BOTTOM_LEFT, BOTTOM_RIGHT, BASE, DETAIL, HANDLE);
        private static final Component CONTAINER_NAME = Component.translatable("container.ammo_knights_workbench");

        public WeaponWorkbenchBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(WeaponWorkbenchBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return waterloggedState(super.getStateForPlacement(context), context);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            player.openMenu(getMenuProvider(state, level, pos), pos);
            return InteractionResult.CONSUME;
        }

        @Override
        public MenuProvider getMenuProvider(BlockState state, net.minecraft.world.level.Level level, BlockPos pos) {
            return new SimpleMenuProvider(
                    (containerId, inventory, player) -> new WeaponWorkbenchMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                    CONTAINER_NAME
            );
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES[state.getValue(FACING).get2DDataValue()];
        }

        private static VoxelShape[] createWorkbenchShapes(VoxelShape... shapes) {
            VoxelShape[] result = new VoxelShape[4];

            for (int i = 0; i < 4; i++) {
                result[i] = Shapes.empty();
                Direction direction = Direction.from2DDataValue(i);
                for (VoxelShape shape : shapes) {
                    result[i] = Shapes.or(result[i], rotateWorkbenchShape(direction, shape));
                }
            }

            return result;
        }

        private static VoxelShape rotateWorkbenchShape(Direction facing, VoxelShape shape) {
            AABB bounds = shape.bounds();
            return switch (facing) {
                case EAST -> Shapes.create(new AABB(1.0D - bounds.minZ, bounds.minY, 1.0D - bounds.minX, 1.0D - bounds.maxZ, bounds.maxY, 1.0D - bounds.maxX));
                case SOUTH -> Shapes.create(new AABB(1.0D - bounds.maxX, bounds.minY, 1.0D - bounds.maxZ, 1.0D - bounds.minX, bounds.maxY, 1.0D - bounds.minZ));
                case WEST -> Shapes.create(new AABB(bounds.minZ, bounds.minY, bounds.minX, bounds.maxZ, bounds.maxY, bounds.maxX));
                default -> shape;
            };
        }
    }

    public static class SpawnPadBlockEntityBlock extends HorizontalFacingBlock implements EntityBlock {
        private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);

        public SpawnPadBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(SpawnPadBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.SPAWN_PAD.get().create(pos, state);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            for (BlockPos edgePos : spawnPadEdgePositions(pos)) {
                if (!level.isInWorldBounds(edgePos) || !level.getBlockState(edgePos).canBeReplaced(context)) {
                    return null;
                }
            }
            BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
            return waterloggedState(state, context);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level.getBlockEntity(pos) instanceof SpawnPadBlockEntity spawnPad) {
                InkColorComponent.color(stack).ifPresent(spawnPad::setColor);
                spawnPad.setInverted(InkColorComponent.isInverted(stack));
            }

            if (!level.isClientSide) {
                for (BlockPos edgePos : spawnPadEdgePositions(pos)) {
                    BlockState edgeState = SplatcraftBlocks.SPAWN_PAD_EDGE.get().defaultBlockState()
                            .setValue(WATERLOGGED, level.getFluidState(edgePos).getType() == Fluids.WATER);
                    level.setBlock(edgePos, edgeState, 3);
                }
            }
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock())) {
                removeSpawnPadEdges(level, pos);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }

        @Override
        public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(
                BlockState state,
                EntityType<?> type,
                LevelReader level,
                BlockPos pos,
                float orientation
        ) {
            Vec3 respawnPos = DismountHelper.findSafeDismountLocation(type, level, pos, false);
            return respawnPos == null
                    ? Optional.empty()
                    : Optional.of(ServerPlayer.RespawnPosAngle.of(respawnPos, pos));
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                net.minecraft.world.level.Level level,
                BlockState state,
                BlockEntityType<T> blockEntityType
        ) {
            return blockEntityType == SplatcraftBlockEntities.SPAWN_PAD.get()
                    ? (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    SpawnPadBlockEntity.tick(tickerLevel, tickerPos, tickerState, (SpawnPadBlockEntity) blockEntity)
                    : null;
        }

        @Override
        public PushReaction getPistonPushReaction(BlockState state) {
            return PushReaction.BLOCK;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return false;
        }
    }

    public static class SpawnPadEdgeBlock extends Block implements SimpleWaterloggedBlock {
        private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);

        public SpawnPadEdgeBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(SpawnPadEdgeBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return null;
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            BlockPos parentPos = spawnPadParent(level, pos);
            if (parentPos != null) {
                return level.getBlockState(parentPos).getBlock().getCloneItemStack(level, parentPos, level.getBlockState(parentPos));
            }
            return super.getCloneItemStack(level, pos, state);
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (!level.isClientSide) {
                BlockPos parentPos = spawnPadParent(level, pos);
                if (parentPos != null) {
                    level.destroyBlock(parentPos, !player.isCreative());
                }
            }
            return super.playerWillDestroy(level, pos, state, player);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            return spawnPadParent(level, currentPos) == null
                    ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public PushReaction getPistonPushReaction(BlockState state) {
            return PushReaction.BLOCK;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
            return false;
        }
    }

    public static class DirectionFacingBlock extends Block {
        public static final DirectionProperty FACING = BlockStateProperties.FACING;

        public DirectionFacingBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(DirectionFacingBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return this.defaultBlockState().setValue(FACING, context.getClickedFace());
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rot) {
            return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
        }
    }

    public static class SquidPassthroughDirectionFacingBlock extends DirectionFacingBlock {
        public SquidPassthroughDirectionFacingBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(SquidPassthroughDirectionFacingBlock::new);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return isSquidPassthroughCollision(context)
                    ? Shapes.empty()
                    : super.getCollisionShape(state, level, pos, context);
        }
    }

    public static class ActiveHorizontalFacingBlock extends HorizontalFacingBlock {
        public ActiveHorizontalFacingBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(ActiveHorizontalFacingBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ACTIVE);
        }
    }

    public static class PoweredBlock extends Block {
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

        public PoweredBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(PoweredBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(POWERED);
        }
    }

    public static class RemotePedestalBlockEntityBlock extends PoweredBlock implements EntityBlock {
        private static final VoxelShape SHAPE = Shapes.or(
                Block.box(3.0D, 0.0D, 3.0D, 13.0D, 2.0D, 13.0D),
                Block.box(4.0D, 2.0D, 4.0D, 12.0D, 3.0D, 12.0D),
                Block.box(5.0D, 3.0D, 5.0D, 11.0D, 11.0D, 11.0D),
                Block.box(4.0D, 11.0D, 4.0D, 12.0D, 13.0D, 12.0D)
        );

        public RemotePedestalBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(RemotePedestalBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.REMOTE_PEDESTAL.get().create(pos, state);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level.getBlockEntity(pos) instanceof RemotePedestalBlockEntity pedestal) {
                InkColorComponent.color(stack).ifPresent(pedestal::setColor);
                updateColorFromInkwell(level, pos);
            }
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }

        @Override
        protected ItemInteractionResult useItemOn(
                ItemStack stack,
                BlockState state,
                Level level,
                BlockPos pos,
                Player player,
                InteractionHand hand,
                BlockHitResult hitResult
        ) {
            if (!(level.getBlockEntity(pos) instanceof RemotePedestalBlockEntity pedestal)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (pedestal.isEmpty() && stack.is(SplatcraftTags.Items.REMOTES)) {
                if (!level.isClientSide) {
                    pedestal.setItem(0, stack.copyWithCount(1));
                    stack.consume(1, player);
                    level.sendBlockUpdated(pos, state, state, 3);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            return removeRemote(level, pos, state, player)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            return removeRemote(level, pos, state, player)
                    ? InteractionResult.sidedSuccess(level.isClientSide)
                    : InteractionResult.PASS;
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
            boolean powered = level.hasNeighborSignal(pos);
            boolean wasPowered = state.getValue(POWERED);
            if (powered == wasPowered) {
                return;
            }

            if (powered && level.getBlockEntity(pos) instanceof RemotePedestalBlockEntity pedestal) {
                pedestal.onPowered();
                level.updateNeighbourForOutputSignal(pos, this);
            }

            level.setBlock(pos, state.setValue(POWERED, powered), 3);
            updateColorFromInkwell(level, pos);
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            updateColorFromInkwell(level, pos);
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            if (direction == Direction.DOWN && level instanceof Level actualLevel) {
                updateColorFromInkwell(actualLevel, currentPos);
            }
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RemotePedestalBlockEntity pedestal) {
                Containers.dropContents(level, pos, pedestal);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            return state.getValue(POWERED) && level.getBlockEntity(pos) instanceof RemotePedestalBlockEntity pedestal
                    ? pedestal.getSignal()
                    : 0;
        }

        @Override
        public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return true;
        }

        private boolean removeRemote(Level level, BlockPos pos, BlockState state, Player player) {
            if (!(level.getBlockEntity(pos) instanceof RemotePedestalBlockEntity pedestal) || pedestal.isEmpty()) {
                return false;
            }

            if (!level.isClientSide) {
                ItemStack remote = pedestal.removeItemNoUpdate(0);
                pedestal.setSignal(0);
                pedestal.setRemoteResult(0);
                if (!player.addItem(remote)) {
                    Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, remote);
                }
                level.sendBlockUpdated(pos, state, state, 3);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            return true;
        }

        private static void updateColorFromInkwell(Level level, BlockPos pos) {
            if (level.getBlockState(pos.below()).is(SplatcraftBlocks.INKWELL.get())
                    && level.getBlockEntity(pos.below()) instanceof InkColorBlockEntity inkwell
                    && level.getBlockEntity(pos) instanceof RemotePedestalBlockEntity pedestal) {
                pedestal.setColor(inkwell.effectiveColor());
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 2);
            }
        }
    }

    public static class DirectionPoweredBlock extends DirectionFacingBlock {
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

        public DirectionPoweredBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(DirectionPoweredBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(POWERED);
        }
    }

    public static class SplatSwitchBlockEntityBlock extends DirectionPoweredBlock implements EntityBlock {
        private static final VoxelShape[] SHAPES = new VoxelShape[]{
                Block.box(1.0D, 14.0D, 1.0D, 15.0D, 16.0D, 15.0D),
                Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.0D, 15.0D),
                Block.box(1.0D, 1.0D, 14.0D, 15.0D, 15.0D, 16.0D),
                Block.box(1.0D, 1.0D, 0.0D, 15.0D, 15.0D, 2.0D),
                Block.box(14.0D, 1.0D, 1.0D, 16.0D, 15.0D, 15.0D),
                Block.box(0.0D, 1.0D, 1.0D, 2.0D, 15.0D, 15.0D)
        };

        public SplatSwitchBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(SplatSwitchBlockEntityBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return waterloggedState(super.getStateForPlacement(context), context);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR.get().create(pos, state);
        }

        public static net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult activate(Level level, BlockPos pos, int color) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SplatSwitchBlockEntityBlock)
                    || !(level.getBlockEntity(pos) instanceof net.splatcraft.neoforge.blockentity.InkColorBlockEntity colorBlock)) {
                return net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult.FAIL;
            }

            int previousColor = colorBlock.getColor();
            boolean wasPowered = state.getValue(POWERED);
            colorBlock.setColor(color);
            if (!wasPowered) {
                level.setBlock(pos, state.setValue(POWERED, true), 3);
                playSound(level, pos, SplatcraftSounds.SPLAT_SWITCH_POWERED_ON.get());
            } else {
                level.sendBlockUpdated(pos, state, state, 2);
            }
            updateNeighbors(level, pos, state);
            return wasPowered && previousColor == color
                    ? net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult.ALREADY_INKED
                    : net.splatcraft.neoforge.worldink.InkBlockUtils.InkResult.SUCCESS;
        }

        public static boolean deactivate(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SplatSwitchBlockEntityBlock) || !state.getValue(POWERED)) {
                return false;
            }

            level.setBlock(pos, state.setValue(POWERED, false), 3);
            playSound(level, pos, SplatcraftSounds.SPLAT_SWITCH_POWERED_OFF.get());
            updateNeighbors(level, pos, state);
            return true;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES[state.getValue(FACING).ordinal()];
        }

        @Override
        protected BlockState updateShape(
                BlockState state,
                Direction direction,
                BlockState neighborState,
                LevelAccessor level,
                BlockPos currentPos,
                BlockPos neighborPos
        ) {
            scheduleWaterTick(state, level, currentPos);
            if (!level.isClientSide() && state.getValue(POWERED) && level instanceof Level actualLevel
                    && isTouchingInkClearingFluid(level, currentPos)) {
                deactivate(actualLevel, currentPos);
                return actualLevel.getBlockState(currentPos);
            }
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return fluidState(state, super.getFluidState(state));
        }

        @Override
        protected boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return true;
        }

        @Override
        protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return state.getValue(POWERED) ? 15 : 0;
        }

        @Override
        protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return state.getValue(POWERED) ? 15 : 0;
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && state.getValue(POWERED)) {
                updateNeighbors(level, pos, state);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        private static void updateNeighbors(Level level, BlockPos pos, BlockState state) {
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), state.getBlock());
        }

        private static void playSound(Level level, BlockPos pos, SoundEvent sound) {
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static class StageBarrierBlockEntityBlock extends Block implements EntityBlock {
        private static final VoxelShape COLLISION = Block.box(0.0D, 0.01D, 0.0D, 16.0D, 15.99D, 16.0D);

        public StageBarrierBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(StageBarrierBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.STAGE_BARRIER.get().create(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level,
                BlockState state,
                BlockEntityType<T> blockEntityType
        ) {
            return blockEntityType == SplatcraftBlockEntities.STAGE_BARRIER.get()
                    ? (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    StageBarrierBlockEntity.tick(tickerLevel, tickerPos, tickerState, (StageBarrierBlockEntity) blockEntity)
                    : null;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return COLLISION;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (context instanceof EntityCollisionContext entityContext
                    && entityContext.getEntity() instanceof Player player
                    && player.isCreative()) {
                return Shapes.block();
            }

            if (!(level.getBlockEntity(pos) instanceof StageBarrierBlockEntity barrier)) {
                return Shapes.block();
            }
            return barrier.getActiveTime() > 5 ? super.getShape(state, level, pos, context) : Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0F;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }
    }

    public static class StageVoidBlockEntityBlock extends StageBarrierBlockEntityBlock {
        public StageVoidBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(StageVoidBlockEntityBlock::new);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level,
                BlockState state,
                BlockEntityType<T> blockEntityType
        ) {
            return blockEntityType == SplatcraftBlockEntities.STAGE_BARRIER.get()
                    ? (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    tickStageVoid(tickerLevel, tickerPos, (StageBarrierBlockEntity) blockEntity)
                    : null;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            handleStageVoidEntity(level, pos, entity);
        }

        private static void tickStageVoid(Level level, BlockPos pos, StageBarrierBlockEntity barrier) {
            barrier.tickActiveTime();
            for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos))) {
                handleStageVoidEntity(level, pos, barrier, entity);
            }
        }

        private static void handleStageVoidEntity(Level level, BlockPos pos, Entity entity) {
            handleStageVoidEntity(level, pos, null, entity);
        }

        private static void handleStageVoidEntity(
                Level level,
                BlockPos pos,
                StageBarrierBlockEntity knownBarrier,
                Entity entity
        ) {
            if (entity instanceof SpawnShieldEntity) {
                return;
            }
            if (!isInsideStageVoid(pos, entity)) {
                return;
            }

            StageBarrierBlockEntity barrier = knownBarrier;
            if (barrier == null && level.getBlockEntity(pos) instanceof StageBarrierBlockEntity foundBarrier) {
                barrier = foundBarrier;
            }
            if (barrier != null) {
                barrier.resetActiveTime();
            }

            if (entity instanceof Player player) {
                if (!level.isClientSide) {
                    player.hurt(player.damageSources().source(SplatcraftDamageTypes.OUT_OF_STAGE), Float.MAX_VALUE);
                }
            } else {
                entity.discard();
            }
        }

        private static boolean isInsideStageVoid(BlockPos pos, Entity entity) {
            Vec3 position = entity.position();
            AABB bounds = entity.getBoundingBox();
            return position.x >= pos.getX()
                    && position.x < pos.getX() + 1.0D
                    && position.z >= pos.getZ()
                    && position.z < pos.getZ() + 1.0D
                    && bounds.maxY > pos.getY()
                    && bounds.minY < pos.getY() + 1.0D;
        }
    }

    public static class ColorBarrierBlockEntityBlock extends StageBarrierBlockEntityBlock {
        public ColorBarrierBlockEntityBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(ColorBarrierBlockEntityBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SplatcraftBlockEntities.COLOR_BARRIER.get().create(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return cloneWithColor(super.getCloneItemStack(level, pos, state), level, pos, state, true);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level,
                BlockState state,
                BlockEntityType<T> blockEntityType
        ) {
            return blockEntityType == SplatcraftBlockEntities.COLOR_BARRIER.get()
                    ? (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    ColoredBarrierBlockEntity.tick(tickerLevel, tickerPos, tickerState, (ColoredBarrierBlockEntity) blockEntity)
                    : null;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (!(context instanceof EntityCollisionContext entityContext)) {
                return super.getCollisionShape(state, level, pos, context);
            }

            if (level.getBlockEntity(pos) instanceof ColoredBarrierBlockEntity barrier
                    && entityContext.getEntity() != null
                    && barrier.canAllowThrough(entityContext.getEntity())) {
                return Shapes.empty();
            }
            return super.getCollisionShape(state, level, pos, context);
        }
    }

    public static class SixWayBlock extends Block {
        public SixWayBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any()
                    .setValue(PipeBlock.DOWN, true)
                    .setValue(PipeBlock.UP, false)
                    .setValue(PipeBlock.NORTH, false)
                    .setValue(PipeBlock.EAST, false)
                    .setValue(PipeBlock.SOUTH, false)
                    .setValue(PipeBlock.WEST, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(SixWayBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(PipeBlock.UP, PipeBlock.DOWN, PipeBlock.NORTH, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.EAST);
        }
    }

    public static class TransparentSixWayBlock extends TransparentBlock {
        public TransparentSixWayBlock(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any()
                    .setValue(PipeBlock.DOWN, true)
                    .setValue(PipeBlock.UP, false)
                    .setValue(PipeBlock.NORTH, false)
                    .setValue(PipeBlock.EAST, false)
                    .setValue(PipeBlock.SOUTH, false)
                    .setValue(PipeBlock.WEST, false));
        }

        @Override
        protected MapCodec<? extends TransparentBlock> codec() {
            return simpleCodec(TransparentSixWayBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(PipeBlock.UP, PipeBlock.DOWN, PipeBlock.NORTH, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.EAST);
        }
    }
}
