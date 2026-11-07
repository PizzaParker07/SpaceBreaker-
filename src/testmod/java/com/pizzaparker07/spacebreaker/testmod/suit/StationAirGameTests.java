package com.pizzaparker07.spacebreaker.testmod.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.SealedRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Station modules (rocketry hull, windows, docking port) hold the air an oxygen distributor puts in them. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationAirGameTests {
    private StationAirGameTests() {
    }

    /** A 3 × 3 × 3 module of station hull with a window, a docking port and an airlock door in its walls. */
    private static OxygenDistributorBlockEntity module(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    boolean shell = x == 1 || x == 5 || y == 1 || y == 5 || z == 1 || z == 5;
                    helper.setBlock(x, y, z, shell ? RocketryModule.STATION_HULL.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.setBlock(3, 3, 1, RocketryModule.STATION_WINDOW.get());
        helper.setBlock(1, 3, 3, RocketryModule.DOCKING_PORT.get());
        BlockState lower = SuitModule.AIRLOCK_DOOR.get().defaultBlockState().setValue(AirlockDoorBlock.FACING, Direction.EAST);
        helper.setBlock(new BlockPos(5, 2, 3), lower);
        helper.setBlock(new BlockPos(5, 3, 3), lower.setValue(AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos d = new BlockPos(3, 2, 3);
        helper.setBlock(d, SuitModule.OXYGEN_DISTRIBUTOR.get());
        OxygenDistributorBlockEntity be = helper.getBlockEntity(d);
        be.setStack(OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        return be;
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void stationModuleHoldsAirBehindItsAirlock(GameTestHelper helper) {
        OxygenDistributorBlockEntity be = module(helper);
        BlockPos inside = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(be.room().sealed() && be.room().volume() == 26,
                        "hull, window, docking port and closed airlock must seal 26 blocks, got " + be.room().state() + " " + be.room().volume()))
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.providesOxygen(helper.getLevel(), inside), "the module pressurises"))
                .thenExecute(() -> {
                    Player p = helper.makeMockPlayer(GameType.SURVIVAL);
                    p.setPos(inside.getX() + 0.5, inside.getY() - 1, inside.getZ() + 0.5);
                    helper.assertTrue(LifeSupport.Env.at(p, 0).breathable(), "without a helmet the air inside is breathable");
                    BlockPos door = helper.absolutePos(new BlockPos(5, 2, 3));
                    ((AirlockDoorBlock) SuitModule.AIRLOCK_DOOR.get()).setOpen(helper.getLevel(), door, helper.getLevel().getBlockState(door), true);
                })
                .thenWaitUntil(() -> helper.assertTrue(!be.room().isScanning() && !be.room().sealed(), "opening the airlock lets the air out"))
                .thenExecute(() -> helper.assertFalse(SealedRooms.providesOxygen(helper.getLevel(), inside), "a vented module is not breathable"))
                .thenExecute(() -> {
                    BlockPos door = helper.absolutePos(new BlockPos(5, 2, 3));
                    ((AirlockDoorBlock) SuitModule.AIRLOCK_DOOR.get()).setOpen(helper.getLevel(), door, helper.getLevel().getBlockState(door), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.providesOxygen(helper.getLevel(), inside), "closed again, the module re-pressurises"))
                .thenSucceed();
    }

    /**
     * The habitat module kit (rocketry) builds a room the suit systems understand: once its distributor has a
     * tank the room pressurises, a player can take the helmet off and breathe, and opening its airlock vents it.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void habitatKitRoomSupportsHelmetOff(GameTestHelper helper) {
        BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 4));
        Direction forward = Direction.SOUTH;
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.unfold(helper.getLevel(), centre, forward),
                "the habitat kit unfolds in empty space");
        BlockPos dPos = com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.distributor(centre);
        OxygenDistributorBlockEntity be = (OxygenDistributorBlockEntity) helper.getLevel().getBlockEntity(dPos);
        helper.assertTrue(be != null, "the kit places an oxygen distributor");
        be.setStack(OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        BlockPos inside = centre.above().relative(forward.getClockWise(), 1).relative(forward, -1);
        BlockPos door = centre.above().relative(forward, -com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.HALF);
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(be.room().sealed(), "the habitat room must be sealed, got " + be.room().state()))
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.providesOxygen(helper.getLevel(), inside.above()), "the habitat pressurises"))
                .thenExecute(() -> {
                    // no suit at all: breathing normally inside
                    p.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
                    var data = LifeSupport.data(p);
                    data.breath = 10;
                    for (int i = 0; i < 100; i++) {
                        LifeSupport.tick(p, LifeSupport.Env.at(p, 0));
                    }
                    helper.assertTrue(data.breathable, "the air inside the habitat is breathable");
                    helper.assertTrue(data.breath == com.pizzaparker07.spacebreaker.content.suit.SurvivalData.MAX_BREATH,
                            "breath recovers without a helmet inside, got " + data.breath);
                    helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SuitEvents.pressurisedAt(p), "the HUD reports a pressurised module");
                    ((AirlockDoorBlock) SuitModule.AIRLOCK_DOOR.get()).setOpen(helper.getLevel(), door, helper.getLevel().getBlockState(door), true);
                })
                .thenWaitUntil(() -> helper.assertTrue(!be.room().isScanning() && !be.room().sealed(), "the open airlock vents the habitat"))
                .thenExecute(() -> {
                    // the test world itself has air, so check the habitat's own supply and the HUD flag
                    helper.assertFalse(SealedRooms.providesOxygen(helper.getLevel(), inside.above()), "a vented habitat holds no air");
                    helper.assertFalse(com.pizzaparker07.spacebreaker.content.suit.SuitEvents.pressurisedAt(p), "the HUD drops the pressurised tag");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void registeredCabinCountsAsPressurised(GameTestHelper helper) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.suit.SuitEvents.pressurisedAt(p), "nothing pressurised here yet");
        String tag = "spacebreaker_test_cabin";
        com.pizzaparker07.spacebreaker.content.suit.SuitEvents.addPressureSource(pl -> pl.getTags().contains(tag));
        p.addTag(tag);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SuitEvents.pressurisedAt(p), "a registered cabin is pressurised");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void environmentOxygenSourceCountsAsPressurised(GameTestHelper helper) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos marker = helper.absolutePos(new BlockPos(1, 1, 1));
        p.setPos(marker.getX() + 0.5, marker.getY() - 1, marker.getZ() + 0.5);
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.suit.SuitEvents.pressurisedAt(p), "open air is not a pressurised cabin");
        // a cabin-like source: air wherever a beacon block marks it (stands in for rocketry's crew capsule)
        com.pizzaparker07.spacebreaker.core.env.Environment.addOxygenSource((level, pos) -> level.getBlockState(pos).is(Blocks.BEACON));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.BEACON);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SuitEvents.pressurisedAt(p), "an Environment oxygen source counts");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void suitStationTopsUpTanksAndMendsWear(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, SuitModule.SUIT_STATION.get());
        var be = (com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlockEntity) helper.getBlockEntity(pos);
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        com.pizzaparker07.spacebreaker.content.suit.SuitTanks.insert(torso, new ItemStack(SuitModule.OXYGEN_TANK.get()));
        torso.setDamageValue(30);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, torso);
        helper.assertFalse(be.service(p, true, 0), "without power nothing happens");
        be.getEnergy().receiveEnergy(32000, false);
        for (int t = 0; t <= 400; t++) {
            be.service(p, true, t);
        }
        ItemStack worn = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SuitTanks.oxygen(worn) == com.pizzaparker07.spacebreaker.content.suit.SuitTanks.capacity(worn),
                "the worn tank is topped up, got " + com.pizzaparker07.spacebreaker.content.suit.SuitTanks.oxygen(worn));
        helper.assertTrue(worn.getDamageValue() == 30 - 401 / com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlockEntity.REPAIR_TICKS - 1
                || worn.getDamageValue() < 30, "wear is mended slowly, damage now " + worn.getDamageValue());
        ItemStack loose = new ItemStack(SuitModule.EVA_HELMET.get());
        loose.setDamageValue(5);
        helper.assertTrue(be.mend(loose) && loose.getDamageValue() == 4, "a loose piece in the slot is mended too");
        // off-planet without piped oxygen the station cannot fill tanks
        com.pizzaparker07.spacebreaker.content.suit.SuitTanks.drain(worn, 100);
        helper.assertFalse(be.fillWearer(p, false), "no breathable air and nothing piped in: no oxygen to give");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void electricDrillDigsOnChargeAndChargesAtTheStation(GameTestHelper helper) {
        ItemStack drill = new ItemStack(SuitModule.ELECTRIC_DRILL.get());
        var stone = Blocks.STONE.defaultBlockState();
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.charged(drill), "a new drill is flat");
        helper.assertTrue(drill.getDestroySpeed(stone) < 2F, "flat, it is barely better than a fist");
        helper.assertFalse(drill.isCorrectToolForDrops(stone), "and drops nothing");
        // charge it at a powered station
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, SuitModule.SUIT_STATION.get());
        var be = (com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlockEntity) helper.getBlockEntity(pos);
        be.getEnergy().receiveEnergy(32000, false);
        for (int i = 0; i < 10; i++) {
            be.chargeDrill(drill);
        }
        int charge = com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.energy(drill);
        helper.assertTrue(charge == 10 * com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlockEntity.DRILL_CHARGE_RATE,
                "the station charges the drill, got " + charge);
        helper.assertTrue(drill.getDestroySpeed(stone) > net.minecraft.world.item.Tiers.DIAMOND.getSpeed(), "charged, it outdigs a diamond pick");
        helper.assertTrue(drill.isCorrectToolForDrops(stone) && drill.isCorrectToolForDrops(Blocks.DIRT.defaultBlockState()),
                "it mines stone and soil");
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos rock = helper.absolutePos(new BlockPos(4, 1, 4));
        drill.mineBlock(helper.getLevel(), stone, rock, p);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.energy(drill)
                == charge - com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.FE_PER_BLOCK, "each block spends charge");
        helper.assertFalse(drill.isDamageableItem(), "no durability: the battery is what runs down");
        // the standard FE item capability fills it too
        var cap = drill.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(cap != null && cap.receiveEnergy(500, false) == 500, "any FE charger can fill it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wideBoreDrillDigsThreeByThree(GameTestHelper helper) {
        // smithing: template + drill + head keeps the charge and adds the wide bore
        ItemStack drill = new ItemStack(SuitModule.ELECTRIC_DRILL.get());
        com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.setEnergy(drill, 5000);
        var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(SuitModule.SUIT_UPGRADE_TEMPLATE.get()), drill,
                new ItemStack(SuitModule.WIDE_BORE_HEAD.get()));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "the wide-bore smithing recipe matches");
        ItemStack wide = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.wideBore(wide), "the result has the wide bore");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.energy(wide) == 5000, "and keeps its charge");
        // a 3 x 3 floor of stone; the centre has just been mined, looking down at it
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
        helper.setBlock(2, 1, 2, Blocks.AIR);
        // a survival miner (the GameTest mock player is creative, which spends no charge)
        net.minecraft.server.level.ServerPlayer sp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        helper.assertFalse(sp.isCreative(), "the miner is in survival");
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wide);
        BlockPos centre = helper.absolutePos(new BlockPos(2, 1, 2));
        com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.breakAround(sp, centre, Blocks.STONE.defaultBlockState(), Direction.UP);
        int left = 0;
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                if (!helper.getBlockState(new BlockPos(x, 1, z)).isAir()) {
                    left++;
                }
            }
        }
        helper.assertTrue(left == 0, "the eight neighbours are dug too, " + left + " left");
        int spent = 5000 - com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.energy(sp.getMainHandItem());
        helper.assertTrue(spent == 8 * com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.FE_PER_BLOCK, "each block spends its own charge, spent " + spent);
        sp.setShiftKeyDown(true);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.wideFace(sp, centre) == null, "sneaking digs a single block");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wayHomeArrowAndOxygenCheck(GameTestHelper helper) {
        // direction: looking south (yaw 0) at a target due south is straight ahead; due east is 90 degrees to the left
        net.minecraft.world.phys.Vec3 o = net.minecraft.world.phys.Vec3.ZERO;
        float ahead = com.pizzaparker07.spacebreaker.content.suit.Navigation.relativeYaw(o, 0F, new BlockPos(0, 0, 20));
        float east = com.pizzaparker07.spacebreaker.content.suit.Navigation.relativeYaw(o, 0F, new BlockPos(20, 0, 0));
        helper.assertTrue(Math.abs(ahead) < 3F, "straight ahead, got " + ahead);
        helper.assertTrue(Math.abs(east + 90F) < 3F, "east is to the left when facing south, got " + east);
        // oxygen: 300 blocks on the Moon take 100 s = 2000 ticks of walking
        var ok = com.pizzaparker07.spacebreaker.content.suit.Navigation.status(300, 0.26, 6000);
        var turn = com.pizzaparker07.spacebreaker.content.suit.Navigation.status(300, 0.26, 2500);
        var crit = com.pizzaparker07.spacebreaker.content.suit.Navigation.status(300, 0.26, 1500);
        helper.assertTrue(ok == com.pizzaparker07.spacebreaker.content.suit.Navigation.Status.OK, "plenty of oxygen: OK");
        helper.assertTrue(turn == com.pizzaparker07.spacebreaker.content.suit.Navigation.Status.TURN_BACK, "within the margin: turn back");
        helper.assertTrue(crit == com.pizzaparker07.spacebreaker.content.suit.Navigation.Status.CRITICAL, "not enough: critical");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.Navigation.walkTicks(30, 1e-5)
                > com.pizzaparker07.spacebreaker.content.suit.Navigation.walkTicks(30, 0.26), "walking home on a comet is slower");
        // targets: only the player's own beacon counts
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 1, 1)));
        var beacons = com.pizzaparker07.spacebreaker.content.rocketry.station.LandingBeacons.get(helper.getLevel());
        BlockPos mine = helper.absolutePos(new BlockPos(5, 1, 5));
        BlockPos theirs = helper.absolutePos(new BlockPos(2, 1, 2));
        beacons.add(mine);
        beacons.setOwner(mine, p.getGameProfile().getName());
        beacons.add(theirs);
        beacons.setOwner(theirs, "someone_else");
        var found = com.pizzaparker07.spacebreaker.content.suit.Navigation.find(helper.getLevel(), p,
                com.pizzaparker07.spacebreaker.content.suit.Navigation.Kind.BEACON);
        beacons.remove(mine);
        beacons.remove(theirs);
        helper.assertTrue(found.isPresent() && found.get().pos().equals(mine), "the arrow points at my own beacon, not the nearer one of another player");
        helper.succeed();
    }
}
