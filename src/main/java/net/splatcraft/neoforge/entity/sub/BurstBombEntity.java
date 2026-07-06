package net.splatcraft.neoforge.entity.sub;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.worldink.InkDamageUtils;

public class BurstBombEntity extends AbstractSubWeaponEntity {
    public BurstBombEntity(EntityType<? extends AbstractSubWeaponEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected int lifetimeTicks() {
        return 80;
    }

    @Override
    protected float explosionRadius() {
        return subFloat("explosion_size", 2.0F);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            InkDamageUtils.doSplatDamage(
                    level(),
                    target,
                    subFloat("direct_damage", 0.0F),
                    color(),
                    getOwner(),
                    this,
                    settings().stats().fullDamageToMobs());
        }
    }

    @Override
    protected Item getDefaultItem() {
        return SplatcraftItems.BURST_BOMB.get();
    }
}
