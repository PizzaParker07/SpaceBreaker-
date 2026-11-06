package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.world.CoronaFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.RupesFeature;
import com.pizzaparker07.spacebreaker.content.uranus.UranusData;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import com.pizzaparker07.spacebreaker.content.uranus.entity.CliffClinger;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UranusGameTests {
    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void mirandaHasAGiantCliffAndCoronae(GameTestHelper helper) {
        helper.assertTrue(WorldTests.stem(helper, "miranda").generator() instanceof PlanetChunkGenerator, "Miranda uses the planet engine");
        PlanetTerrain t = WorldTests.generator(helper, "miranda", 8L).terrain();
        RupesFeature.Config cliff = UranusData.cliff();
        double cz = RupesFeature.centreZ(t, cliff);
        double a = Math.toRadians(cliff.angleDeg());
        // across the middle of the cliff: high side untouched, low side far below
        double nx = -Math.sin(a);
        double nz = Math.cos(a);
        double high = RupesFeature.offset(t, cliff, cliff.x() - nx * 40, cz - nz * 40);
        double low = RupesFeature.offset(t, cliff, cliff.x() + nx * 40, cz + nz * 40);
        helper.assertTrue(high == 0, "the high side keeps its ground: " + high);
        helper.assertTrue(low < -80, "the low side lies more than 80 blocks down: " + low);
        double end = RupesFeature.offset(t, cliff, cliff.x() + Math.cos(a) * cliff.length() * 0.7 + nx * 40, cz + Math.sin(a) * cliff.length() * 0.7 + nz * 40);
        helper.assertTrue(end == 0, "the cliff ends: " + end);
        CoronaFeature.Config cor = UranusData.coronae();
        CoronaFeature.Corona c = cor.coronae().getFirst();
        double ccz = -c.latitude() / 90.0 * t.profile().poleDistance();
        int bright = 0;
        int dark = 0;
        for (int i = 0; i < 200; i++) {
            double b = CoronaFeature.band(t, cor, c.x() + (i - 100) * 1.7, ccz + 3);
            if (b > 0) {
                bright++;
            } else if (b <= 0) {
                dark++;
            }
        }
        helper.assertTrue(bright > 30 && dark > 30, "alternating bright and dark ridges: " + bright + " / " + dark);
        helper.assertTrue(Double.isNaN(CoronaFeature.band(t, cor, c.x(), ccz + c.halfLength() * 3)), "plains outside the coronae");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void cliffClingerClimbsAndNeverFalls(GameTestHelper helper) {
        CliffClinger c = helper.spawn(UranusEntities.CLIFF_CLINGER.get(), new BlockPos(2, 2, 2));
        helper.assertTrue(c.getNavigation() instanceof WallClimberNavigation, "it paths up walls");
        helper.assertFalse(c.causeFallDamage(50F, 1F, helper.getLevel().damageSources().fall()), "and a fall never hurts it");
        helper.assertTrue(c.isFood(new ItemStack(Items.COAL)), "it breeds on carbon (coal)");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void verticalExplorationIsInTheTree(GameTestHelper helper) {
        var node = Tech.all(helper.getLevel().registryAccess()).get(Spacebreaker.id("vertical_exploration"));
        helper.assertTrue(node != null && node.tier() == 4, "vertical exploration is a tier-4 tech");
        helper.assertTrue(UranusModule.CARBON_ORE.get().defaultBlockState().is(net.minecraft.tags.BlockTags.COAL_ORES), "the carbon deposit counts as coal ore");
        helper.succeed();
    }
}
