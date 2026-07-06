package net.splatcraft.neoforge.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class InkOverlayInfo implements INBTSerializable<CompoundTag> {
    public static final StreamCodec<RegistryFriendlyByteBuf, InkOverlayInfo> STREAM_CODEC = StreamCodec.of(
            InkOverlayInfo::encode,
            InkOverlayInfo::decode
    );

    private int color = InkColorData.DEFAULT_COLOR;
    private float amount;
    private int woolColor = -1;
    private boolean inkproof;

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public float amount() {
        return amount;
    }

    public boolean setAmount(float amount) {
        float previous = this.amount;
        this.amount = Math.max(0.0F, amount);
        return previous != this.amount;
    }

    public boolean addAmount(float amount) {
        return setAmount(this.amount + amount);
    }

    public int woolColor() {
        return woolColor;
    }

    public void setWoolColor(int woolColor) {
        this.woolColor = woolColor < 0 ? -1 : woolColor & 0xFFFFFF;
    }

    public boolean inkproof() {
        return inkproof;
    }

    public void setInkproof(boolean inkproof) {
        this.inkproof = inkproof;
    }

    public boolean isDefault() {
        return amount <= 0.0F
                && woolColor < 0
                && !inkproof;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Color", color);
        tag.putFloat("Amount", amount);
        tag.putBoolean("Inkproof", inkproof);
        if (woolColor >= 0) {
            tag.putInt("WoolColor", woolColor);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("Color", Tag.TAG_STRING)) {
            color = InkColorData.resolve(tag.getString("Color")).orElse(InkColorData.DEFAULT_COLOR);
        } else if (tag.contains("Color", Tag.TAG_INT)) {
            color = tag.getInt("Color") & 0xFFFFFF;
        } else {
            color = InkColorData.DEFAULT_COLOR;
        }
        amount = Math.max(0.0F, tag.getFloat("Amount"));
        inkproof = tag.getBoolean("Inkproof");
        woolColor = tag.contains("WoolColor", Tag.TAG_INT) ? tag.getInt("WoolColor") & 0xFFFFFF : -1;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, InkOverlayInfo info) {
        buffer.writeInt(info.color);
        buffer.writeFloat(info.amount);
        buffer.writeInt(info.woolColor);
        buffer.writeBoolean(info.inkproof);
    }

    private static InkOverlayInfo decode(RegistryFriendlyByteBuf buffer) {
        InkOverlayInfo info = new InkOverlayInfo();
        info.setColor(buffer.readInt());
        info.setAmount(buffer.readFloat());
        info.setWoolColor(buffer.readInt());
        info.setInkproof(buffer.readBoolean());
        return info;
    }
}
