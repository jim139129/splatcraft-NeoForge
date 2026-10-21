package net.splatcraft.neoforge.network.payload;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.blockentity.InkVatBlockEntity;
import net.splatcraft.neoforge.entity.sub.AbstractSubWeaponEntity;
import net.splatcraft.neoforge.entity.sub.SuctionBombEntity;
import net.splatcraft.neoforge.item.WeaponItem;
import net.splatcraft.neoforge.menu.InkVatMenu;
import net.splatcraft.neoforge.player.PlayerInfo;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.registry.SplatcraftEntities;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import net.splatcraft.neoforge.worldink.WorldInk;
import net.splatcraft.neoforge.worldink.WorldInkStorage;

@GameTestHolder(Splatcraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReviewRegressionTests {
    private static final String TEMPLATE = "review_empty";

    @GameTest(template = TEMPLATE)
    public static void vatSwapChargesHotbarAndOffhand(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int destination : new int[]{0, 40}) {
            InkVatMenu menu = vat(helper, player, 4);
            InkVatBlockEntity vat = (InkVatBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
            menu.clicked(4, destination, ClickType.SWAP, player);
            helper.assertTrue(player.getInventory().getItem(destination).getCount() == 4, "Swap must deliver four inkwells");
            assertIngredients(helper, vat, 0);
            menu.clicked(4, destination, ClickType.SWAP, player);
            helper.assertTrue(player.getInventory().getItem(destination).getCount() == 4, "An occupied swap destination must remain unchanged");
            player.getInventory().setItem(destination, ItemStack.EMPTY);
            menu.clicked(4, destination, ClickType.SWAP, player);
            helper.assertTrue(player.getInventory().getItem(destination).isEmpty(), "Empty ingredients must not regenerate a free preview");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vatNormalAndPartialShiftTakeChargeExactlyOnce(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InkVatMenu menu = vat(helper, player, 4);
        InkVatBlockEntity vat = (InkVatBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        menu.clicked(4, 1, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 2, "Right click must take half the output");
        assertIngredients(helper, vat, 2);

        menu = vat(helper, player, 4);
        vat = (InkVatBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        int stackLimit = vat.getItem(4).getMaxStackSize();
        player.getInventory().setItem(0, vat.getItem(4).copyWithCount(stackLimit - 1));
        menu.clicked(4, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(player.getInventory().getItem(0).getCount() == stackLimit, "Shift click must use the one available space");
        assertIngredients(helper, vat, 3);
        menu.clicked(4, 0, ClickType.QUICK_MOVE, player);
        assertIngredients(helper, vat, 3);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vatAutomationSimulationDoesNotConsume(GameTestHelper helper) {
        vat(helper, helper.makeMockPlayer(GameType.SURVIVAL), 4);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        InkVatBlockEntity vat = (InkVatBlockEntity) helper.getLevel().getBlockEntity(pos);
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        helper.assertTrue(handler != null, "Vat must expose its extraction capability");
        helper.assertTrue(handler.extractItem(0, 1, true).getCount() == 1, "Simulation must offer an inkwell");
        assertIngredients(helper, vat, 4);
        helper.assertTrue(handler.extractItem(0, 1, false).getCount() == 1, "Committed extraction must deliver an inkwell");
        assertIngredients(helper, vat, 3);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void colorPacketRejectsUnboundCoordinatesBeforeLoading(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "review-player"), ClientInformation.createDefault());
        IPayloadContext context = context(player);
        BlockPos remote = new BlockPos(2_000_000, 70, 2_000_000);
        helper.assertTrue(!helper.getLevel().hasChunkAt(remote), "Remote test chunk must start unloaded");
        UpdateBlockColorPayload.handle(new UpdateBlockColorPayload(remote, 0, 0), context);
        helper.assertTrue(!helper.getLevel().hasChunkAt(remote), "Packet without a vat menu must not load a chunk");

        InkVatMenu menu = vat(helper, player, 4);
        player.containerMenu = menu;
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        UpdateBlockColorPayload.handle(new UpdateBlockColorPayload(remote, 0, 0), context);
        helper.assertTrue(!helper.getLevel().hasChunkAt(remote), "Packet for another coordinate must not load a chunk");
        InkVatBlockEntity vat = (InkVatBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        int selected = menu.getRecipeColors().get(1);
        UpdateBlockColorPayload.handle(new UpdateBlockColorPayload(vat.getBlockPos(), selected, 1), context);
        helper.assertTrue(vat.getColor() == selected && vat.getPointer() == 1, "Valid color selection must still work");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void soundOutsideLoadedChunksDoesNotLoadTerrain(GameTestHelper helper) {
        BlockPos remote = new BlockPos(2_000_032, 70, 2_000_032);
        helper.assertTrue(!helper.getLevel().hasChunkAt(remote), "Remote sound chunk must start unloaded");
        helper.getLevel().playSound(null, remote, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        helper.assertTrue(!helper.getLevel().hasChunkAt(remote), "Sound decoration must never load terrain");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void splatBombDetonatesAfterLanding(GameTestHelper helper) {
        for (double height : new double[]{1.1D, 3.0D, 5.0D}) {
            floor(helper);
            Vec3 start = helper.absoluteVec(new Vec3(3.5D, height, 3.5D));
            var bomb = AbstractSubWeaponEntity.create(SplatcraftEntities.SPLAT_BOMB.get(), helper.getLevel(),
                    start.x, start.y, start.z, new ItemStack(SplatcraftItems.SPLAT_BOMB.get()));
            boolean landed = false;
            for (int tick = 0; tick < 120 && !bomb.isRemoved(); tick++) {
                bomb.tick();
                landed |= bomb.onGround();
            }
            helper.assertTrue(landed, "Splat bomb must detect the floor from height " + height);
            helper.assertTrue(bomb.isRemoved(), "Ground fuse must detonate well before the 600-tick fallback");
            helper.assertTrue(WorldInkStorage.isInked(helper.getLevel(), helper.absolutePos(new BlockPos(3, 0, 3))), "Detonation must paint the floor");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void curlingBombMaintainsGroundContactAndFallsOffEdge(GameTestHelper helper) {
        floor(helper);
        Vec3 start = helper.absoluteVec(new Vec3(3.5D, 1.1D, 3.5D));
        var bomb = AbstractSubWeaponEntity.create(SplatcraftEntities.CURLING_BOMB.get(), helper.getLevel(),
                start.x, start.y, start.z, new ItemStack(SplatcraftItems.CURLING_BOMB.get()));
        for (int tick = 0; tick < 50 && !bomb.onGround(); tick++) {
            bomb.tick();
        }
        helper.assertTrue(bomb.onGround(), "Curling bomb must settle on the floor: y=" + bomb.getY() + ", velocity=" + bomb.getDeltaMovement());
        double groundY = bomb.getY();
        for (int tick = 0; tick < 5; tick++) {
            bomb.tick();
            helper.assertTrue(bomb.onGround() && Math.abs(bomb.getY() - groundY) < 0.001D, "Ground contact must not flicker or sink");
        }
        helper.setBlock(3, 0, 3, Blocks.AIR);
        bomb.tick();
        helper.assertTrue(!bomb.onGround(), "Removing support must clear ground contact");
        bomb.tick();
        bomb.tick();
        helper.assertTrue(bomb.getY() < groundY - 0.01D, "Unsupported curling bomb must fall");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void suctionBombStaysOutsideEveryHitFace(GameTestHelper helper) {
        BlockPos stone = helper.absolutePos(new BlockPos(3, 3, 3));
        helper.setBlock(3, 3, 3, Blocks.STONE);
        for (Direction direction : Direction.values()) {
            TestSuctionBomb bomb = new TestSuctionBomb(helper.getLevel());
            Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
            Vec3 hit = Vec3.atCenterOf(stone).add(normal.scale(0.5D));
            bomb.hit(new BlockHitResult(hit, direction, stone, false));
            helper.assertTrue(bomb.position().subtract(hit).dot(normal) > 0.0D, "Bomb must stick outside " + direction);
            Player target = helper.makeMockPlayer(GameType.SURVIVAL);
            target.setPos(hit.add(normal.scale(2.0D)));
            helper.assertTrue(Explosion.getSeenPercent(bomb.position(), target) > 0.0F, "Attached block must not hide explosion from " + direction);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void suctionBombPaintsBeyondAttachment(GameTestHelper helper) {
        floor(helper);
        TestSuctionBomb bomb = new TestSuctionBomb(helper.getLevel());
        bomb.setSourceWeapon(new ItemStack(SplatcraftItems.SUCTION_BOMB.get()));
        BlockPos stone = helper.absolutePos(new BlockPos(3, 0, 3));
        bomb.hit(new BlockHitResult(Vec3.atBottomCenterOf(stone.above()), Direction.UP, stone, false));
        for (int tick = 0; tick < 50 && !bomb.isRemoved(); tick++) {
            bomb.tick();
        }
        helper.assertTrue(bomb.isRemoved(), "Suction fuse must detonate");
        helper.assertTrue(WorldInkStorage.isInked(helper.getLevel(), stone.east()), "Explosion must paint beyond the attached block");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargerChargeSurvivesTransformationUntilExpiry(GameTestHelper helper) {
        assertChargeStorage(helper, SplatcraftItems.SPLAT_CHARGER.get(), "ChargerStorageTicks", 25);
    }

    @GameTest(template = TEMPLATE)
    public static void nautilusChargeSurvivesTransformationUntilExpiry(GameTestHelper helper) {
        assertChargeStorage(helper, SplatcraftItems.NAUTILUS_47.get(), "SplatlingStorageTicks", 66);
    }

    @GameTest(template = TEMPLATE)
    public static void inkDeltaOnlyInvalidatesChangedPositions(GameTestHelper helper) {
        WorldInk ink = new WorldInk(helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO)));
        WorldInk.Entry entry = new WorldInk.Entry(0x123456, PlayerInfo.NORMAL_INK_TYPE);
        for (int i = 0; i < 5000; i++) {
            ink.setSyncedInk(new BlockPos(i % 16, i / 256, i / 16 % 16), entry);
        }
        var updates = new ArrayList<BlockPos>();
        WorldInkPayload.update(new ChunkPos(0, 0), BlockPos.ZERO, null).apply(ink, updates::add);
        helper.assertTrue(updates.size() == 1 && updates.getFirst().equals(BlockPos.ZERO), "A delta must invalidate only its one changed position");
        helper.assertTrue(ink.getInk(BlockPos.ZERO) == null && ink.inkInChunk().size() == 4999, "Delta removal must preserve unrelated ink");
        updates.clear();
        BlockPos replacement = new BlockPos(0, 100, 0);
        WorldInkPayload.chunk(new ChunkPos(0, 0), Map.of(replacement, entry)).apply(ink, updates::add);
        helper.assertTrue(updates.size() == 5000 && updates.contains(replacement), "Snapshot must invalidate both removed and added positions");
        helper.assertTrue(ink.inkInChunk().equals(Map.of(replacement, entry)), "Snapshot must replace all old ink");
        helper.succeed();
    }

    private static InkVatMenu vat(GameTestHelper helper, Player player, int count) {
        helper.setBlock(1, 1, 1, SplatcraftBlocks.INK_VAT.get());
        InkVatBlockEntity vat = (InkVatBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        vat.setItem(0, new ItemStack(Items.INK_SAC, count));
        vat.setItem(1, new ItemStack(SplatcraftItems.POWER_EGG.get(), count));
        vat.setItem(2, new ItemStack(SplatcraftItems.EMPTY_INKWELL.get(), count));
        vat.setItem(3, new ItemStack(SplatcraftItems.CREATIVE_FILTER.get()));
        InkVatMenu menu = new InkVatMenu(1, player.getInventory(), vat);
        helper.assertTrue(menu.clickMenuButton(player, 0), "Fixture needs a valid color recipe");
        return menu;
    }

    private static void assertIngredients(GameTestHelper helper, InkVatBlockEntity vat, int count) {
        for (int slot = 0; slot < 3; slot++) {
            helper.assertTrue(vat.getItem(slot).getCount() == count, "Ingredient " + slot + " must have " + count + " remaining, got " + vat.getItem(slot).getCount());
        }
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                WorldInkStorage.clearInk(helper.getLevel(), helper.absolutePos(new BlockPos(x, 0, z)), true);
            }
        }
    }

    private static void assertChargeStorage(GameTestHelper helper, WeaponItem weapon, String ticksKey, int duration) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        player.setNoGravity(true);
        player.setPos(helper.absoluteVec(new Vec3(3.5D, 3.0D, 3.5D)));
        ItemStack stack = new ItemStack(weapon);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 30; tick++) {
            player.setOnGround(true);
            player.tick();
        }
        player.setOnGround(true);
        SplatcraftPlayerInfoEvents.setSquid(player, true);
        helper.assertTrue(!player.isUsingItem(), "Transformation must stop use after storing charge");
        helper.assertTrue(storageTicks(stack, ticksKey) == duration, "Transformation must store the configured charge duration");
        for (int tick = 0; tick < 5; tick++) {
            weapon.inventoryTick(stack, helper.getLevel(), player, 0, true);
        }
        helper.assertTrue(storageTicks(stack, ticksKey) == duration - 5, "Squid inventory ticks must preserve unexpired charge");
        SplatcraftPlayerInfoEvents.setSquid(player, false);
        helper.assertTrue(storageTicks(stack, ticksKey) == duration - 5, "Reverting must preserve the charge");
        for (int tick = 5; tick < duration; tick++) {
            weapon.inventoryTick(stack, helper.getLevel(), player, 0, true);
        }
        helper.assertTrue(storageTicks(stack, ticksKey) == 0, "Stored charge must expire on schedule");
        helper.succeed();
    }

    private static int storageTicks(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(key);
    }

    private static IPayloadContext context(Player player) {
        return (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class<?>[]{IPayloadContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "player" -> player;
                    case "enqueueWork" -> {
                        ((Runnable) args[0]).run();
                        yield CompletableFuture.completedFuture(null);
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class TestSuctionBomb extends SuctionBombEntity {
        private TestSuctionBomb(Level level) {
            super(SplatcraftEntities.SUCTION_BOMB.get(), level);
        }

        private void hit(BlockHitResult hit) {
            onHitBlock(hit);
        }
    }
}
