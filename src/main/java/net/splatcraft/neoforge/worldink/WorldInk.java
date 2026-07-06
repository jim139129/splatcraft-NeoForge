package net.splatcraft.neoforge.worldink;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.splatcraft.neoforge.player.PlayerInfo;

public class WorldInk implements INBTSerializable<CompoundTag> {
    private final Map<BlockPos, Entry> ink = new HashMap<>();
    private final Map<BlockPos, Entry> permanentInk = new HashMap<>();
    private final ChunkAccess chunk;

    public WorldInk(ChunkAccess chunk) {
        this.chunk = chunk;
    }

    public boolean isInked(BlockPos pos) {
        return getInk(pos) != null;
    }

    public Entry getInk(BlockPos pos) {
        return ink.get(localize(pos));
    }

    public Map<BlockPos, Entry> inkInChunk() {
        return Collections.unmodifiableMap(ink);
    }

    public boolean ink(BlockPos pos, int color, ResourceLocation type) {
        BlockPos localPos = localize(pos);
        Entry entry = new Entry(color, normalizeType(type));
        Entry previous = ink.put(localPos, entry);
        boolean changed = !Objects.equals(previous, entry);
        if (changed) {
            markUnsaved();
            sync(localPos, entry);
        }
        return changed;
    }

    public boolean clearInk(BlockPos pos) {
        return clearInk(pos, false);
    }

    public boolean clearInk(BlockPos pos, boolean removePermanent) {
        BlockPos localPos = localize(pos);
        if (removePermanent) {
            boolean changed = ink.remove(localPos) != null;
            changed = permanentInk.remove(localPos) != null || changed;
            if (changed) {
                markUnsaved();
                sync(localPos, null);
            }
            return changed;
        }

        Entry current = ink.get(localPos);
        if (current == null || Objects.equals(current, permanentInk.get(localPos))) {
            return false;
        }

        Entry permanent = permanentInk.get(localPos);
        if (permanent == null) {
            ink.remove(localPos);
        } else {
            ink.put(localPos, permanent);
        }
        markUnsaved();
        sync(localPos, permanent);
        return true;
    }

    public boolean setColor(BlockPos pos, int color) {
        BlockPos localPos = localize(pos);
        Entry current = ink.get(localPos);
        if (current == null || current.color() == color) {
            return false;
        }

        Entry replacement = new Entry(color, current.type());
        Entry permanent = permanentInk.get(localPos);
        ink.put(localPos, replacement);
        if (Objects.equals(current, permanent)) {
            permanentInk.put(localPos, replacement);
        }
        markUnsaved();
        sync(localPos, replacement);
        return true;
    }

    public boolean hasPermanentInk(BlockPos pos) {
        return getPermanentInk(pos) != null;
    }

    public Entry getPermanentInk(BlockPos pos) {
        return permanentInk.get(localize(pos));
    }

    public Map<BlockPos, Entry> permanentInkInChunk() {
        return Collections.unmodifiableMap(permanentInk);
    }

    public boolean setPermanentInk(BlockPos pos, int color, ResourceLocation type) {
        BlockPos localPos = localize(pos);
        Entry entry = new Entry(color, normalizeType(type));
        Entry previous = permanentInk.put(localPos, entry);
        Entry previousActive = ink.put(localPos, entry);
        boolean changed = !Objects.equals(previous, entry) || !Objects.equals(previousActive, entry);
        if (changed) {
            markUnsaved();
            sync(localPos, entry);
        }
        return changed;
    }

    public boolean removePermanentInk(BlockPos pos) {
        BlockPos localPos = localize(pos);
        if (permanentInk.remove(localPos) != null) {
            markUnsaved();
            sync(localPos, ink.get(localPos));
            return true;
        }
        return false;
    }

    public boolean replaceColor(int color, ReplacementFilter filter) {
        boolean changed = false;
        for (Map.Entry<BlockPos, Entry> entry : ink.entrySet()) {
            Entry inkEntry = entry.getValue();
            if (inkEntry.color() != color && filter.matches(inkEntry)) {
                entry.setValue(new Entry(color, inkEntry.type()));
                changed = true;
            }
        }

        for (Map.Entry<BlockPos, Entry> entry : permanentInk.entrySet()) {
            Entry inkEntry = entry.getValue();
            if (inkEntry.color() != color && filter.matches(inkEntry)) {
                entry.setValue(new Entry(color, inkEntry.type()));
                Entry activeInk = ink.get(entry.getKey());
                if (Objects.equals(activeInk, inkEntry)) {
                    ink.put(entry.getKey(), new Entry(color, inkEntry.type()));
                }
                changed = true;
            }
        }

        if (changed) {
            markUnsaved();
        }
        return changed;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("Ink", writeEntries(ink));
        tag.put("PermanentInk", writeEntries(permanentInk));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        permanentInk.clear();
        readEntries(tag.getList("PermanentInk", Tag.TAG_COMPOUND), permanentInk);

        ink.clear();
        readEntries(tag.getList("Ink", Tag.TAG_COMPOUND), ink);
    }

    private static ListTag writeEntries(Map<BlockPos, Entry> entries) {
        ListTag list = new ListTag();
        entries.forEach((pos, entry) -> {
            CompoundTag element = new CompoundTag();
            element.put("Pos", NbtUtils.writeBlockPos(pos));
            element.putInt("Color", entry.color());
            element.putString("Type", entry.type().toString());
            list.add(element);
        });
        return list;
    }

    private static void readEntries(ListTag list, Map<BlockPos, Entry> entries) {
        for (Tag tag : list) {
            if (tag instanceof CompoundTag element) {
                NbtUtils.readBlockPos(element, "Pos").ifPresent(pos -> {
                    ResourceLocation type = ResourceLocation.tryParse(element.getString("Type"));
                    entries.put(localize(pos), new Entry(element.getInt("Color"), normalizeType(type)));
                });
            }
        }
    }

    private void markUnsaved() {
        chunk.setUnsaved(true);
    }

    public void replaceSyncedInk(Map<BlockPos, Entry> syncedInk) {
        ink.clear();
        syncedInk.forEach((pos, entry) -> {
            if (entry != null) {
                ink.put(localize(pos), entry);
            }
        });
    }

    public void setSyncedInk(BlockPos pos, Entry entry) {
        BlockPos localPos = localize(pos);
        if (entry == null) {
            ink.remove(localPos);
        } else {
            ink.put(localPos, entry);
        }
    }

    private void sync(BlockPos localPos, Entry entry) {
        if (chunk.getLevel() instanceof ServerLevel level) {
            WorldInkSync.sendUpdate(level, chunk.getPos(), localPos, entry);
        }
    }

    private static BlockPos localize(BlockPos pos) {
        return new BlockPos(Math.floorMod(pos.getX(), 16), pos.getY(), Math.floorMod(pos.getZ(), 16));
    }

    private static ResourceLocation normalizeType(ResourceLocation type) {
        if (type == null) {
            return PlayerInfo.NORMAL_INK_TYPE;
        }
        return type;
    }

    public interface ReplacementFilter {
        boolean matches(Entry entry);
    }

    public record Entry(int color, ResourceLocation type) {
    }
}
