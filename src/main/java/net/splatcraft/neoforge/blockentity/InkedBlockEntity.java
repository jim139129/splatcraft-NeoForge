package net.splatcraft.neoforge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;

public class InkedBlockEntity extends InkColorBlockEntity {
    private BlockState savedState = Blocks.AIR.defaultBlockState();
    private int savedColor = -1;
    private int permanentColor = -1;
    private ResourceLocation permanentInkType = ResourceLocation.fromNamespaceAndPath("splatcraft", "normal");

    public InkedBlockEntity(BlockPos pos, BlockState state) {
        super(SplatcraftBlockEntities.INKED_BLOCK.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("SavedState", NbtUtils.writeBlockState(savedState));
        if (savedColor != -1) {
            tag.putInt("SavedColor", savedColor);
        }
        if (permanentColor != -1) {
            tag.putInt("PermanentColor", permanentColor);
            tag.putString("PermanentInkType", permanentInkType.toString());
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("SavedState", CompoundTag.TAG_COMPOUND)) {
            savedState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("SavedState"));
        }
        savedColor = tag.contains("SavedColor") ? tag.getInt("SavedColor") : -1;
        permanentColor = tag.contains("PermanentColor") ? tag.getInt("PermanentColor") : -1;
        if (tag.contains("PermanentInkType", CompoundTag.TAG_STRING)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("PermanentInkType"));
            if (parsed != null) {
                permanentInkType = parsed;
            }
        }
    }

    public BlockState getSavedState() {
        return savedState;
    }

    public void setSavedState(BlockState savedState) {
        this.savedState = savedState == null ? Blocks.AIR.defaultBlockState() : savedState;
        setChanged();
    }

    public int getSavedColor() {
        return savedColor;
    }

    public void setSavedColor(int savedColor) {
        this.savedColor = savedColor;
        setChanged();
    }

    public int getPermanentColor() {
        return permanentColor;
    }

    public void setPermanentColor(int permanentColor) {
        this.permanentColor = permanentColor;
        setChanged();
    }

    public ResourceLocation getPermanentInkType() {
        return permanentInkType;
    }

    public void setPermanentInkType(ResourceLocation permanentInkType) {
        this.permanentInkType = permanentInkType == null ? ResourceLocation.fromNamespaceAndPath("splatcraft", "normal") : permanentInkType;
        setChanged();
    }
}
