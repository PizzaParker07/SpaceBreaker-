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
                0, 100, flags, minC, 130, 30, 1F);
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

    /** Radiation suits and the shield module take any radiation shielding plate: tungsten or depleted uranium. */
    @GameTest(template = "empty")
    public static void radiationSuitsTakeEitherShieldingPlate(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        ItemStack uranium = new ItemStack(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.DEPLETED_URANIUM_PLATE.get());
        var tungsten = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.parse("c:plates/tungsten")));
        helper.assertTrue(tungsten.isPresent() && tungsten.get().size() > 0, "there is a tungsten plate");
        ItemStack tungstenPlate = new ItemStack(tungsten.get().get(0));
        for (String id : new String[]{"radiation_helmet", "radiation_chestplate", "radiation_leggings", "radiation_boots", "radiation_shield_module"}) {
            var holder = manager.byKey(com.pizzaparker07.spacebreaker.Spacebreaker.id(id));
            helper.assertTrue(holder.isPresent(), id + " has a recipe");
            boolean takesUranium = false, takesTungsten = false;
            for (var ing : holder.get().value().getIngredients()) {
                takesUranium |= ing.test(uranium);
                takesTungsten |= ing.test(tungstenPlate);
            }
            helper.assertTrue(takesUranium, id + " takes a depleted uranium plate");
            helper.assertTrue(takesTungsten, id + " still takes a tungsten plate");
        }
        helper.succeed();
    }

    /** Oxygen use follows the activity: sprinting and work cost more, rest less, heavy suits a little more. */
    @GameTest(template = "empty")
    public static void oxygenUseFollowsActivity(GameTestHelper helper) {
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.activity(true, true, 0.3) == 1.5, "sprinting x1.5");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.activity(false, true, 0.0) == 1.25, "mining or hitting x1.25");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.activity(false, false, 0.2) == 1.0, "walking x1");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.activity(false, false, 0.0) == 0.8, "standing still x0.8");
        // a sprinting wearer in a thermal suit: 1.5 x 1.1 tank ticks per tick
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(SuitModule.THERMAL_CHESTPLATE.get()));
        helper.assertTrue(Math.abs(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.suitFactor(player) - 1.1) < 1e-9, "heavy suits x1.1");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(SuitModule.EVA_CHESTPLATE.get()));
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.suitFactor(player) == 1.0, "the Mk1 suit x1");
        // the tanks drain by the rate, fractions carried over
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        com.pizzaparker07.spacebreaker.content.suit.SuitTanks.insert(torso,
                com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        int full = com.pizzaparker07.spacebreaker.content.suit.SuitTanks.oxygen(torso);
        var data = new com.pizzaparker07.spacebreaker.content.suit.SurvivalData();
        for (int i = 0; i < 100; i++) {
            com.pizzaparker07.spacebreaker.content.suit.OxygenUse.breathe(torso, data, 1.5);
        }
        helper.assertTrue(full - com.pizzaparker07.spacebreaker.content.suit.SuitTanks.oxygen(torso) == 150, "100 ticks of sprinting use 150");
        for (int i = 0; i < 100; i++) {
            com.pizzaparker07.spacebreaker.content.suit.OxygenUse.breathe(torso, data, 0.8);
        }
        helper.assertTrue(full - com.pizzaparker07.spacebreaker.content.suit.SuitTanks.oxygen(torso) == 230, "100 ticks of rest use 80");
        // the HUD time follows a running average: it moves smoothly, about 63 % of the way in 5 s
        double avg = 1.0;
        for (int i = 0; i < com.pizzaparker07.spacebreaker.content.suit.OxygenUse.AVERAGE_TICKS; i++) {
            avg = com.pizzaparker07.spacebreaker.content.suit.OxygenUse.follow(avg, 1.5);
        }
        helper.assertTrue(avg > 1.30 && avg < 1.33, "after 5 s of sprinting the average is ~1.32, got " + avg);
        // 10 min of oxygen lasts 12.5 min at rest and 6:40 sprinting; the way-home check uses the same time
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.ticksLeft(12000, 0.8) == 15000, "12:30 at rest");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.OxygenUse.ticksLeft(12000, 1.5) == 8000, "6:40 sprinting");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.Navigation.status(300, 0.26,
                com.pizzaparker07.spacebreaker.content.suit.OxygenUse.ticksLeft(3600, 1.0)) == com.pizzaparker07.spacebreaker.content.suit.Navigation.Status.OK
                && com.pizzaparker07.spacebreaker.content.suit.Navigation.status(300, 0.26,
                com.pizzaparker07.spacebreaker.content.suit.OxygenUse.ticksLeft(3600, 1.5)) == com.pizzaparker07.spacebreaker.content.suit.Navigation.Status.TURN_BACK,
                "the same oxygen is enough walking but means turn back after a sprint");
        helper.succeed();
    }

    /** Realistic sound: the air around decides how loud and how dull the world sounds. */
    @GameTest(template = "empty")
    public static void soundCarriesByTheAir(GameTestHelper helper) {
        var earth = com.pizzaparker07.spacebreaker.content.suit.SoundMuffling.outside(101.325, true);
        helper.assertTrue(earth.none(), "Earth: normal");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SoundMuffling.outside(147, false).none(), "Titan's thick air: normal");
        var mars = com.pizzaparker07.spacebreaker.content.suit.SoundMuffling.outside(0.6, false);
        helper.assertTrue(mars.gain() >= 0.8F && mars.highGain() >= 0.6F, "Mars: only a touch quieter and softer, got " + mars);
        var moon = com.pizzaparker07.spacebreaker.content.suit.SoundMuffling.outside(0.0, false);
        helper.assertTrue(moon.gain() >= 0.5F && moon.highGain() >= 0.4F && !moon.none(), "vacuum: quieter, never silent, got " + moon);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SoundMuffling.outside(0.0, true).none(), "a pressurised room: normal");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SoundMuffling.contact(0.0, false).none(), "own steps and tools: normal");
        helper.assertFalse(com.pizzaparker07.spacebreaker.client.hud.HudConfig.REALISTIC_SOUND.getDefault(), "the option is off by default");
        helper.succeed();
    }

    /** The cockpit HUD's number formats and Δv share. */
    @GameTest(template = "empty")
    public static void cockpitReadoutFormats(GameTestHelper helper) {
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.speed(-4.62).equals("-4.6 m/s"), "slow speeds to 0.1 m/s");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.speed(845).equals("845 m/s"), "hundreds of m/s");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.speed(-1240).equals("-1.24 km/s"), "km/s from 1000");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.speed(11020).equals("11.0 km/s"), "escape speeds");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.distance(38).equals("38 m"), "metres");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.distance(12_400).equals("12.4 km"), "km with a decimal");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.distance(3.844e8).equals("384,400 km"), "the Moon's distance");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.distance(Double.POSITIVE_INFINITY).equals("∞"), "escape apoapsis");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.FlightFormat.time(45).equals("0:45")
                && com.pizzaparker07.spacebreaker.client.hud.FlightFormat.time(11_112).equals("3:05:12"), "times");
        var r = new com.pizzaparker07.spacebreaker.client.hud.CockpitReadout(net.minecraft.network.chat.Component.literal("Moon"), -1240, 3.3e8, false,
                975, 3900, false, Double.NaN, Double.NaN, null, Double.NaN, Double.NaN, Double.NaN);
        helper.assertTrue(Math.abs(r.dvFraction() - 0.25F) < 1e-6 && !r.hasNode(), "Δv share and no node");
        // what a craft syncs, turned into the HUD's readout
        var sync = new com.pizzaparker07.spacebreaker.core.astro.flight.FlightReadout("spacebreaker:moon", -1240, 3.3e8, false,
                2650, 3900, true, java.util.List.of(), 815, 60);
        var hud = com.pizzaparker07.spacebreaker.client.hud.CockpitReadout.of(sync);
        helper.assertTrue(hud.target().getString().equals("Moon"), "the target body by its name, got " + hud.target().getString());
        helper.assertTrue(hud.assist() && hud.hasNode() && hud.nodeDv() == 815 && hud.nodeIn() == 60, "assist and the node carried over");
        helper.assertTrue(Double.isNaN(hud.periapsis()) && hud.event() == null, "no path: no orbit or event shown");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.hud.CockpitReadout.name("ship-7").getString().equals("ship-7"), "other names as written");
        helper.succeed();
    }

    /** The cockpit target marker's projection: ahead lands on screen, behind gives a direction for the edge arrow. */
    @GameTest(template = "empty")
    public static void cockpitTargetProjection(GameTestHelper helper) {
        var view = new org.joml.Matrix4f();
        var proj = new org.joml.Matrix4f().perspective((float) Math.toRadians(70), 16F / 9F, 0.05F, 1000F);
        var cam = net.minecraft.world.phys.Vec3.ZERO;
        double[] ahead = com.pizzaparker07.spacebreaker.client.hud.CockpitHud.project(view, proj, cam, new net.minecraft.world.phys.Vec3(0, 0, -100), 10, 640, 360);
        helper.assertTrue(ahead[2] == 1 && Math.abs(ahead[0] - 320) < 1 && Math.abs(ahead[1] - 180) < 1, "straight ahead: the screen centre");
        helper.assertTrue(ahead[3] > 10 && ahead[3] < 40, "a 10-block body 100 blocks away is a few dozen pixels, got " + ahead[3]);
        double[] right = com.pizzaparker07.spacebreaker.client.hud.CockpitHud.project(view, proj, cam, new net.minecraft.world.phys.Vec3(30, 0, -100), 1, 640, 360);
        helper.assertTrue(right[2] == 1 && right[0] > 320, "to the right: right of centre");
        double[] behind = com.pizzaparker07.spacebreaker.client.hud.CockpitHud.project(view, proj, cam, new net.minecraft.world.phys.Vec3(-5, 0, 100), 1, 640, 360);
        helper.assertTrue(behind[2] == 0 && behind[0] < 320, "behind and to the left: off screen, pointing left, got " + behind[0]);
        helper.succeed();
    }
}
