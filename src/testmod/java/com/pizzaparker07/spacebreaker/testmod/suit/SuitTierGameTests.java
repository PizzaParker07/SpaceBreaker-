package com.pizzaparker07.spacebreaker.testmod.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitProtection;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import com.pizzaparker07.spacebreaker.content.suit.SuitTier;
import com.pizzaparker07.spacebreaker.content.suit.SuitUpgrade;
import com.pizzaparker07.spacebreaker.content.suit.SuitUpgradeRecipe;
import com.pizzaparker07.spacebreaker.content.suit.SurvivalData;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/** Suit tiers against real environments, technology gating and upgrade modules. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SuitTierGameTests {
    private static final String EMPTY = "empty";
    // real surface conditions (data/spacebreaker/spacebreaker/celestial_body)
    private static final double TITAN_C = -179, TITAN_KPA = 146.7;
    private static final double VENUS_C = 464, VENUS_KPA = 9200;
    private static final double IO_MSV_DAY = 36000, EUROPA_MSV_DAY = 5400;

    private SuitTierGameTests() {
    }

    private static Player wearing(GameTestHelper helper, GameType mode, List<DeferredItem<SpaceSuitItem>> set) {
        Player p = helper.makeMockPlayer(mode);
        EquipmentSlot[] slots = SuitProtection.SLOTS;
        for (int i = 0; i < 4; i++) {
            p.setItemSlot(slots[i], new ItemStack(set.get(i).get()));
        }
        return p;
    }

    private static List<DeferredItem<SpaceSuitItem>> set(SuitTier tier) {
        if (tier == SuitTier.THERMAL) {
            return List.of(SuitModule.THERMAL_HELMET, SuitModule.THERMAL_CHESTPLATE, SuitModule.THERMAL_LEGGINGS, SuitModule.THERMAL_BOOTS);
        }
        if (tier == SuitTier.HEATED) {
            return List.of(SuitModule.HEATED_HELMET, SuitModule.HEATED_CHESTPLATE, SuitModule.HEATED_LEGGINGS, SuitModule.HEATED_BOOTS);
        }
        if (tier == SuitTier.RADIATION) {
            return List.of(SuitModule.RADIATION_HELMET, SuitModule.RADIATION_CHESTPLATE, SuitModule.RADIATION_LEGGINGS, SuitModule.RADIATION_BOOTS);
        }
        return List.of(SuitModule.EVA_HELMET, SuitModule.EVA_CHESTPLATE, SuitModule.EVA_LEGGINGS, SuitModule.EVA_BOOTS);
    }

    private static double bodyTarget(Player p, double c, double kpa) {
        return LifeSupport.targetBodyTemp(LifeSupport.effectiveAmbient(p, c, kpa));
    }

    @GameTest(template = EMPTY)
    public static void titanNeedsTheHeatedSuit(GameTestHelper helper) {
        Player mk1 = wearing(helper, GameType.CREATIVE, set(SuitTier.MK1));
        helper.assertTrue(bodyTarget(mk1, TITAN_C, TITAN_KPA) < 35.0, "Titan's dense cold air chills a Mk1 suit, target " + bodyTarget(mk1, TITAN_C, TITAN_KPA));
        helper.assertTrue(bodyTarget(mk1, TITAN_C, 0.0) >= SurvivalData.NORMAL_BODY_C - 0.5, "the same cold in vacuum is bearable");
        Player heated = wearing(helper, GameType.CREATIVE, set(SuitTier.HEATED));
        helper.assertTrue(bodyTarget(heated, TITAN_C, TITAN_KPA) == SurvivalData.NORMAL_BODY_C, "a heated suit is comfortable on Titan");
        helper.assertTrue(LifeSupport.convection(0) == 1.0 && LifeSupport.convection(VENUS_KPA) == 4.0, "convection: 1x in vacuum, capped at 4x");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void venusNeedsTheThermalSuit(GameTestHelper helper) {
        Player mk1 = wearing(helper, GameType.CREATIVE, set(SuitTier.MK1));
        helper.assertTrue(bodyTarget(mk1, VENUS_C, VENUS_KPA) > 41.0, "Venus cooks a Mk1 suit");
        Player thermal = wearing(helper, GameType.CREATIVE, set(SuitTier.THERMAL));
        helper.assertTrue(bodyTarget(thermal, VENUS_C, VENUS_KPA) == SurvivalData.NORMAL_BODY_C, "a thermal suit survives Venus");
        helper.assertTrue(bodyTarget(thermal, 430, 0) == SurvivalData.NORMAL_BODY_C, "and Mercury's noon");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void jupiterMoonsNeedRadiationHardening(GameTestHelper helper) {
        double mk1 = IO_MSV_DAY * LifeSupport.radiationFactor(wearing(helper, GameType.CREATIVE, set(SuitTier.MK1)));
        double hard = IO_MSV_DAY * LifeSupport.radiationFactor(wearing(helper, GameType.CREATIVE, set(SuitTier.RADIATION)));
        helper.assertTrue(mk1 > 20000, "Io's surface is lethal in a Mk1 suit within minutes, " + mk1 + " mSv/day");
        helper.assertTrue(hard < 1500, "radiation hardening makes short Io trips survivable, " + hard + " mSv/day");
        double europa = EUROPA_MSV_DAY * SuitTier.RADIATION.radiationFactor();
        // one in-game day (20 min) on Europa stays below the 1 Sv sickness threshold
        helper.assertTrue(europa < 1000, "a game day on Europa stays below 1 Sv in a hardened suit, " + europa);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mixedSuitIsAsGoodAsItsWeakestPiece(GameTestHelper helper) {
        Player p = wearing(helper, GameType.CREATIVE, set(SuitTier.THERMAL));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
        SuitProtection.Rating r = SuitProtection.rating(p);
        helper.assertTrue(r != null && r.maxTempC() == SuitTier.MK1.maxTempC(), "Mk1 boots cap a thermal suit's heat rating");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tierProtectionNeedsItsTechnology(GameTestHelper helper) {
        Player p = wearing(helper, GameType.SURVIVAL, set(SuitTier.THERMAL));
        SuitProtection.Rating locked = SuitProtection.rating(p);
        helper.assertTrue(locked != null && locked.maxTempC() == SuitTier.MK1.maxTempC(), "without the technology a thermal suit protects like Mk1");
        Tech.learn(p, SuitTier.THERMAL.tech());
        SuitProtection.Rating known = SuitProtection.rating(p);
        helper.assertTrue(known != null && known.maxTempC() == SuitTier.THERMAL.maxTempC(), "researched: full thermal rating");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgradeModulesStack(GameTestHelper helper) {
        Player p = wearing(helper, GameType.CREATIVE, set(SuitTier.MK1));
        for (EquipmentSlot slot : SuitProtection.SLOTS) {
            p.getItemBySlot(slot).set(SuitModule.SUIT_UPGRADE.get(), SuitUpgrade.THERMAL_REGULATOR);
        }
        SuitProtection.Rating r = SuitProtection.rating(p);
        helper.assertTrue(r.minTempC() == SuitTier.MK1.minTempC() - 4 * SuitProtection.THERMAL_MODULE_C, "four regulators widen the cold end");
        helper.assertTrue(r.maxTempC() == SuitTier.MK1.maxTempC() + 4 * SuitProtection.THERMAL_MODULE_C, "and the hot end");
        for (EquipmentSlot slot : SuitProtection.SLOTS) {
            p.getItemBySlot(slot).set(SuitModule.SUIT_UPGRADE.get(), SuitUpgrade.RADIATION_SHIELD);
        }
        double f = SuitProtection.rating(p).radiationFactor();
        helper.assertTrue(Math.abs(f - SuitTier.MK1.radiationFactor() * Math.pow(SuitProtection.SHIELD_MODULE_FACTOR, 4)) < 1e-9, "shields multiply, " + f);
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        helper.assertTrue(SuitTanks.slots(torso) == 2, "two tank slots by default");
        torso.set(SuitModule.SUIT_UPGRADE.get(), SuitUpgrade.EXTRA_OXYGEN);
        helper.assertTrue(SuitTanks.slots(torso) == 3, "extra oxygen module adds a third slot");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void smithingFitsModules(GameTestHelper helper) {
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        torso.set(SuitModule.SUN_VISOR.get(), false);
        ItemStack out = SuitUpgradeRecipe.result(torso, new ItemStack(SuitModule.EXTRA_OXYGEN_MODULE.get()));
        helper.assertTrue(out != null && SuitUpgrade.of(out) == SuitUpgrade.EXTRA_OXYGEN, "torso takes the extra oxygen module");
        helper.assertTrue(SuitUpgradeRecipe.result(new ItemStack(SuitModule.EVA_LEGGINGS.get()), new ItemStack(SuitModule.EXTRA_OXYGEN_MODULE.get())) == null,
                "leggings cannot take a torso-only module");
        helper.assertTrue(SuitUpgradeRecipe.result(out, new ItemStack(SuitModule.EXTRA_OXYGEN_MODULE.get())) == null, "the same module twice is refused");
        ItemStack boots = SuitUpgradeRecipe.result(new ItemStack(SuitModule.HEATED_BOOTS.get()), new ItemStack(SuitModule.THERMAL_REGULATOR_MODULE.get()));
        helper.assertTrue(boots != null && boots.is(SuitModule.HEATED_BOOTS.get()), "result keeps the piece");
        helper.succeed();
    }

    // QA-4: the Moon is the first destination; its night (-173 °C, vacuum) must be fully covered by the Mk1 suit
    private static final double MOON_NIGHT_C = -173, MERCURY_NIGHT_C = -180, MERCURY_DAY_C = 430;

    @GameTest(template = EMPTY)
    public static void mk1SurvivesTheLunarNight(GameTestHelper helper) {
        Player p = wearing(helper, GameType.SURVIVAL, set(SuitTier.MK1));
        SuitTanks.insert(p.getItemBySlot(EquipmentSlot.CHEST),
                com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        helper.assertTrue(bodyTarget(p, MOON_NIGHT_C, 0.0) == SurvivalData.NORMAL_BODY_C,
                "Mk1 must keep the body at 37 °C in the lunar night, target " + bodyTarget(p, MOON_NIGHT_C, 0.0));
        float health = p.getHealth();
        LifeSupport.Env night = new LifeSupport.Env(false, MOON_NIGHT_C, 0.0, 0.0);
        for (int i = 0; i < 20 * 60 * 3; i++) {
            LifeSupport.tick(p, night);
        }
        SurvivalData d = LifeSupport.data(p);
        helper.assertTrue(d.bodyTempC >= SurvivalData.NORMAL_BODY_C - 0.5, "body temperature fell to " + d.bodyTempC);
        helper.assertTrue(p.getHealth() >= health, "no damage in three minutes of lunar night, health " + p.getHealth());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void thermalSuitCoversAllOfMercury(GameTestHelper helper) {
        Player p = wearing(helper, GameType.CREATIVE, set(SuitTier.THERMAL));
        Tech.learn(p, SuitTier.THERMAL.tech());
        helper.assertTrue(bodyTarget(p, MERCURY_NIGHT_C, 0.0) == SurvivalData.NORMAL_BODY_C, "thermal suit in Mercury's night");
        helper.assertTrue(bodyTarget(p, MERCURY_DAY_C, 0.0) == SurvivalData.NORMAL_BODY_C, "thermal suit in Mercury's day");
        Player heated = wearing(helper, GameType.CREATIVE, set(SuitTier.HEATED));
        Tech.learn(heated, SuitTier.HEATED.tech());
        Tech.learn(p, SuitTier.THERMAL.tech());
        helper.assertTrue(bodyTarget(p, TITAN_C, TITAN_KPA) < bodyTarget(heated, TITAN_C, TITAN_KPA),
                "Titan's dense cold air still calls for the heated suit");
        helper.succeed();
    }

    private static com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload status(float ambientC, int flags, int minC) {
        return new com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload(1000, 2000, 300, 37F, ambientC, 0F, 0F, 0F, 0.165F,
                0, 100, flags, minC, 130, 30);
    }

    @GameTest(template = EMPTY)
    public static void visorFrostsBeyondTheColdRating(GameTestHelper helper) {
        int sealed = com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload.SEALED;
        int trap = com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload.COLD_TRAP;
        int pressurised = com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload.PRESSURISED;
        helper.assertTrue(status(-120, sealed, -190).visorFrost() == 0F, "no frost within the rating");
        helper.assertTrue(status(-233, sealed | trap, -190).visorFrost() == 1F, "a Mk1 suit in a lunar cold trap frosts over");
        helper.assertTrue(status(-233, sealed | trap, -250).visorFrost() == 0.3F, "a heated suit in a cold trap shows only a light rime");
        helper.assertTrue(status(-233, trap, -190).visorFrost() == 0F, "an open suit has no sealed visor to frost");
        helper.assertTrue(status(-233, sealed | trap | pressurised, -190).visorFrost() == 0F, "never inside a pressurised module");
        // the cold trap really is beyond a Mk1 suit and within a heated one (Moon: -173 night, cold trap 60 °C below)
        double trapC = MOON_NIGHT_C - com.pizzaparker07.spacebreaker.content.worldgen.ColdTraps.BELOW_NIGHT_C;
        helper.assertTrue(trapC < SuitTier.MK1.minTempC() && trapC > SuitTier.HEATED.minTempC(),
                "a lunar cold trap (" + trapC + " °C) needs more than Mk1 and is within the heated suit");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void venusPressureWearsTheSuitButNeverRuinsIt(GameTestHelper helper) {
        Player p = wearing(helper, GameType.SURVIVAL, set(SuitTier.THERMAL));
        Tech.learn(p, SuitTier.THERMAL.tech());
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        p.tickCount = 0;
        LifeSupport.tickPressureWear(p, 50.0, true);
        helper.assertTrue(chest.getDamageValue() == 0, "ordinary pressure does not wear the suit");
        for (int i = 0; i < 5000; i++) {
            p.tickCount = i * LifeSupport.STRAIN_WEAR_TICKS;
            LifeSupport.tickPressureWear(p, VENUS_KPA, true);
        }
        helper.assertTrue(chest.getDamageValue() > 0, "Venus' pressure wears the suit");
        helper.assertTrue(chest.getDamageValue() <= chest.getMaxDamage() * LifeSupport.STRAIN_WEAR_LIMIT, "but never past the limit");
        helper.assertTrue(SpaceSuitItem.isSealed(p), "so the thermal suit stays sealed: Venus stays playable");
        helper.assertTrue(bodyTarget(p, VENUS_C, VENUS_KPA) == SurvivalData.NORMAL_BODY_C, "and still keeps the body at 37 °C");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void suitWearApiRespectsItsLimit(GameTestHelper helper) {
        Player p = wearing(helper, GameType.SURVIVAL, set(SuitTier.MK1));
        ItemStack boots = p.getItemBySlot(EquipmentSlot.FEET);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SuitWear.wearAll(p, 3, 1.0) == 4, "all four pieces take wear");
        helper.assertTrue(boots.getDamageValue() == 3, "three points of wear, got " + boots.getDamageValue());
        com.pizzaparker07.spacebreaker.content.suit.SuitWear.wearPiece(p, EquipmentSlot.FEET, 100000, 0.5);
        helper.assertTrue(boots.getDamageValue() == (int) Math.floor(boots.getMaxDamage() * 0.5), "capped at half, got " + boots.getDamageValue());
        com.pizzaparker07.spacebreaker.content.suit.SuitWear.wearPiece(p, EquipmentSlot.FEET, 100000, 1.0);
        helper.assertTrue(!boots.isEmpty() && boots.getDamageValue() == boots.getMaxDamage() - 1, "worn out but still worn");
        helper.assertFalse(SpaceSuitItem.isSealed(p), "a worn-out piece leaks");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void titaniumToolsSitBetweenIronAndDiamond(GameTestHelper helper) {
        var ti = com.pizzaparker07.spacebreaker.content.suit.TitaniumTools.TITANIUM;
        var iron = net.minecraft.world.item.Tiers.IRON;
        var diamond = net.minecraft.world.item.Tiers.DIAMOND;
        helper.assertTrue(ti.getUses() > iron.getUses() * 4 && ti.getUses() < diamond.getUses(), "tougher than iron, below diamond");
        helper.assertTrue(ti.getSpeed() > iron.getSpeed() && ti.getSpeed() < diamond.getSpeed(), "digs between iron and diamond");
        ItemStack pick = new ItemStack(com.pizzaparker07.spacebreaker.content.suit.TitaniumTools.all().get(0).get());
        helper.assertTrue(pick.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState()), "mines diamond ore");
        helper.assertFalse(pick.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()), "but not obsidian");
        helper.assertTrue(ti.getRepairIngredient().test(new ItemStack(com.pizzaparker07.spacebreaker.content.earth.EarthModule.TITANIUM.ingot.get())),
                "repaired with titanium ingots");
        var recipe = helper.getLevel().getRecipeManager().byKey(com.pizzaparker07.spacebreaker.Spacebreaker.id("titanium_pickaxe"));
        helper.assertTrue(recipe.isPresent(), "the pickaxe has a recipe");
        helper.succeed();
    }
}
