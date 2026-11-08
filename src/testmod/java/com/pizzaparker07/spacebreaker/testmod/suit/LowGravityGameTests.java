package com.pizzaparker07.spacebreaker.testmod.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.LowGravity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** How low gravity feels: things that are not alive fall at the local rate too. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LowGravityGameTests {
    private static final String EMPTY = "empty";
    /** Applied factor on the Moon with the default (balanced) realism: 0.165^0.75. */
    private static final double MOON_F = Math.pow(0.1652, 0.75);

    private LowGravityGameTests() {
    }

    /** Ticks for something to fall {@code height} blocks from rest, vanilla style (pull, then drag). */
    private static int fallTicks(double gravity, double drag, double height) {
        double y = 0, vy = 0;
        int t = 0;
        while (y > -height && t < 10_000) {
            y += vy;
            vy = (vy - gravity) * drag;
            t++;
        }
        return t;
    }

    @GameTest(template = EMPTY)
    public static void onlyThingsThatAreNotAliveAreTouched(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(LowGravity.affected(new ItemEntity(level, 0, 0, 0, new ItemStack(Items.STONE))), "dropped items fall at the local rate");
        helper.assertTrue(LowGravity.affected(EntityType.ARROW.create(level)), "arrows too");
        helper.assertTrue(LowGravity.affected(EntityType.TNT.create(level)), "and primed TNT");
        helper.assertFalse(LowGravity.affected(EntityType.PIG.create(level)), "living things already use the gravity attribute");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aDroppedItemLandsAsSlowlyAsThePlayerOnTheMoon(GameTestHelper helper) {
        double itemG = 0.04;
        helper.assertTrue(LowGravity.restoredFall(itemG, 1.0) == 0.0, "nothing changes on Earth");
        helper.assertTrue(LowGravity.restoredFall(itemG, 2.5) == 0.0, "nor on heavier worlds (vanilla pull is kept)");
        helper.assertTrue(Math.abs(LowGravity.restoredFall(itemG, 0.0) - itemG) < 1e-12, "in free fall the whole pull is given back");
        double felt = itemG - LowGravity.restoredFall(itemG, MOON_F);
        helper.assertTrue(Math.abs(felt - itemG * MOON_F) < 1e-12, "what is left is the local share of the pull");
        int earth = fallTicks(itemG, 0.98, 3);
        int moon = fallTicks(felt, 0.98, 3);
        int player = fallTicks(0.08 * MOON_F, 0.98, 3);
        helper.assertTrue(moon > earth * 1.7, "a stone dropped from 3 blocks takes clearly longer on the Moon (" + earth + " vs " + moon + " ticks)");
        helper.assertTrue(Math.abs(moon - player * Math.sqrt(2)) < 4, "and keeps the same proportion to a falling player as on Earth");
        helper.succeed();
    }

    /** Blocks covered by one sprint jump, vanilla style, with the air push scaled by {@code control} and the given drag. */
    static double sprintJump(double f, double control, double drag) {
        double vy = 0.42 * Math.pow(f, 0.25), y = 0, vx = 0.28 + 0.2, x = 0;
        for (int t = 0; t < 2000; t++) {
            y += vy;
            x += vx;
            vy = (vy - 0.08 * f) * 0.98;
            vx = (vx + 0.026 * control) * drag;
            if (y <= 0) {
                break;
            }
        }
        return x;
    }

    private static double before(double f) {
        return sprintJump(f, 1.0, LowGravity.VANILLA_AIR_DRAG);
    }

    private static double after(double f) {
        return sprintJump(f, LowGravity.airControl(f), LowGravity.airDrag(f));
    }

    @GameTest(template = EMPTY)
    public static void lowGravityHopsMostlyKeepTheirTakeOff(GameTestHelper helper) {
        helper.assertTrue(LowGravity.airControl(1.0) == 1.0 && LowGravity.airControl(2.5) == 1.0, "full control on Earth and heavier worlds");
        helper.assertTrue(LowGravity.airDrag(1.0) == LowGravity.VANILLA_AIR_DRAG && LowGravity.airDrag(0.0) == LowGravity.VANILLA_AIR_DRAG,
                "vanilla drag on Earth and in microgravity");
        helper.assertTrue(Math.abs(LowGravity.airControl(MOON_F) - Math.sqrt(MOON_F)) < 1e-9, "about half the steering on the Moon");
        double moon = after(MOON_F) / before(MOON_F);
        // this tick model runs about 12% long against the game (measured: 7.69 blocks vanilla, ~0.92 of it wanted)
        helper.assertTrue(moon > 0.98 && moon < 1.1, "a Moon sprint hop keeps about its length in this model (" + moon + ")");
        double moonReal = after(0.1652) / before(0.1652);
        helper.assertTrue(moonReal > 0.98 && moonReal < 1.1, "also at realistic gravity (" + moonReal + ")");
        double ceresF = Math.pow(0.0289, 0.75);
        helper.assertTrue(LowGravity.airControl(ceresF) == LowGravity.MIN_AIR_CONTROL, "never below the floor on the lightest worlds");
        double ceres = after(ceresF) / before(ceresF);
        helper.assertTrue(ceres > 0.5 && ceres < 0.7, "a Ceres hop gets clearly shorter (" + before(ceresF) + " -> " + after(ceresF) + ")");
        helper.assertTrue(Math.abs(after(1.0) - before(1.0)) < 1e-9, "Earth jumps unchanged");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void floatingMinersAreSlowedLessAndNotAtAllOnATether(GameTestHelper helper) {
        helper.assertTrue(LowGravity.floatingMiningBoost(0.0, false, false) * 0.2F == 0.5F, "floating in orbit: two times slower, not five");
        helper.assertTrue(LowGravity.floatingMiningBoost(0.0, false, true) * 0.2F == 1F, "on a tether: full speed");
        helper.assertTrue(LowGravity.floatingMiningBoost(0.0, true, false) == 1F, "standing (magnetic boots): vanilla");
        helper.assertTrue(LowGravity.floatingMiningBoost(MOON_F, false, true) == 1F, "a jump on the Moon keeps the vanilla penalty");
        helper.assertTrue(LowGravity.floatingMiningBoost(1.0, false, false) == 1F, "Earth unchanged");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void landingsKickUpALightPuffOnlyInLowGravity(GameTestHelper helper) {
        helper.assertTrue(LowGravity.landingDust(1.0, 10) == 0, "nothing extra on Earth");
        helper.assertTrue(LowGravity.landingDust(0.0, 10) == 0, "nor in microgravity (no landing to speak of)");
        helper.assertTrue(LowGravity.landingDust(MOON_F, 1.0) == 0, "a step down raises nothing");
        int hop = LowGravity.landingDust(MOON_F, 2.0), drop = LowGravity.landingDust(MOON_F, 30);
        helper.assertTrue(hop > 0 && hop < drop && drop <= 10, "a hop raises a little dust, a long drop more, never a cloud");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wingsNeedAir(GameTestHelper helper) {
        helper.assertTrue(LowGravity.elytraLift(0.0) == 0.0, "no lift in vacuum");
        helper.assertTrue(LowGravity.elytraLift(101.325) == 1.0 && LowGravity.elytraLift(9200) == 1.0, "full lift on Earth and in denser air");
        helper.assertTrue(LowGravity.elytraLift(146.7) == 1.0, "Titan's thick air carries wings fine");
        double mars = LowGravity.elytraLift(0.61);
        helper.assertTrue(mars > 0.15 && mars < 0.19, "Mars's thin air gives about a sixth of the lift (" + mars + ")");
        helper.succeed();
    }
}
