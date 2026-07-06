package net.splatcraft.neoforge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.splatcraft.neoforge.registry.SplatcraftLootModifiers;

public class ChestLootModifier extends LootModifier {
    public static final MapCodec<ChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(modifier -> modifier.conditions),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ChestLootModifier::item),
            Codec.INT.fieldOf("countMin").forGetter(ChestLootModifier::countMin),
            Codec.INT.fieldOf("countMax").forGetter(ChestLootModifier::countMax),
            Codec.FLOAT.fieldOf("chance").forGetter(ChestLootModifier::chance),
            ResourceLocation.CODEC.fieldOf("parent").forGetter(ChestLootModifier::parentTable)
    ).apply(instance, ChestLootModifier::new));

    private final Item item;
    private final int countMin;
    private final int countMax;
    private final float chance;
    private final ResourceLocation parentTable;

    public ChestLootModifier(LootItemCondition[] conditions, Item item, int countMin, int countMax, float chance, ResourceLocation parentTable) {
        super(conditions);
        this.item = item;
        this.countMin = countMin;
        this.countMax = countMax;
        this.chance = chance;
        this.parentTable = parentTable;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!context.getQueriedLootTableId().equals(this.parentTable)) {
            return generatedLoot;
        }

        if (context.getRandom().nextFloat() <= this.chance) {
            generatedLoot.add(new ItemStack(this.item, this.randomCount(context)));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return SplatcraftLootModifiers.CHEST_LOOT.get();
    }

    private int randomCount(LootContext context) {
        return (this.countMax - this.countMin <= 0 ? 0 : context.getRandom().nextInt(this.countMax - this.countMin)) + this.countMin;
    }

    public Item item() {
        return this.item;
    }

    public int countMin() {
        return this.countMin;
    }

    public int countMax() {
        return this.countMax;
    }

    public float chance() {
        return this.chance;
    }

    public ResourceLocation parentTable() {
        return this.parentTable;
    }
}
