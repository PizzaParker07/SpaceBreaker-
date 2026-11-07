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

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void arielHasFlatFlooredRiftValleys(GameTestHelper helper) {
        helper.assertTrue(WorldTests.stem(helper, "ariel").generator() instanceof PlanetChunkGenerator, "Ariel uses the planet engine");
        PlanetTerrain t = WorldTests.generator(helper, "ariel", 3L).terrain();
        var cfg = UranusData.chasmata();
        var g = cfg.grabens().getFirst();
        double cz = -g.latitude() / 90.0 * t.profile().poleDistance();
        double a = Math.toRadians(g.angleDeg());
        double nx = -Math.sin(a);
        double nz = Math.cos(a);
        double[] floor = new double[1];
        // across the valley: plains on both sides, a flat sunken floor between
        double out1 = com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.offset(t, cfg, g.x() - nx * 80, cz - nz * 80, floor);
        double out2 = com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.offset(t, cfg, g.x() + nx * 80, cz + nz * 80, floor);
        double f1 = com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.offset(t, cfg, g.x() + nx * g.width() * 0.3, cz + nz * g.width() * 0.3,
                floor);
        helper.assertTrue(floor[0] > 0, "that point is on the valley floor");
        double f2 = com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.offset(t, cfg, g.x() - nx * g.width() * 0.3, cz - nz * g.width() * 0.3,
                floor);
        helper.assertTrue(out1 == 0 && out2 == 0, "plains on both sides: " + out1 + " " + out2);
        helper.assertTrue(f1 < -g.depth() * 0.8 && Math.abs(f1 - f2) < 3, "a deep, flat floor: " + f1 + " " + f2);
        var moth = helper.spawn(UranusEntities.FROST_MOTH.get(), new BlockPos(2, 3, 2));
        helper.assertTrue(moth.getType().getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT, "the frost moth is an ambient flutterer");
        var node = Tech.all(helper.getLevel().registryAccess()).get(Spacebreaker.id("ice_shell_tectonics"));
        helper.assertTrue(node != null && node.tier() == 4, "ice shell tectonics is a tier-4 tech");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void titaniaHasGreatCanyonsAndACraterBasin(GameTestHelper helper) {
        helper.assertTrue(WorldTests.stem(helper, "titania").generator() instanceof PlanetChunkGenerator, "Titania uses the planet engine");
        PlanetTerrain t = WorldTests.generator(helper, "titania", 5L).terrain();
        var cfg = UranusData.canyons();
        var g = cfg.grabens().getFirst();
        double cz = -g.latitude() / 90.0 * t.profile().poleDistance();
        double[] floor = new double[1];
        double centre = com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature.offset(t, cfg, g.x(), cz, floor);
        helper.assertTrue(floor[0] > 0 && centre < -50, "a canyon more than 50 blocks deep: " + centre);
        var ariel = UranusData.chasmata().grabens().getFirst();
        helper.assertTrue(g.width() > ariel.width() && g.depth() > ariel.depth(), "wider and deeper than Ariel's valleys");
        var basin = UranusData.greatCrater();
        double bz = com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.centreZ(t, basin);
        double peak = com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.offset(t, basin, basin.x(), bz);
        double bfloor = com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.offset(t, basin, basin.x() + basin.radius() * 0.5, bz);
        helper.assertTrue(bfloor < -15 && peak > bfloor + 20, "a great crater with a central peak: " + bfloor + " / " + peak);
        var roller = helper.spawn(UranusEntities.SCREE_ROLLER.get(), new BlockPos(2, 2, 2));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        roller.hurt(helper.getLevel().damageSources().playerAttack(player), 2F);
        helper.assertTrue(roller.isRolled(), "a hit curls it into a ball");
        float before = roller.getHealth();
        roller.invulnerableTime = 0;
        roller.hurt(helper.getLevel().damageSources().playerAttack(player), 10F);
        helper.assertTrue(before - roller.getHealth() < 4F, "curled, it takes a fraction of the damage");
        var node = Tech.all(helper.getLevel().registryAccess()).get(Spacebreaker.id("tectonic_mapping"));
        helper.assertTrue(node != null && node.tier() == 5, "tectonic mapping is a tier-5 tech");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void umbrielIsDarkWithABrightRingInWunda(GameTestHelper helper) {
        helper.assertTrue(WorldTests.stem(helper, "umbriel").generator() instanceof PlanetChunkGenerator, "Umbriel uses the planet engine");
        PlanetTerrain t = WorldTests.generator(helper, "umbriel", 7L).terrain();
        helper.assertTrue(WorldTests.profile(helper, "umbriel").poleDistance() < WorldTests.profile(helper, "titania").poleDistance(),
                "smaller than Titania");
        var wunda = com.pizzaparker07.spacebreaker.content.uranus.Umbriel.wunda();
        double wz = com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.centreZ(t, wunda);
        double floor = com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.offset(t, wunda, wunda.x(), wz);
        double far = com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature.offset(t, wunda, wunda.x() + wunda.radius() * 3, wz);
        helper.assertTrue(floor < -10 && far == 0, "Wunda is a sunken crater floor: " + floor);
        var ring = com.pizzaparker07.spacebreaker.content.uranus.Umbriel.ring();
        var mark = ring.marks().getFirst();
        helper.assertTrue(mark.radius() < wunda.radius(), "the ring lies on the crater floor");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.belt.world.RingFeature.markAt(t, ring, mark.x() + mark.radius(), wz) != null,
                "frost on the ring");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.belt.world.RingFeature.markAt(t, ring, mark.x(), wz) == null,
                "dark in the ring's middle");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.belt.world.RingFeature.markAt(t, ring, mark.x() + mark.radius() * 2, wz) == null,
                "dark outside it");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void gloamHopperBlindsWhatHitsIt(GameTestHelper helper) {
        var hopper = helper.spawn(UranusEntities.GLOAM_HOPPER.get(), new BlockPos(2, 2, 2));
        var near = helper.spawn(UranusEntities.SCREE_ROLLER.get(), new BlockPos(4, 2, 2));
        var other = helper.spawn(UranusEntities.GLOAM_HOPPER.get(), new BlockPos(2, 2, 4));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(hopper.cloudReady(), "ready to kick up dust");
        hopper.hurt(helper.getLevel().damageSources().playerAttack(player), 1F);
        helper.assertTrue(near.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "the dust cloud blinds what is close");
        helper.assertFalse(other.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "but not other hoppers");
        helper.assertFalse(hopper.cloudReady(), "then it needs a while before the next cloud");
        helper.assertTrue(hopper.isFood(new ItemStack(Items.COAL)), "breeds on coal");
        helper.succeed();
    }
}
