package com.pizzaparker07.spacebreaker.testmod.outer;

import com.mojang.authlib.GameProfile;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterArrivals;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** First landings on the outer worlds: one note per world per player, nothing for other dimensions, every note in EN and TR. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OuterArrivalTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void firstLandingNoteComesOncePerWorld(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "arrival_test"));
        helper.assertTrue(OuterArrivals.arrive(player, Spacebreaker.id("triton")), "first landing on Triton gets the note");
        helper.assertFalse(OuterArrivals.arrive(player, Spacebreaker.id("triton")), "the second does not");
        helper.assertTrue(OuterArrivals.visited(player, "triton"), "the visit is remembered");
        helper.assertTrue(OuterArrivals.arrive(player, Spacebreaker.id("bennu")), "each world has its own note");
        helper.assertFalse(OuterArrivals.arrive(player, Spacebreaker.id("moon")), "inner worlds have their own welcome elsewhere");
        helper.assertFalse(OuterArrivals.arrive(player, ResourceLocation.withDefaultNamespace("the_nether")), "not other mods' or vanilla worlds");
        for (String w : new String[]{"ceres", "vesta", "io", "europa", "ganymede", "callisto", "titan", "enceladus", "miranda", "ariel", "titania",
                "umbriel", "oberon", "triton", "proteus", "nereid", "pluto", "charon", "nix", "hydra", "bennu", "ryugu", "eros", "itokawa"}) {
            WorldTests.stem(helper, w);
            helper.assertTrue(net.minecraft.locale.Language.getInstance().has("arrival.spacebreaker." + w), w + " has an arrival note");
        }
        helper.succeed();
    }

    /** The arrival notes and the research tree's system achievements name the same outer worlds (inner planets aside). */
    @GameTest(template = WorldTests.EMPTY)
    public static void arrivalNotesMatchTheSystemAchievements(GameTestHelper helper) {
        java.util.Set<String> achievements = new java.util.TreeSet<>();
        for (String[] system : com.pizzaparker07.spacebreaker.content.research.data.ResearchAdvancements.SYSTEMS) {
            if (!system[0].equals("inner_planets")) {
                achievements.addAll(java.util.Arrays.asList(system).subList(2, system.length));
            }
        }
        java.util.Set<String> notes = new java.util.TreeSet<>(OuterArrivals.worlds());
        helper.assertTrue(notes.equals(achievements), "outer worlds differ: notes " + notes + " vs achievements " + achievements);
        helper.succeed();
    }
}
