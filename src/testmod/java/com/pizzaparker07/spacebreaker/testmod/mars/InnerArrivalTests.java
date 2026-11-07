package com.pizzaparker07.spacebreaker.testmod.mars;

import com.mojang.authlib.GameProfile;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterArrivals;
import com.pizzaparker07.spacebreaker.content.mars.InnerArrivals;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** First landings on the inner worlds: one note per world per player, translated, and never doubled by the outer notes. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InnerArrivalTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void innerLandingNoteComesOncePerWorld(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "inner_arrival_test"));
        helper.assertTrue(InnerArrivals.arrive(player, Spacebreaker.id("venus")), "first landing on Venus gets the note");
        helper.assertFalse(InnerArrivals.arrive(player, Spacebreaker.id("venus")), "the second does not");
        helper.assertTrue(InnerArrivals.arrive(player, Spacebreaker.id("mars")), "each world has its own note");
        helper.assertFalse(InnerArrivals.arrive(player, Spacebreaker.id("titan")), "outer worlds have their own notes");
        helper.assertFalse(InnerArrivals.arrive(player, ResourceLocation.withDefaultNamespace("the_nether")), "not vanilla worlds");
        for (String w : new String[]{"mars", "phobos", "deimos", "venus", "mercury"}) {
            helper.assertTrue(InnerArrivals.worlds().contains(w), w + " has an inner note");
            helper.assertFalse(OuterArrivals.worlds().contains(w), w + " is not also an outer note");
            helper.assertTrue(net.minecraft.locale.Language.getInstance().has("arrival.spacebreaker." + w), w + " note is translated");
            helper.assertTrue(net.minecraft.locale.Language.getInstance().has("dimension.spacebreaker." + w), w + " has a name");
        }
        helper.succeed();
    }
}
