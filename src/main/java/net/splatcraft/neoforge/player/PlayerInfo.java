package net.splatcraft.neoforge.player;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.data.SplatcraftData;
import net.splatcraft.neoforge.registry.SplatcraftItems;

public class PlayerInfo implements INBTSerializable<CompoundTag> {
    public static final ResourceLocation NORMAL_INK_TYPE = id("normal");
    public static final ResourceLocation GLOWING_INK_TYPE = id("glowing");
    public static final ResourceLocation CLEAR_INK_TYPE = id("clear");
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerInfo> STREAM_CODEC = StreamCodec.of(
            PlayerInfo::encode,
            PlayerInfo::decode
    );

    private int color;
    private boolean squid;
    private boolean initialized;
    private ItemStack inkBand = ItemStack.EMPTY;

    public PlayerInfo() {
        this(InkColorData.DEFAULT_COLOR);
    }

    public PlayerInfo(int color) {
        this.color = color;
    }

    public static PlayerInfo randomStarter(RandomSource random) {
        List<Integer> starterColors = SplatcraftData.starterColors();
        int color = starterColors.isEmpty()
                ? InkColorData.DEFAULT_COLOR
                : starterColors.get(random.nextInt(starterColors.size()));
        return new PlayerInfo(color);
    }

    public PlayerInfo copy() {
        PlayerInfo copy = new PlayerInfo(color);
        copy.squid = squid;
        copy.initialized = initialized;
        copy.inkBand = inkBand.copy();
        return copy;
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isSquid() {
        return squid;
    }

    public void setSquid(boolean squid) {
        this.squid = squid;
    }

    public boolean initialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public ItemStack inkBand() {
        return inkBand;
    }

    public void setInkBand(ItemStack inkBand) {
        this.inkBand = inkBand == null ? ItemStack.EMPTY : inkBand.copyWithCount(1);
    }

    public ResourceLocation inkType() {
        if (inkBand.is(SplatcraftItems.SPLATFEST_BAND.get())) {
            return GLOWING_INK_TYPE;
        }
        if (inkBand.is(SplatcraftItems.CLEAR_INK_BAND.get())) {
            return CLEAR_INK_TYPE;
        }
        return NORMAL_INK_TYPE;
    }

    public boolean sameInkBand(ItemStack stack) {
        return ItemStack.matches(inkBand, stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Color", color);
        tag.putBoolean("IsSquid", squid);
        tag.putString("InkType", inkType().toString());
        tag.putBoolean("Initialized", initialized);
        if (!inkBand.isEmpty()) {
            tag.put("InkBand", inkBand.saveOptional(provider));
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("Color", Tag.TAG_STRING)) {
            color = InkColorData.resolve(tag.getString("Color")).orElse(InkColorData.DEFAULT_COLOR);
        } else if (tag.contains("Color", Tag.TAG_INT)) {
            color = tag.getInt("Color");
        } else {
            color = InkColorData.DEFAULT_COLOR;
        }

        squid = tag.getBoolean("IsSquid");
        initialized = tag.getBoolean("Initialized");
        inkBand = tag.contains("InkBand", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(provider, tag.getCompound("InkBand"))
                : ItemStack.EMPTY;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlayerInfo other)) {
            return false;
        }
        return color == other.color
                && squid == other.squid
                && initialized == other.initialized
                && ItemStack.matches(inkBand, other.inkBand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, squid, initialized, ItemStack.hashItemAndComponents(inkBand));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerInfo info) {
        buffer.writeInt(info.color);
        buffer.writeBoolean(info.squid);
        buffer.writeBoolean(info.initialized);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, info.inkBand);
    }

    private static PlayerInfo decode(RegistryFriendlyByteBuf buffer) {
        PlayerInfo info = new PlayerInfo(buffer.readInt());
        info.squid = buffer.readBoolean();
        info.initialized = buffer.readBoolean();
        info.inkBand = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        return info;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, path);
    }
}
