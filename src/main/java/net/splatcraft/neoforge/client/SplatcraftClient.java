package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.OptionalInt;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.SplatcraftConfig;
import net.splatcraft.neoforge.blockentity.ColoredBarrierBlockEntity;
import net.splatcraft.neoforge.blockentity.InkColorBlockEntity;
import net.splatcraft.neoforge.blockentity.InkVatBlockEntity;
import net.splatcraft.neoforge.data.ClientStageCache;
import net.splatcraft.neoforge.data.ClientPlayerColors;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.item.ColoredBlockItem;
import net.splatcraft.neoforge.item.InkTankItem;
import net.splatcraft.neoforge.item.RemoteItem;
import net.splatcraft.neoforge.item.SubWeaponItem;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.network.payload.PlayerSkinOverlayPayload;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftEntities;
import net.splatcraft.neoforge.registry.SplatcraftGameRules;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.registry.SplatcraftMenuTypes;
import net.splatcraft.neoforge.registry.SplatcraftParticleTypes;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.player.SquidInkMovement;

@Mod(value = Splatcraft.MOD_ID, dist = Dist.CLIENT)
public final class SplatcraftClient {
    private static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/gui/widgets.png");
    private static ChargerChargingSound chargerChargingSound;
    private static RollerRollSound rollerRollSound;
    private static SplatlingChargingSound splatlingChargingSound;
    private static float inkHudTime;
    private static float previousInkPercent;
    private static float inkFlash;
    private static Boolean suppressedBobViewValue;
    private static final IClientItemExtensions WEAPON_CLIENT_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
            return weaponArmPose(entityLiving, hand, itemStack);
        }
    };
    private static final IClientItemExtensions SUB_WEAPON_CLIENT_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
            return weaponArmPose(entityLiving, hand, itemStack);
        }

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return SubWeaponItemRenderer.INSTANCE;
        }
    };

    public SplatcraftClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        PlayerSkinOverlayPayload.setClientHandler(SplatcraftClient::handlePlayerSkinOverlay);
        modEventBus.addListener(SplatcraftClient::onClientSetup);
        modEventBus.addListener(SplatcraftClient::registerEntityRenderers);
        modEventBus.addListener(SplatcraftClient::registerLayerDefinitions);
        modEventBus.addListener(SplatcraftClient::addEntityLayers);
        modEventBus.addListener(SplatcraftClient::registerMenuScreens);
        modEventBus.addListener(SplatcraftClient::registerBlockColors);
        modEventBus.addListener(SplatcraftClient::registerItemColors);
        modEventBus.addListener(SplatcraftClient::registerParticleProviders);
        modEventBus.addListener(SplatcraftClient::registerKeyMappings);
        modEventBus.addListener(SplatcraftClient::registerAdditionalModels);
        modEventBus.addListener(SplatcraftClient::registerClientExtensions);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onComputeFovModifier);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onMovementInput);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onClientLoggingIn);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onClientLoggingOut);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onRenderArm);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onRenderFramePre);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onRenderFramePost);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onRenderNameTag);
        NeoForge.EVENT_BUS.addListener(SplatcraftClient::onClientChat);
        NeoForge.EVENT_BUS.addListener(WorldInkRenderer::onRenderLevelStage);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.CLEAR_INKED_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.EMPTY_INKWELL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.INKWELL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.INK_STAINED_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.INK_STAINED_GLASS_PANE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.SPAWN_PAD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.GRATE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.GRATE_RAMP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.CRATE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.SUNKEN_CRATE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.AMMO_KNIGHTS_DEBRIS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.REMOTE_PEDESTAL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.SPLAT_SWITCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.TARP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.GLASS_COVER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.STAGE_BARRIER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.STAGE_VOID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.ALLOWED_COLOR_BARRIER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SplatcraftBlocks.DENIED_COLOR_BARRIER.get(), RenderType.translucent());
            registerItemProperties();
        });
    }

    private static void handlePlayerSkinOverlay(PlayerSkinOverlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerInkColoredSkinLayer.apply(payload.playerId(), payload.imageBytes()));
    }

    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SplatcraftEntities.INK_SQUID.get(), InkSquidRenderer::new);
        event.registerEntityRenderer(SplatcraftEntities.INK_PROJECTILE.get(), InkProjectileRenderer::new);
        event.registerEntityRenderer(SplatcraftEntities.SQUID_BUMPER.get(), SquidBumperRenderer::new);
        event.registerEntityRenderer(SplatcraftEntities.SPAWN_SHIELD.get(), SpawnShieldRenderer::new);
        event.registerEntityRenderer(SplatcraftEntities.SPLAT_BOMB.get(), SplatBombRenderer::new);
        event.registerEntityRenderer(SplatcraftEntities.BURST_BOMB.get(), BurstBombRenderer::new);
        event.registerEntityRenderer(SplatcraftEntities.SUCTION_BOMB.get(), SuctionBombRenderer::new);
        event.registerEntityRenderer(SplatcraftEntities.CURLING_BOMB.get(), CurlingBombRenderer::new);
        event.registerBlockEntityRenderer(SplatcraftBlockEntities.STAGE_BARRIER.get(), StageBarrierRenderer::new);
        event.registerBlockEntityRenderer(SplatcraftBlockEntities.COLOR_BARRIER.get(), StageBarrierRenderer::new);
        event.registerBlockEntityRenderer(SplatcraftBlockEntities.REMOTE_PEDESTAL.get(), RemotePedestalRenderer::new);
    }

    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(InkSquidModel.LAYER_LOCATION, InkSquidModel::createBodyLayer);
        event.registerLayerDefinition(SquidBumperModel.LAYER_LOCATION, SquidBumperModel::createBodyLayer);
        event.registerLayerDefinition(InkProjectileModel.LAYER_LOCATION, InkProjectileModel::createBodyLayer);
        event.registerLayerDefinition(ShooterInkProjectileModel.LAYER_LOCATION, ShooterInkProjectileModel::createBodyLayer);
        event.registerLayerDefinition(BlasterInkProjectileModel.LAYER_LOCATION, BlasterInkProjectileModel::createBodyLayer);
        event.registerLayerDefinition(RollerInkProjectileModel.LAYER_LOCATION, RollerInkProjectileModel::createBodyLayer);
        event.registerLayerDefinition(SplatBombModel.LAYER_LOCATION, SplatBombModel::createBodyLayer);
        event.registerLayerDefinition(BurstBombModel.LAYER_LOCATION, BurstBombModel::createBodyLayer);
        event.registerLayerDefinition(SuctionBombModel.LAYER_LOCATION, SuctionBombModel::createBodyLayer);
        event.registerLayerDefinition(CurlingBombModel.LAYER_LOCATION, CurlingBombModel::createBodyLayer);
        event.registerLayerDefinition(InkTankModel.LAYER_LOCATION, InkTankModel::createBodyLayer);
        event.registerLayerDefinition(ClassicInkTankModel.LAYER_LOCATION, ClassicInkTankModel::createBodyLayer);
        event.registerLayerDefinition(InkTankJrModel.LAYER_LOCATION, InkTankJrModel::createBodyLayer);
        event.registerLayerDefinition(ArmoredInkTankModel.LAYER_LOCATION, ArmoredInkTankModel::createBodyLayer);
    }

    static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        for (var entityType : event.getEntityTypes()) {
            addInkOverlayLayer(event.getRenderer(entityType));
        }
        for (var skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            addInkOverlayLayer(renderer);
            addPlayerInkSkinLayer(renderer, event, skin);
            addInkAccessoryLayer(renderer, event);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addInkOverlayLayer(EntityRenderer<?> renderer) {
        if (renderer instanceof LivingEntityRenderer livingRenderer) {
            livingRenderer.addLayer(new InkOverlayLayer<>((LivingEntityRenderer<? extends LivingEntity, ?>) livingRenderer));
        }
    }

    @SuppressWarnings("unchecked")
    private static void addPlayerInkSkinLayer(EntityRenderer<?> renderer, EntityRenderersEvent.AddLayers event, PlayerSkin.Model skin) {
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            boolean slim = skin == PlayerSkin.Model.SLIM;
            PlayerModel<AbstractClientPlayer> model = new PlayerModel<>(
                    event.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER),
                    slim);
            ((LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) livingRenderer)
                    .addLayer(new PlayerInkColoredSkinLayer(
                            (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) livingRenderer,
                            model));
        }
    }

    @SuppressWarnings("unchecked")
    private static void addInkAccessoryLayer(EntityRenderer<?> renderer, EntityRenderersEvent.AddLayers event) {
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            HumanoidModel<AbstractClientPlayer> model = new HumanoidModel<>(
                    event.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
            ((LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) livingRenderer)
                    .addLayer(new InkAccessoryLayer(
                            (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) livingRenderer,
                            model));
        }
    }

    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SplatcraftMenuTypes.INK_VAT.get(), InkVatScreen::new);
        event.register(SplatcraftMenuTypes.WEAPON_WORKBENCH.get(), WeaponWorkbenchScreen::new);
    }

    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                SplatcraftClient::coloredBlockTint,
                SplatcraftBlocks.INKED_BLOCK.get(),
                SplatcraftBlocks.GLOWING_INKED_BLOCK.get(),
                SplatcraftBlocks.CLEAR_INKED_BLOCK.get(),
                SplatcraftBlocks.CORALITE.get(),
                SplatcraftBlocks.CORALITE_SLAB.get(),
                SplatcraftBlocks.CORALITE_STAIRS.get(),
                SplatcraftBlocks.INK_VAT.get(),
                SplatcraftBlocks.REMOTE_PEDESTAL.get(),
                SplatcraftBlocks.INKWELL.get(),
                SplatcraftBlocks.INK_STAINED_WOOL.get(),
                SplatcraftBlocks.INK_STAINED_CARPET.get(),
                SplatcraftBlocks.INK_STAINED_GLASS.get(),
                SplatcraftBlocks.INK_STAINED_GLASS_PANE.get(),
                SplatcraftBlocks.CANVAS.get(),
                SplatcraftBlocks.SPLAT_SWITCH.get(),
                SplatcraftBlocks.SPAWN_PAD.get(),
                SplatcraftBlocks.CRATE.get(),
                SplatcraftBlocks.SUNKEN_CRATE.get(),
                SplatcraftBlocks.ALLOWED_COLOR_BARRIER.get(),
                SplatcraftBlocks.DENIED_COLOR_BARRIER.get());
    }

    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        Item[] inkColoredItems = SplatcraftItems.inkColoredItems().stream()
                .map(holder -> (Item) holder.get())
                .toArray(Item[]::new);
        event.register(SplatcraftClient::inkColorTint, inkColoredItems);
    }

    private static int inkColorTint(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return -1;
        }

        int color = itemInkColor(stack);
        if (color < 0) {
            return -1;
        }

        if (!SplatcraftConfig.COLOR_LOCK.get() && InkColorComponent.isInverted(stack)) {
            color = 0xFFFFFF - color;
        }
        return ClientInkColors.visibleArgb(color);
    }

    private static int itemInkColor(ItemStack stack) {
        OptionalInt color = InkColorComponent.color(stack);
        if (color.isPresent()) {
            return color.getAsInt();
        }

        if (InkColorComponent.isColorLocked(stack)) {
            return -1;
        }

        boolean usesPlayerColorFallback = stack.is(SplatcraftTags.Items.INK_BANDS)
                || !stack.is(SplatcraftTags.Items.MATCH_ITEMS);
        Player player = Minecraft.getInstance().player;
        return usesPlayerColorFallback && player != null
                ? SplatcraftPlayerInfoEvents.color(player)
                : -1;
    }

    private static void registerItemProperties() {
        ResourceLocation active = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "active");
        ResourceLocation colored = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "colored");
        ResourceLocation inked = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "inked");
        ResourceLocation ink = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "ink");
        ResourceLocation isLeft = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "is_left");
        ResourceLocation mode = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "mode");
        ResourceLocation unfolded = ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "unfolded");
        registerRemoteProperties(SplatcraftItems.COLOR_CHANGER.get(), active, mode);
        registerRemoteProperties(SplatcraftItems.INK_DISRUPTOR.get(), active, mode);
        registerRemoteProperties(SplatcraftItems.TURF_SCANNER.get(), active, mode);
        registerDualieHandProperty(SplatcraftItems.SPLAT_DUALIES.get(), isLeft);
        registerDualieHandProperty(SplatcraftItems.ENPERRY_SPLAT_DUALIES.get(), isLeft);
        registerDualieHandProperty(SplatcraftItems.DUALIE_SQUELCHERS.get(), isLeft);
        registerDualieHandProperty(SplatcraftItems.GLOOGA_DUALIES.get(), isLeft);
        registerDualieHandProperty(SplatcraftItems.GLOOGA_DUALIES_DECO.get(), isLeft);
        registerDualieHandProperty(SplatcraftItems.KENSA_GLOOGA_DUALIES.get(), isLeft);
        registerRollerUnfoldedProperty(SplatcraftItems.SPLAT_ROLLER.get(), unfolded);
        registerRollerUnfoldedProperty(SplatcraftItems.KRAK_ON_SPLAT_ROLLER.get(), unfolded);
        registerRollerUnfoldedProperty(SplatcraftItems.COROCORO_SPLAT_ROLLER.get(), unfolded);
        registerRollerUnfoldedProperty(SplatcraftItems.CARBON_ROLLER.get(), unfolded);
        registerRollerUnfoldedProperty(SplatcraftItems.DYNAMO_ROLLER.get(), unfolded);
        registerRollerUnfoldedProperty(SplatcraftItems.INKBRUSH.get(), unfolded);
        registerRollerUnfoldedProperty(SplatcraftItems.OCTOBRUSH.get(), unfolded);
        registerRollerUnfoldedProperty(SplatcraftItems.KENSA_OCTOBRUSH.get(), unfolded);
        registerInkTankProperty(SplatcraftItems.INK_TANK.get(), ink);
        registerInkTankProperty(SplatcraftItems.CLASSIC_INK_TANK.get(), ink);
        registerInkTankProperty(SplatcraftItems.INK_TANK_JR.get(), ink);
        registerInkTankProperty(SplatcraftItems.ARMORED_INK_TANK.get(), ink);
        ItemProperties.register(SplatcraftItems.CANVAS.get(), inked, (stack, level, entity, seed) -> ColoredBlockItem.hasColor(stack));
        ItemProperties.register(SplatcraftItems.CORALITE.get(), colored, (stack, level, entity, seed) -> ColoredBlockItem.hasColor(stack));
        ItemProperties.register(SplatcraftItems.CORALITE_SLAB.get(), colored, (stack, level, entity, seed) -> ColoredBlockItem.hasColor(stack));
        ItemProperties.register(SplatcraftItems.CORALITE_STAIRS.get(), colored, (stack, level, entity, seed) -> ColoredBlockItem.hasColor(stack));
    }

    private static void registerRemoteProperties(Item item, ResourceLocation active, ResourceLocation mode) {
        ItemProperties.register(
                item,
                active,
                (stack, level, entity, seed) -> RemoteItem.hasCoordSet(stack) ? 1.0F : 0.0F);
        ItemProperties.register(item, mode, (stack, level, entity, seed) -> RemoteItem.mode(stack));
    }

    private static void registerDualieHandProperty(Item item, ResourceLocation isLeft) {
        ItemProperties.register(item, isLeft, (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            boolean mainLeft = entity.getMainArm() == HumanoidArm.LEFT;
            boolean mainHand = entity.getMainHandItem().equals(stack);
            boolean offhand = entity.getOffhandItem().equals(stack);
            return (mainLeft && mainHand) || (!mainLeft && offhand) ? 1.0F : 0.0F;
        });
    }

    private static void registerRollerUnfoldedProperty(Item item, ResourceLocation unfolded) {
        ItemProperties.register(item, unfolded, (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            if (entity.isUsingItem() && entity.getUseItem().equals(stack)) {
                return 1.0F;
            }
            boolean held = entity.getMainHandItem().equals(stack) || entity.getOffhandItem().equals(stack);
            return held && entity instanceof Player player
                    && player.getCooldowns().isOnCooldown(stack.getItem())
                    ? 1.0F
                    : 0.0F;
        });
    }

    private static void registerInkTankProperty(Item item, ResourceLocation ink) {
        ItemProperties.register(item, ink, (stack, level, entity, seed) -> {
            if (!(stack.getItem() instanceof InkTankItem tank) || tank.capacity() <= 0) {
                return 0.0F;
            }
            return InkTankItem.getInkAmount(stack) / tank.capacity();
        });
    }

    private static int coloredBlockTint(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        if (tintIndex != 0 || level == null || pos == null) {
            return -1;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        int color = -1;
        if (blockEntity instanceof InkColorBlockEntity colorBlock) {
            color = colorBlock.effectiveColor();
        } else if (blockEntity instanceof ColoredBarrierBlockEntity barrier) {
            color = barrier.getColor();
            if (color >= 0 && barrier.isInverted()) {
                color = 0xFFFFFF - color;
            }
        } else if (blockEntity instanceof InkVatBlockEntity inkVat) {
            color = inkVat.getColor();
        }
        return color >= 0 ? ClientInkColors.visibleColor(color) : 0xFFFFFF;
    }

    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SplatcraftParticleTypes.INK_SPLASH.get(), InkSplashParticle.Provider::new);
        event.registerSpriteSet(SplatcraftParticleTypes.INK_EXPLOSION.get(), InkExplosionParticle.Provider::new);
        event.registerSpriteSet(SplatcraftParticleTypes.SQUID_SOUL.get(), SquidSoulParticle.Provider::new);
        event.registerSpriteSet(SplatcraftParticleTypes.INK_TERRAIN.get(), sprites -> new InkTerrainParticle.Provider());
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        SplatcraftKeyHandler.registerKeyMappings(event);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        SubWeaponItemRenderer.registerGuiModels(event);
    }

    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Item[] poseItems = SplatcraftItems.weaponTabItems().stream()
                .map(holder -> holder.get())
                .filter(item -> item instanceof WeaponItem && !(item instanceof SubWeaponItem<?>))
                .toArray(Item[]::new);
        if (poseItems.length > 0) {
            event.registerItem(WEAPON_CLIENT_EXTENSIONS, poseItems);
        }
        event.registerItem(
                SUB_WEAPON_CLIENT_EXTENSIONS,
                SplatcraftItems.SPLAT_BOMB.get(),
                SplatcraftItems.SPLAT_BOMB_2.get(),
                SplatcraftItems.BURST_BOMB.get(),
                SplatcraftItems.SUCTION_BOMB.get(),
                SplatcraftItems.CURLING_BOMB.get());
        event.registerItem(
                inkTankArmorExtensions(InkTankModel.LAYER_LOCATION, InkTankModel::new),
                SplatcraftItems.INK_TANK.get());
        event.registerItem(
                inkTankArmorExtensions(ClassicInkTankModel.LAYER_LOCATION, ClassicInkTankModel::new),
                SplatcraftItems.CLASSIC_INK_TANK.get());
        event.registerItem(
                inkTankArmorExtensions(InkTankJrModel.LAYER_LOCATION, InkTankJrModel::new),
                SplatcraftItems.INK_TANK_JR.get());
        event.registerItem(
                inkTankArmorExtensions(ArmoredInkTankModel.LAYER_LOCATION, ArmoredInkTankModel::new),
                SplatcraftItems.ARMORED_INK_TANK.get());
    }

    private static IClientItemExtensions inkTankArmorExtensions(
            ModelLayerLocation layerLocation,
            Function<ModelPart, AbstractInkTankModel> modelFactory) {
        return new IClientItemExtensions() {
            private AbstractInkTankModel model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity entityLiving,
                    ItemStack itemStack,
                    EquipmentSlot armorSlot,
                    HumanoidModel<?> defaultModel) {
                if (!(itemStack.getItem() instanceof InkTankItem tank)) {
                    return defaultModel;
                }
                if (model == null) {
                    model = modelFactory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(layerLocation));
                }
                float inkPercent = tank.capacity() <= 0 ? 0.0F : InkTankItem.getInkAmount(itemStack) / tank.capacity();
                model.prepareForRender(inkPercent, armorSlot, defaultModel);
                return model;
            }
        };
    }

    private static HumanoidModel.ArmPose weaponArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
        if (entityLiving instanceof Player player && SplatcraftPlayerInfoEvents.isSquid(player)) {
            return null;
        }
        if (itemStack.getItem() instanceof SubWeaponItem<?> && isUsingStack(entityLiving, hand, itemStack)) {
            return HumanoidModel.ArmPose.THROW_SPEAR;
        }
        if (!(itemStack.getItem() instanceof WeaponItem weapon) || !hasActiveWeaponPose(entityLiving, hand, itemStack)) {
            return null;
        }

        return switch (weapon.weaponClass()) {
            case CHARGER -> HumanoidModel.ArmPose.BOW_AND_ARROW;
            case SPLATLING -> HumanoidModel.ArmPose.CROSSBOW_HOLD;
            default -> null;
        };
    }

    private static boolean hasActiveWeaponPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
        return isUsingStack(entityLiving, hand, itemStack) || WeaponItem.hasActiveChargeState(itemStack);
    }

    private static boolean isUsingStack(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
        return entityLiving.isUsingItem()
                && entityLiving.getUsedItemHand() == hand
                && entityLiving.getUseItem().equals(itemStack);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        SplatcraftKeyHandler.onClientTick(event);
        updateChargerChargingSound();
        updateRollerRollSound();
        updateSplatlingChargingSound();
    }

    private static void updateChargerChargingSound() {
        Player player = Minecraft.getInstance().player;
        if (!ChargerChargingSound.shouldPlay(player)) {
            stopChargerChargingSound();
            return;
        }

        if (chargerChargingSound == null || chargerChargingSound.isStopped()) {
            chargerChargingSound = new ChargerChargingSound(player);
            Minecraft.getInstance().getSoundManager().play(chargerChargingSound);
        }
    }

    private static void updateSplatlingChargingSound() {
        Player player = Minecraft.getInstance().player;
        if (!SplatlingChargingSound.shouldPlay(player)) {
            stopSplatlingChargingSound();
            return;
        }

        boolean secondLevel = SplatlingChargingSound.usesSecondLevel(player);
        if (splatlingChargingSound == null
                || splatlingChargingSound.isStopped()
                || splatlingChargingSound.isSecondLevel() != secondLevel) {
            stopSplatlingChargingSound();
            splatlingChargingSound = new SplatlingChargingSound(player, secondLevel);
            Minecraft.getInstance().getSoundManager().play(splatlingChargingSound);
        }
    }

    private static void updateRollerRollSound() {
        Player player = Minecraft.getInstance().player;
        if (!RollerRollSound.shouldPlay(player)) {
            stopRollerRollSound();
            return;
        }

        if (rollerRollSound == null || rollerRollSound.isStopped()) {
            rollerRollSound = new RollerRollSound(player, RollerRollSound.isBrush(player));
            Minecraft.getInstance().getSoundManager().play(rollerRollSound);
        }
    }

    private static void stopChargerChargingSound() {
        if (chargerChargingSound != null) {
            chargerChargingSound.stopSound();
            chargerChargingSound = null;
        }
    }

    private static void stopRollerRollSound() {
        if (rollerRollSound != null) {
            rollerRollSound.stopSound();
            rollerRollSound = null;
        }
    }

    private static void stopSplatlingChargingSound() {
        if (splatlingChargingSound != null) {
            splatlingChargingSound.stopSound();
            splatlingChargingSound = null;
        }
    }

    static void onMovementInput(MovementInputUpdateEvent event) {
        SquidMovementInputHandler.onMovementInput(event);
    }

    static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        PlayerInkColoredSkinLayer.uploadLocalOverlay(event.getPlayer());
    }

    static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SplatcraftGameRules.resetClientValues();
        ClientPlayerColors.clear();
        ClientStageCache.clear();
        PlayerInkColoredSkinLayer.clearTextures();
        stopChargerChargingSound();
        stopRollerRollSound();
        stopSplatlingChargingSound();
    }

    static void onRenderArm(RenderArmEvent event) {
        PlayerInkColoredSkinLayer.renderHand(event);
    }

    private static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (shouldSuppressSquidViewEffects(event.getPlayer())) {
            event.setNewFovModifier(1.0F);
        }
    }

    private static void onRenderFramePre(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !shouldSuppressSquidViewEffects(minecraft.player)) {
            restoreBobView(minecraft);
            return;
        }

        if (suppressedBobViewValue == null) {
            suppressedBobViewValue = minecraft.options.bobView().get();
        }
        if (minecraft.options.bobView().get()) {
            minecraft.options.bobView().set(false);
        }
    }

    private static void onRenderFramePost(RenderFrameEvent.Post event) {
        restoreBobView(Minecraft.getInstance());
    }

    private static boolean shouldSuppressSquidViewEffects(Player player) {
        if (!SplatcraftPlayerInfoEvents.isSquid(player)) {
            return false;
        }
        return switch (SplatcraftConfig.PREVENT_BOB_VIEW.get()) {
            case ALWAYS -> true;
            case SUBMERGED -> SquidInkMovement.canSquidHide(player);
            case OFF -> false;
        };
    }

    private static void restoreBobView(Minecraft minecraft) {
        if (suppressedBobViewValue != null) {
            minecraft.options.bobView().set(suppressedBobViewValue);
            suppressedBobViewValue = null;
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.isSpectator()) {
            resetInkHud();
            return;
        }

        renderChargeHud(event.getGuiGraphics(), player);
        if (crosshairInkIndicatorEnabled()) {
            renderCrosshairInkIndicator(event.getGuiGraphics(), player, event.getPartialTick().getRealtimeDeltaTicks());
        } else {
            resetInkHud();
        }
    }

    private static void renderChargeHud(GuiGraphics guiGraphics, Player player) {
        ItemStack stack = chargeHudStack(player);
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return;
        }

        int x = guiGraphics.guiWidth() / 2 - 15;
        int y = guiGraphics.guiHeight() / 2 + 14;
        float charge = Mth.clamp(weapon.hudCharge(player, stack), 0.0F, weapon.hudMaxCharge());

        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(WIDGETS, x, y, 88, 0, 30, 9);

        if (charge > 1.0F) {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.25F);
            guiGraphics.blit(WIDGETS, x, y, 88, 9, 30, 9);
            charge -= 1.0F;
        }

        renderChargeFill(guiGraphics, x, y, charge);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ItemStack chargeHudStack(Player player) {
        ItemStack usingStack = player.getUseItem();
        if (isChargeHudWeapon(usingStack)) {
            return usingStack;
        }
        if (isChargeHudWeapon(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        return isChargeHudWeapon(player.getOffhandItem()) ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    private static boolean isChargeHudWeapon(ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return false;
        }
        return weapon.weaponClass() == WeaponItem.WeaponClass.CHARGER
                || weapon.weaponClass() == WeaponItem.WeaponClass.SPLATLING;
    }

    private static void renderChargeFill(GuiGraphics guiGraphics, int x, int y, float charge) {
        int fillWidth = Mth.clamp((int)(30.0F * Mth.clamp(charge, 0.0F, 1.0F)), 0, 30);
        if (fillWidth <= 0) {
            return;
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(WIDGETS, x, y, 88, 9, fillWidth, 9);
    }

    private static void renderCrosshairInkIndicator(GuiGraphics guiGraphics, Player player, float deltaTicks) {
        boolean squid = SplatcraftPlayerInfoEvents.isSquid(player);
        boolean holdingMatchItem = isHoldingMatchItem(player);
        ItemStack tankStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasTank = tankStack.getItem() instanceof InkTankItem;
        boolean unlimitedInk = hasUnlimitedInk(player);
        float inkPercent = inkPercent(tankStack, unlimitedInk);
        boolean canUseHeldItem = !hasTank || !holdingMatchItem || canUseHeldMatchItem(player, tankStack);
        boolean showLowInkWarning = SplatcraftConfig.LOW_INK_WARNING.get()
                && (holdingMatchItem || squid)
                && !hasInkAmount(tankStack, unlimitedInk, 10.0F);

        if (!squid && !showLowInkWarning && canUseHeldItem) {
            resetInkHud();
            previousInkPercent = inkPercent;
            return;
        }

        inkHudTime += Math.max(0.0F, deltaTicks);
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int x = width / 2 + 9;
        int y = height / 2 - 9;
        float speed = 0.15F;
        int heightAnim = Math.min(14, (int)(inkHudTime * speed));
        int glowAnim = Mth.clamp((int)(inkHudTime * speed) - 16, 0, 18);
        int color = ClientInkColors.visibleColor(SplatcraftPlayerInfoEvents.color(player));

        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose();
        if (unlimitedInk) {
            renderUnlimitedInkIndicator(guiGraphics, x, y, heightAnim, glowAnim, color);
        } else {
            renderTankInkIndicator(guiGraphics, x, y, heightAnim, glowAnim, color, inkPercent, showLowInkWarning, canUseHeldItem);
        }
        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        previousInkPercent = inkPercent;
    }

    private static void renderUnlimitedInkIndicator(GuiGraphics guiGraphics, int x, int y, int heightAnim, int glowAnim, int color) {
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(WIDGETS, x, y + 14 - heightAnim, 0, 131, 18, 2);
        guiGraphics.blit(WIDGETS, x, y + 14 - heightAnim, 0, 131, 18, 4 + heightAnim);

        setGuiColor(guiGraphics, color, 1.0F, 0.0F);
        guiGraphics.blit(WIDGETS, x, y + 14 - heightAnim, 18, 131, 18, 4 + heightAnim);
        if (glowAnim > 0) {
            guiGraphics.blit(WIDGETS, x + 18 - glowAnim, y, 18 - glowAnim, 149, glowAnim, 18);
        }
    }

    private static void renderTankInkIndicator(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int heightAnim,
            int glowAnim,
            int color,
            float inkPercent,
            boolean showLowInkWarning,
            boolean canUseHeldItem) {
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(WIDGETS, x, y + 14 - heightAnim, 0, 95, 18, 2);
        guiGraphics.blit(WIDGETS, x, y + 14 - heightAnim, 0, 95, 18, 4 + heightAnim);

        if (inkPercent != previousInkPercent && inkPercent >= 1.0F) {
            inkFlash = 0.2F;
        }
        inkFlash = Math.max(0.0F, inkFlash - 0.0004F);

        float inkPercentLerp = Mth.lerp(0.05F, previousInkPercent, inkPercent);
        int visibleHeight = Mth.clamp((int)((4 + heightAnim) * inkPercentLerp), 0, 18);
        if (visibleHeight > 0) {
            float emptyPixels = (1.0F - inkPercent) * 18.0F;
            int fillY = (int)(y + 14 - heightAnim + (1.0F - inkPercentLerp) * 18.0F);
            if (SplatcraftConfig.VANILLA_INK_DURABILITY_COLOR.get()) {
                setGuiColor(guiGraphics, Mth.hsvToArgb(Math.max(0.0F, inkPercentLerp) / 3.0F, 1.0F, 1.0F, 255), 1.0F, 0.0F);
            } else {
                setGuiColor(guiGraphics, color, 1.0F, inkFlash);
            }
            guiGraphics.blit(WIDGETS, x, fillY, 18, visibleHeight, 18.0F, 95.0F + emptyPixels, 18, visibleHeight, 256, 256);
        }

        if (glowAnim > 0) {
            setGuiColor(guiGraphics, color, 1.0F, 0.0F);
            guiGraphics.blit(WIDGETS, x + 18 - glowAnim, y, 18 - glowAnim, 113, glowAnim, 18);
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (glowAnim == 18) {
            if (!canUseHeldItem) {
                guiGraphics.blit(WIDGETS, x, y, 36, 112, 18, 18);
            } else if (showLowInkWarning) {
                guiGraphics.blit(WIDGETS, x, y, 18, 112, 18, 18);
            }
        }
    }

    private static boolean crosshairInkIndicatorEnabled() {
        SplatcraftConfig.InkIndicatorMode mode = SplatcraftConfig.INK_INDICATOR.get();
        return mode == SplatcraftConfig.InkIndicatorMode.BOTH
                || mode == SplatcraftConfig.InkIndicatorMode.CROSSHAIR;
    }

    private static boolean isHoldingMatchItem(Player player) {
        return player.getMainHandItem().is(SplatcraftTags.Items.MATCH_ITEMS)
                || player.getOffhandItem().is(SplatcraftTags.Items.MATCH_ITEMS);
    }

    private static boolean canUseHeldMatchItem(Player player, ItemStack tankStack) {
        if (!(tankStack.getItem() instanceof InkTankItem tank)) {
            return true;
        }
        return tank.canUse(player.getMainHandItem().getItem())
                || tank.canUse(player.getOffhandItem().getItem());
    }

    private static boolean hasUnlimitedInk(Player player) {
        boolean requireInkTank = SplatcraftGameRules.localizedBoolean(
                player.level(),
                player.blockPosition(),
                SplatcraftGameRules.REQUIRE_INK_TANK);
        if (!requireInkTank) {
            return true;
        }
        return player.getAbilities().instabuild
                && SplatcraftGameRules.localizedBoolean(
                        player.level(),
                        player.blockPosition(),
                        SplatcraftGameRules.INFINITE_INK_IN_CREATIVE);
    }

    private static boolean hasInkAmount(ItemStack tankStack, boolean unlimitedInk, float amount) {
        return unlimitedInk || InkTankItem.getInkAmount(tankStack) + 0.0001F >= amount;
    }

    private static float inkPercent(ItemStack tankStack, boolean unlimitedInk) {
        if (unlimitedInk) {
            return 1.0F;
        }
        if (!(tankStack.getItem() instanceof InkTankItem tank) || tank.capacity() <= 0) {
            return 0.0F;
        }
        return Mth.clamp(InkTankItem.getInkAmount(tankStack) / tank.capacity(), 0.0F, 1.0F);
    }

    private static void setGuiColor(GuiGraphics guiGraphics, int color, float alpha, float add) {
        float red = Mth.clamp(((color >> 16) & 0xFF) / 255.0F + add, 0.0F, 1.0F);
        float green = Mth.clamp(((color >> 8) & 0xFF) / 255.0F + add, 0.0F, 1.0F);
        float blue = Mth.clamp((color & 0xFF) / 255.0F + add, 0.0F, 1.0F);
        guiGraphics.setColor(red, green, blue, alpha);
    }

    private static void resetInkHud() {
        inkHudTime = 0.0F;
        inkFlash = 0.0F;
    }

    static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player) || !coloredPlayerNamesEnabled(player)) {
            return;
        }

        event.setContent(withPlayerInkColor(event.getContent(), player));
    }

    static void onClientChat(ClientChatReceivedEvent.Player event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !SplatcraftGameRules.booleanValue(minecraft.level, SplatcraftGameRules.COLORED_PLAYER_NAMES)) {
            return;
        }

        Player sender = minecraft.level.getPlayerByUUID(event.getSender());
        ChatType.Bound bound = event.getBoundChatType();
        if (bound == null) {
            return;
        }

        OptionalInt cachedColor = ClientPlayerColors.color(event.getSender());
        int color = cachedColor.isPresent()
                ? cachedColor.getAsInt()
                : sender != null ? SplatcraftPlayerInfoEvents.color(sender) : -1;
        if (color < 0) {
            return;
        }

        ChatType.Bound coloredBound = new ChatType.Bound(
                bound.chatType(),
                withInkColor(bound.name(), color),
                bound.targetName());
        event.setMessage(coloredBound.decorate(event.getPlayerChatMessage().decoratedContent()));
    }

    private static boolean coloredPlayerNamesEnabled(Player player) {
        return SplatcraftGameRules.booleanValue(player.level(), SplatcraftGameRules.COLORED_PLAYER_NAMES);
    }

    private static Component withPlayerInkColor(Component component, Player player) {
        return withInkColor(component, SplatcraftPlayerInfoEvents.color(player));
    }

    private static Component withInkColor(Component component, int color) {
        int visibleColor = ClientInkColors.visibleColor(color);
        return component.copy().withStyle(style -> style.withColor(TextColor.fromRgb(visibleColor)));
    }
}
