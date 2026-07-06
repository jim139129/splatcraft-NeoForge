package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.InkSquidEntity;
import net.splatcraft.neoforge.entity.InkProjectileEntity;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;
import net.splatcraft.neoforge.entity.SquidBumperEntity;
import net.splatcraft.neoforge.entity.sub.AbstractSubWeaponEntity;
import net.splatcraft.neoforge.entity.sub.BurstBombEntity;
import net.splatcraft.neoforge.entity.sub.CurlingBombEntity;
import net.splatcraft.neoforge.entity.sub.SplatBombEntity;
import net.splatcraft.neoforge.entity.sub.SuctionBombEntity;

public final class SplatcraftEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Splatcraft.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<InkSquidEntity>> INK_SQUID =
            inkSquid("ink_squid");
    public static final DeferredHolder<EntityType<?>, EntityType<InkProjectileEntity>> INK_PROJECTILE =
            projectile("ink_projectile");
    public static final DeferredHolder<EntityType<?>, EntityType<SquidBumperEntity>> SQUID_BUMPER =
            squidBumper("squid_bumper");
    public static final DeferredHolder<EntityType<?>, EntityType<SpawnShieldEntity>> SPAWN_SHIELD =
            spawnShield("spawn_shield");

    public static final DeferredHolder<EntityType<?>, EntityType<BurstBombEntity>> BURST_BOMB =
            subWeapon("burst_bomb", BurstBombEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SuctionBombEntity>> SUCTION_BOMB =
            subWeapon("suction_bomb", SuctionBombEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SplatBombEntity>> SPLAT_BOMB =
            subWeapon("splat_bomb", SplatBombEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CurlingBombEntity>> CURLING_BOMB =
            subWeapon("curling_bomb", CurlingBombEntity::new);

    private SplatcraftEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener(SplatcraftEntities::registerAttributes);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(INK_SQUID.get(), InkSquidEntity.createAttributes().build());
        event.put(SQUID_BUMPER.get(), SquidBumperEntity.createAttributes().build());
    }

    private static DeferredHolder<EntityType<?>, EntityType<InkSquidEntity>> inkSquid(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(InkSquidEntity::new, MobCategory.AMBIENT)
                .sized(0.6F, 0.5F)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name).toString()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<InkProjectileEntity>> projectile(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(InkProjectileEntity::new, MobCategory.MISC)
                .sized(0.35F, 0.35F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name).toString()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<SquidBumperEntity>> squidBumper(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(SquidBumperEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name).toString()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<SpawnShieldEntity>> spawnShield(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(SpawnShieldEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name).toString()));
    }

    private static <T extends AbstractSubWeaponEntity> DeferredHolder<EntityType<?>, EntityType<T>> subWeapon(
            String name,
            EntityType.EntityFactory<T> factory
    ) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(factory, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build(ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name).toString()));
    }
}
