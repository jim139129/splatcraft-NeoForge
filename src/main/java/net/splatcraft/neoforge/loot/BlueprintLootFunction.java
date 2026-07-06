package net.splatcraft.neoforge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.splatcraft.neoforge.item.BlueprintItem;
import net.splatcraft.neoforge.registry.SplatcraftLootFunctions;

public class BlueprintLootFunction extends LootItemConditionalFunction {
    public static final MapCodec<BlueprintLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
            .and(Codec.STRING.optionalFieldOf("weapon_pool", "").forGetter(BlueprintLootFunction::weaponPool))
            .and(Codec.STRING.listOf().optionalFieldOf("advancements", List.of()).forGetter(BlueprintLootFunction::advancementIds))
            .apply(instance, BlueprintLootFunction::new));

    private final String weaponPool;
    private final List<String> advancementIds;

    public BlueprintLootFunction(List<LootItemCondition> conditions, String weaponPool, List<String> advancementIds) {
        super(conditions);
        this.weaponPool = weaponPool;
        this.advancementIds = List.copyOf(advancementIds);
    }

    @Override
    public LootItemFunctionType<BlueprintLootFunction> getType() {
        return SplatcraftLootFunctions.BLUEPRINT_POOL.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        BlueprintItem.setPoolFromWeaponType(stack, weaponPool);
        BlueprintItem.addToAdvancementPool(stack, advancementIds);
        return stack;
    }

    private String weaponPool() {
        return weaponPool;
    }

    private List<String> advancementIds() {
        return advancementIds;
    }
}
