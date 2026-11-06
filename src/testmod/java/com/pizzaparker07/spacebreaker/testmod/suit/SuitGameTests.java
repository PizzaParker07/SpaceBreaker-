package com.pizzaparker07.spacebreaker.testmod.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.Gravity;
import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import com.pizzaparker07.spacebreaker.content.suit.SurvivalData;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenFillerBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.SealedRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SuitGameTests {
    private static final String EMPTY = "empty";
    private static final LifeSupport.Env VACUUM = new LifeSupport.Env(false, 21.0, 0.0, 0.0);

    private SuitGameTests() {
    }

    private static Player suited(GameTestHelper helper, boolean complete, int tanks) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        for (int i = 0; i < tanks; i++) {
            SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        }
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, torso);
        if (complete) {
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
        }
        return p;
    }

    // ------------------------------------------------------------------ suit and oxygen

    @GameTest(template = EMPTY)
    public static void suitSealedOnlyWithAllFourPieces(GameTestHelper helper) {
        helper.assertTrue(SpaceSuitItem.isSealed(suited(helper, true, 0)), "full suit must be sealed");
        helper.assertFalse(SpaceSuitItem.isSealed(suited(helper, false, 0)), "helmet + torso alone must not be sealed");
        Player p = suited(helper, true, 0);
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.MAGNETIC_BOOTS.get()));
        helper.assertTrue(SpaceSuitItem.isSealed(p), "magnetic boots count as EVA boots");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tanksSlotIntoTorso(GameTestHelper helper) {
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        helper.assertTrue(SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get())), "first slot");
        helper.assertTrue(SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get())), "second slot");
        helper.assertFalse(SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get())), "only two slots");
        int total = OxygenTankItem.SMALL_CAPACITY + OxygenTankItem.LARGE_CAPACITY;
        helper.assertTrue(SuitTanks.oxygen(torso) == total && SuitTanks.capacity(torso) == total, "oxygen sums both tanks");
        helper.assertTrue(SuitTanks.drain(torso, 100) == 100 && SuitTanks.oxygen(torso) == total - 100, "drain takes from the tanks");
        helper.assertFalse(SuitTanks.removeLast(torso).isEmpty(), "tanks can be taken out");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void sealedSuitUsesOxygenInVacuum(GameTestHelper helper) {
        Player p = suited(helper, true, 1);
        for (int i = 0; i < 200; i++) {
            LifeSupport.tick(p, VACUUM);
        }
        int left = SuitTanks.oxygen(SuitTanks.wornTorso(p));
        helper.assertTrue(left == OxygenTankItem.SMALL_CAPACITY - 200, "one breathing tick per tick, left " + left);
        helper.assertTrue(LifeSupport.data(p).breath == SurvivalData.MAX_BREATH, "breath stays full while supplied");
        helper.assertTrue(p.getHealth() == p.getMaxHealth(), "no damage while supplied");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void breathableAirUsesNoOxygen(GameTestHelper helper) {
        Player p = suited(helper, true, 1);
        for (int i = 0; i < 100; i++) {
            LifeSupport.tick(p, new LifeSupport.Env(true, 15.0, 0.0, 101.3));
        }
        helper.assertTrue(SuitTanks.oxygen(SuitTanks.wornTorso(p)) == OxygenTankItem.SMALL_CAPACITY, "no oxygen used in breathable air");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void unsealedSuitSuffocates(GameTestHelper helper) {
        Player p = suited(helper, false, 1);
        for (int i = 0; i < SurvivalData.MAX_BREATH + 60; i++) {
            p.tickCount = i;
            p.invulnerableTime = 0;
            LifeSupport.tick(p, VACUUM);
        }
        helper.assertTrue(SuitTanks.oxygen(SuitTanks.wornTorso(p)) == OxygenTankItem.SMALL_CAPACITY, "an unsealed suit cannot use its tanks");
        helper.assertTrue(LifeSupport.data(p).breath == 0, "breath runs out in vacuum");
        helper.assertTrue(p.getHealth() < p.getMaxHealth(), "suffocation hurts, health " + p.getHealth());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void emptyTanksSuffocate(GameTestHelper helper) {
        Player p = suited(helper, true, 0);
        for (int i = 0; i < SurvivalData.MAX_BREATH + 60; i++) {
            p.tickCount = i;
            p.invulnerableTime = 0;
            LifeSupport.tick(p, VACUUM);
        }
        helper.assertTrue(p.getHealth() < p.getMaxHealth(), "no oxygen means suffocation");
        helper.succeed();
    }

    // ------------------------------------------------------------------ temperature and radiation

    @GameTest(template = EMPTY)
    public static void bodyTemperatureDriftsSlowly(GameTestHelper helper) {
        double body = SurvivalData.NORMAL_BODY_C;
        double target = LifeSupport.targetBodyTemp(-173.0);
        helper.assertTrue(target < 30.0, "lunar night without a suit is deadly cold, target " + target);
        for (int i = 0; i < 20 * 10; i++) {
            body = LifeSupport.drift(body, target);
        }
        helper.assertTrue(body < SurvivalData.NORMAL_BODY_C && body > 35.0, "ten seconds only cool a little, body " + body);
        for (int i = 0; i < 20 * 600; i++) {
            body = LifeSupport.drift(body, target);
        }
        helper.assertTrue(Math.abs(body - target) < 0.5 && body >= target, "settles at the target without overshoot, body " + body);
        Player p = suited(helper, true, 1);
        helper.assertTrue(LifeSupport.effectiveAmbient(p, -173.0) == 21.0, "Mk1 suit keeps the cabin at 21 °C in lunar night");
        helper.assertTrue(LifeSupport.effectiveAmbient(p, 120.0) == 21.0, "and in lunar day");
        helper.assertTrue(LifeSupport.effectiveAmbient(p, 400.0) > 200.0, "Venus heat overwhelms a Mk1 suit");
        helper.assertTrue(LifeSupport.targetBodyTemp(21.0) == SurvivalData.NORMAL_BODY_C, "comfortable cabin keeps 37 °C");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void radiationDoseAccumulates(GameTestHelper helper) {
        double dose = 0;
        for (int i = 0; i < 24000; i++) {
            dose = LifeSupport.accumulate(dose, 1000.0);
        }
        helper.assertTrue(Math.abs(dose - 998.0) < 1.0, "one day at 1 Sv/day minus recovery, got " + dose);
        double moon = 0;
        for (int i = 0; i < 24000; i++) {
            moon = LifeSupport.accumulate(moon, 1.37);
        }
        helper.assertTrue(moon == 0.0, "lunar surface dose is fully recovered day to day");
        helper.assertTrue(LifeSupport.radiationFactor(suited(helper, true, 0)) < 1.0, "a sealed suit shields some radiation");
        helper.succeed();
    }

    // ------------------------------------------------------------------ gravity

    @GameTest(template = EMPTY)
    public static void gravityFactorApplied(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 2, 2);
        zombie.setNoAi(true);
        double base = zombie.getAttributeBaseValue(Attributes.GRAVITY);
        Gravity.apply(zombie, 0.26);
        double g = zombie.getAttributeValue(Attributes.GRAVITY);
        helper.assertTrue(Math.abs(g - base * 0.26) < 1e-6, "gravity attribute scaled to 0.26, got " + g);
        double safe = zombie.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        helper.assertTrue(Math.abs(safe - 3.0 / 0.26) < 1e-3, "safe fall height grows with 1/g, got " + safe);
        helper.assertTrue(zombie.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) < 0.3, "fall damage scales with g");
        double jump = zombie.getAttributeValue(Attributes.JUMP_STRENGTH) / zombie.getAttributeBaseValue(Attributes.JUMP_STRENGTH);
        helper.assertTrue(jump < 1.0 && jump > 0.6, "jump speed scales with g^0.25, got " + jump);
        Gravity.apply(zombie, 0.0);
        helper.assertTrue(zombie.getAttributeValue(Attributes.GRAVITY) == 0.0, "free fall in orbit");
        Gravity.apply(zombie, 1.0);
        helper.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.GRAVITY) - base) < 1e-9, "Earth gravity restores vanilla");
        helper.assertTrue(Gravity.factorFor(zombie) == 1.0, "overworld gravity factor is 1");
        helper.succeed();
    }

    // ------------------------------------------------------------------ sealed rooms

    /** Hollow 5x5x5 stone box at (1..5, 1..5, 1..5): air inside is 3x3x3 minus the distributor. */
    private static BlockPos buildBox(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    boolean shell = x == 1 || x == 5 || y == 1 || y == 5 || z == 1 || z == 5;
                    helper.setBlock(x, y, z, shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        BlockPos distributor = new BlockPos(3, 2, 3);
        helper.setBlock(distributor, SuitModule.OXYGEN_DISTRIBUTOR.get());
        OxygenDistributorBlockEntity be = helper.getBlockEntity(distributor);
        be.setStack(OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        return distributor;
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void sealedRoomFloodFill(GameTestHelper helper) {
        BlockPos d = buildBox(helper);
        OxygenDistributorBlockEntity be = helper.getBlockEntity(d);
        BlockPos inside = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(be.room().sealed() && be.room().volume() == 26,
                        "expected a sealed room of 26 blocks, got " + be.room().state() + " " + be.room().volume()))
                .thenExecute(() -> {
                    helper.assertTrue(SealedRooms.providesOxygen(helper.getLevel(), inside), "air inside is breathable");
                    helper.assertFalse(SealedRooms.providesOxygen(helper.getLevel(), helper.absolutePos(new BlockPos(7, 3, 7))), "outside is not");
                    helper.assertTrue(helper.getBlockState(d).getValue(com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlock.LIT), "lamp on");
                    helper.setBlock(5, 3, 3, Blocks.AIR); // punch a hole into the wall
                })
                .thenWaitUntil(() -> helper.assertTrue(!be.room().isScanning() && !be.room().sealed(), "the hole must make the room leak"))
                .thenExecute(() -> helper.assertFalse(SealedRooms.providesOxygen(helper.getLevel(), inside), "leaking room is not breathable"))
                .thenExecute(() -> helper.setBlock(5, 3, 3, Blocks.GLASS))
                .thenWaitUntil(() -> helper.assertTrue(be.room().sealed(), "glass seals it again"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void airlockDoorSealsWhenClosed(GameTestHelper helper) {
        BlockPos d = buildBox(helper);
        OxygenDistributorBlockEntity be = helper.getBlockEntity(d);
        BlockState lower = SuitModule.AIRLOCK_DOOR.get().defaultBlockState().setValue(AirlockDoorBlock.FACING, Direction.EAST);
        helper.setBlock(new BlockPos(5, 2, 3), lower);
        helper.setBlock(new BlockPos(5, 3, 3), lower.setValue(AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(be.room().sealed(), "closed airlock is airtight"))
                .thenExecute(() -> {
                    BlockPos abs = helper.absolutePos(new BlockPos(5, 2, 3));
                    ((AirlockDoorBlock) SuitModule.AIRLOCK_DOOR.get()).setOpen(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), true);
                    helper.assertTrue(helper.getBlockState(new BlockPos(5, 3, 3)).getValue(AirlockDoorBlock.OPEN), "upper half follows");
                })
                .thenWaitUntil(() -> helper.assertTrue(!be.room().isScanning() && !be.room().sealed(), "open airlock leaks"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void distributorUsesOxygenByVolume(GameTestHelper helper) {
        helper.assertTrue(OxygenDistributorBlockEntity.usePerTick(0, 0) == 1, "an empty room uses one breathing tick per tick");
        long total = 0;
        for (int t = 0; t < 20; t++) {
            total += OxygenDistributorBlockEntity.usePerTick(400, t);
        }
        helper.assertTrue(total == 220, "400 blocks use 220 ticks of oxygen per second, got " + total);
        helper.succeed();
    }

    // ------------------------------------------------------------------ filler

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fillerRefillsTanksWithPower(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, SuitModule.OXYGEN_FILLER.get());
        OxygenFillerBlockEntity be = helper.getBlockEntity(pos);
        be.setStack(new ItemStack(SuitModule.OXYGEN_TANK.get()));
        helper.runAfterDelay(20, () -> helper.assertTrue(OxygenTankItem.getOxygen(be.getStack()) == 0, "no power, no filling"));
        helper.runAfterDelay(21, () -> be.getEnergy().receiveEnergy(4000, false));
        helper.succeedWhen(() -> helper.assertTrue(OxygenTankItem.getOxygen(be.getStack()) >= OxygenFillerBlockEntity.FILL_RATE * 10,
                "tank fills with power, has " + OxygenTankItem.getOxygen(be.getStack())));
    }

}
