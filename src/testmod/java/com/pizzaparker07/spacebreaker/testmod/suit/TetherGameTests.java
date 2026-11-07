package com.pizzaparker07.spacebreaker.testmod.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SurvivalData;
import com.pizzaparker07.spacebreaker.content.suit.Tether;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TetherGameTests {
    private static final String EMPTY = "empty";

    private TetherGameTests() {
    }

    private static Player clipped(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, Blocks.IRON_BLOCK);
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack tether = new ItemStack(SuitModule.SAFETY_TETHER.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, tether);
        BlockPos abs = helper.absolutePos(rel);
        tether.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        return p;
    }

    @GameTest(template = EMPTY)
    public static void tetherClipsAndUnclipsOnTheSameBlock(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        Player p = clipped(helper, rel);
        BlockPos abs = helper.absolutePos(rel);
        helper.assertTrue(Tether.anchor(p).filter(abs::equals).isPresent(), "using the tether on a block must clip it there");
        ItemStack tether = p.getMainHandItem();
        tether.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        helper.assertTrue(Tether.anchor(p).isEmpty(), "using it on the anchor again must unclip");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tetherComesLooseWhenTheAnchorIsBroken(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        Player p = clipped(helper, rel);
        helper.setBlock(rel, Blocks.AIR);
        p.tickCount = 10;
        Tether.tick(p);
        helper.assertTrue(Tether.anchor(p).isEmpty(), "the line must come loose with its anchor block");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tetherComesLooseWithoutTheItem(GameTestHelper helper) {
        Player p = clipped(helper, new BlockPos(2, 1, 2));
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.tickCount = 10;
        Tether.tick(p);
        helper.assertTrue(Tether.anchor(p).isEmpty(), "dropping the tether must unclip it");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tetherSnapsFarBeyondItsLength(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        Player p = clipped(helper, rel);
        Vec3 a = Vec3.atCenterOf(helper.absolutePos(rel));
        p.setPos(a.x + Tether.LENGTH - 2, a.y, a.z);
        Tether.tick(p);
        helper.assertTrue(Tether.anchor(p).isPresent(), "within its length the line must hold");
        p.setPos(a.x + Tether.BREAK_DISTANCE + 2, a.y, a.z);
        Tether.tick(p);
        helper.assertTrue(Tether.anchor(p).isEmpty(), "far beyond its length (teleport) the clip must let go");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tautTetherStopsDriftingAway(GameTestHelper helper) {
        Vec3 anchor = Vec3.ZERO;
        Vec3 slack = Tether.constrain(new Vec3(5, 0, 0), new Vec3(0.5, 0, 0), anchor, Tether.LENGTH);
        helper.assertTrue(slack.x == 0.5, "a slack line must not change the motion");
        Vec3 taut = Tether.constrain(new Vec3(Tether.LENGTH + 0.5, 0, 0), new Vec3(0.5, 0.1, 0), anchor, Tether.LENGTH);
        helper.assertTrue(taut.x < 0, "a taut line must stop the outward drift and pull back, got " + taut.x);
        helper.assertTrue(Math.abs(taut.y - 0.1) < 1e-9, "sideways motion along the circle must stay");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tetherIsSavedWithThePlayer(GameTestHelper helper) {
        Player p = clipped(helper, new BlockPos(2, 1, 2));
        SurvivalData d = LifeSupport.data(p);
        var tag = SurvivalData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, d).getOrThrow();
        SurvivalData back = SurvivalData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
        helper.assertTrue(d.tether.equals(back.tether) && d.tetherDim.equals(back.tetherDim), "anchor must survive save/load");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void maneuveringUnitBurnsGasOnlyWhileThrustingInTheAir(GameTestHelper helper) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        com.pizzaparker07.spacebreaker.content.suit.SuitTanks.setManeuveringUnit(torso,
                com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem.filled(SuitModule.MANEUVERING_UNIT.get()));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, torso);
        p.setPos(helper.absoluteVec(new Vec3(2, 4, 2)));
        p.setOnGround(false);
        SurvivalData d = LifeSupport.data(p);
        int full = com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem.CAPACITY;
        com.pizzaparker07.spacebreaker.content.suit.ZeroG.tickThrust(p);
        helper.assertTrue(fuel(p) == full, "no gas may burn without thrust");
        d.thrusting = true;
        d.thrustDir = new Vec3(1, 0, 0);
        com.pizzaparker07.spacebreaker.content.suit.ZeroG.tickThrust(p); // also sends the thruster puffs
        helper.assertTrue(fuel(p) == full - 1, "one tick of gas per tick of thrust, got " + fuel(p));
        p.setOnGround(true);
        com.pizzaparker07.spacebreaker.content.suit.ZeroG.tickThrust(p);
        helper.assertTrue(fuel(p) == full - 1, "no gas may burn standing on the ground");
        helper.succeed();
    }

    private static int fuel(Player p) {
        ItemStack torso = com.pizzaparker07.spacebreaker.content.suit.SuitTanks.wornTorso(p);
        return com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem.fuel(
                com.pizzaparker07.spacebreaker.content.suit.SuitTanks.maneuveringUnit(torso));
    }

    @GameTest(template = EMPTY)
    public static void jetpackHoversOnTheMoonButOnlySoftensAFallOnEarth(GameTestHelper helper) {
        // vanilla gravity is 0.08 blocks/tick² at 1 g, then 2 % drag
        for (double g : new double[]{0.165, 1.0}) {
            double vy = 0;
            for (int t = 0; t < 200; t++) {
                vy = com.pizzaparker07.spacebreaker.content.suit.ZeroG.lift(vy, true, false, 1.0);
                vy = (vy - 0.08 * g) * 0.98;
            }
            if (g < 1) {
                helper.assertTrue(vy > 0.1, "holding jump on the Moon climbs steadily, vy " + vy);
            } else {
                helper.assertTrue(vy < 0 && vy > -1.2, "on Earth the unit only slows the fall, vy " + vy);
            }
        }
        double fall = -0.6;
        for (int t = 0; t < 40; t++) {
            fall = com.pizzaparker07.spacebreaker.content.suit.ZeroG.lift(fall, false, true, 1.0);
            fall = (fall - 0.08 * 0.165) * 0.98;
        }
        helper.assertTrue(fall > -0.15 && fall < 0, "sneaking on the Moon brakes a fall to a gentle glide, vy " + fall);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.ZeroG.lift(-0.05, false, true, 1.0) == -0.05,
                "no gas is spent while already gliding slowly");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void helmetRadioRecognisesMissionControlLines(GameTestHelper helper) {
        var en = com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.ORBIT_REACHED.event(false).getLocation();
        var tr = com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.OXYGEN_LOW.event(true).getLocation();
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetRadioLines.lineOf(en)
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.ORBIT_REACHED, "English line recognised: " + en);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetRadioLines.lineOf(tr)
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.OXYGEN_LOW, "Turkish line recognised: " + tr);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetRadioLines.lineOf(
                com.pizzaparker07.spacebreaker.content.audio.SbSounds.RADIO_IN.get().getLocation()) == null, "radio tones are not lines");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void helmetLampLongBeamReachesFurtherAndNarrower(GameTestHelper helper) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.toggle(p) == null, "no helmet, nothing to switch");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.longBeam(p), "lamps start wide");
        helper.assertTrue(Boolean.TRUE.equals(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.toggle(p)), "switches to long beam");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.longBeam(p), "the helmet remembers the long beam");
        double d = 15;
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.range(true) > com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.range(false),
                "the long beam reaches further");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.poolRadius(d, true)
                < com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.poolRadius(d, false), "and lights a narrower spot");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.strength(30, true) > 0
                && com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.strength(30, false) == 0, "30 blocks: only the long beam gets there");
        helper.assertTrue(Boolean.FALSE.equals(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.toggle(p)), "and back to wide");
        helper.succeed();
    }
}
