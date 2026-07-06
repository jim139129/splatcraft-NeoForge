package net.splatcraft.neoforge.blockentity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.neoforge.block.BlockStateCompatBlocks;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;

public class CrateBlockEntity extends InkColorBlockEntity implements Container {
    public static final float CRATE_MAX_HEALTH = 20.0F;
    public static final float SUNKEN_CRATE_MAX_HEALTH = 25.0F;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private float health;
    private float maxHealth;
    private ResourceLocation lootTable;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(SplatcraftBlockEntities.CRATE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("Health", health);
        tag.putFloat("MaxHealth", maxHealth);
        ContainerHelper.saveAllItems(tag, inventory, registries);
        if (lootTable != null) {
            tag.putString("LootTable", lootTable.toString());
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        float defaultMaxHealth = defaultMaxHealth(getBlockState());
        maxHealth = tag.contains("MaxHealth", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("MaxHealth") : defaultMaxHealth;
        if (maxHealth <= 0.0F) {
            maxHealth = defaultMaxHealth;
        }
        health = tag.contains("Health", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("Health") : maxHealth;
        if (health <= 0.0F || health > maxHealth) {
            health = maxHealth;
        }
        inventory.clear();
        ContainerHelper.loadAllItems(tag, inventory, registries);
        lootTable = null;
        if (tag.contains("LootTable", CompoundTag.TAG_STRING)) {
            lootTable = ResourceLocation.tryParse(tag.getString("LootTable"));
        }
    }

    @Override
    public int getContainerSize() {
        return hasLoot() ? 0 : inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int index) {
        return inventory.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (hasLoot()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ContainerHelper.removeItem(inventory, index, count);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (hasLoot()) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.takeItem(inventory, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (hasLoot()) {
            return;
        }
        inventory.set(index, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        inventory.clear();
        setChanged();
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = Math.max(0.0F, Math.min(health, maxHealth));
        setChanged();
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = Math.max(1.0F, maxHealth);
        if (health <= 0.0F || health > this.maxHealth) {
            health = this.maxHealth;
        }
        setChanged();
    }

    public ResourceLocation getLootTable() {
        return lootTable;
    }

    public void setLootTable(ResourceLocation lootTable) {
        this.lootTable = lootTable;
        setChanged();
    }

    public boolean hasLoot() {
        return lootTable != null;
    }

    public void resetHealth() {
        setHealth(maxHealth);
        setColor(-1);
        syncCrateState();
    }

    public boolean isDamaged() {
        return health < maxHealth;
    }

    public int stateIndex() {
        if (health >= maxHealth) {
            setColor(-1);
            return 0;
        }
        int state = 4 - Math.round(health * 4.0F / maxHealth);
        return Math.max(0, Math.min(4, state));
    }

    public void initializeHealth(BlockState state) {
        setMaxHealth(defaultMaxHealth(state));
        resetHealth();
    }

    public boolean ink(int color, float damage) {
        if (level == null || level.isClientSide || damage <= 0.0F) {
            return false;
        }

        setColor(color);
        setHealth(health - damage);
        if (health <= 0.0F) {
            dropInventory();
            level.destroyBlock(worldPosition, false);
        } else {
            syncCrateState();
        }
        return true;
    }

    public void dropInventory() {
        if (!(level instanceof ServerLevel serverLevel) || !level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            return;
        }

        for (ItemStack stack : getDrops(serverLevel)) {
            Block.popResource(serverLevel, worldPosition, stack);
        }
        inventory.clear();
        setChanged();
    }

    public List<ItemStack> getDrops(ServerLevel serverLevel) {
        if (!hasLoot()) {
            return inventory.stream()
                    .filter(stack -> !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
        }

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.BLOCK_STATE, getBlockState())
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, this)
                .create(LootContextParamSets.BLOCK);
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, lootTable);
        return serverLevel.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params);
    }

    private void syncCrateState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateCompatBlocks.CRATE_STATE)) {
            level.setBlock(worldPosition, state.setValue(BlockStateCompatBlocks.CRATE_STATE, stateIndex()), 3);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

    private static float defaultMaxHealth(BlockState state) {
        return state.is(SplatcraftBlocks.SUNKEN_CRATE.get()) ? SUNKEN_CRATE_MAX_HEALTH : CRATE_MAX_HEALTH;
    }
}
