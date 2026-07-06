package net.splatcraft.neoforge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.critereon.FishingHookPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.splatcraft.neoforge.registry.SplatcraftLootModifiers;

public class FishingLootModifier extends LootModifier {
    public static final MapCodec<FishingLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(modifier -> modifier.conditions),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(FishingLootModifier::item),
            Codec.INT.fieldOf("countMin").forGetter(FishingLootModifier::countMin),
            Codec.INT.fieldOf("countMax").forGetter(FishingLootModifier::countMax),
            Codec.FLOAT.fieldOf("chance").forGetter(FishingLootModifier::chance),
            Codec.INT.fieldOf("quality").forGetter(FishingLootModifier::quality),
            Codec.BOOL.optionalFieldOf("isTreasure", false).forGetter(FishingLootModifier::isTreasure)
    ).apply(instance, FishingLootModifier::new));

    private final Item item;
    private final int countMin;
    private final int countMax;
    private final float chance;
    private final int quality;
    private final boolean isTreasure;

    public FishingLootModifier(
            LootItemCondition[] conditions,
            Item item,
            int countMin,
            int countMax,
            float chance,
            int quality,
            boolean isTreasure
    ) {
        super(conditions);
        this.item = item;
        this.countMin = countMin;
        this.countMax = countMax;
        this.chance = chance;
        this.quality = quality;
        this.isTreasure = isTreasure;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity hookedEntity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(hookedEntity instanceof FishingHook fishingHook) || this.isTreasure && !isOpenWater(fishingHook, context)) {
            return generatedLoot;
        }

        float chanceModifier = 0;
        Player player = fishingHook.getPlayerOwner();
        if (player != null) {
            ItemStack stack = context.getParamOrNull(LootContextParams.TOOL);
            int fishingLuck = stack == null ? 0 : EnchantmentHelper.getFishingLuckBonus(context.getLevel(), stack, player);
            float luck = player.getLuck();

            if (this.isTreasure) {
                chanceModifier += fishingLuck;
            }
            chanceModifier += luck;
            chanceModifier *= this.quality * (this.chance / 2);
        }

        if (context.getRandom().nextInt(100) <= (this.chance + chanceModifier) * 100) {
            if (generatedLoot.size() <= 1) {
                generatedLoot.clear();
            }
            generatedLoot.add(new ItemStack(this.item, this.randomCount(context)));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return SplatcraftLootModifiers.FISHING.get();
    }

    private static boolean isOpenWater(FishingHook fishingHook, LootContext context) {
        return FishingHookPredicate.inOpenWater(true).matches(fishingHook, context.getLevel(), Vec3.atCenterOf(fishingHook.blockPosition()));
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

    public int quality() {
        return this.quality;
    }

    public boolean isTreasure() {
        return this.isTreasure;
    }
}
