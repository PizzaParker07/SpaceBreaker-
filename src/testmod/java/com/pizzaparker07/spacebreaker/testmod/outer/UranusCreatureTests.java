package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.entity.OuterGrazer;
import com.pizzaparker07.spacebreaker.content.uranus.Oberon;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import com.pizzaparker07.spacebreaker.content.uranus.entity.GloamHopper;
import com.pizzaparker07.spacebreaker.content.uranus.entity.RimBurrower;
import com.pizzaparker07.spacebreaker.content.uranus.entity.ScreeRoller;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Behaviour over time of the Uranian moons' creatures: each defensive trick wears off and can be used again, the climber
 * really climbs, and the grazers built on the shared base really breed.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UranusCreatureTests {
    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static void floor(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), block);
            }
        }
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void screeRollerUnrollsAfterAWhile(GameTestHelper helper) {
        floor(helper, UranusModule.TITANIA_REGOLITH.get());
        ScreeRoller r = helper.spawn(UranusEntities.SCREE_ROLLER.get(), new BlockPos(3, 2, 3));
        r.setSpeed(0.2F);
        float idle = r.getSpeed();
        r.hurt(helper.getLevel().damageSources().playerAttack(player(helper)), 1F);
        r.setSpeed(0.2F);
        helper.assertTrue(r.isRolled() && r.getSpeed() > idle * 2, "rolled and fast: " + r.getSpeed() + " vs " + idle);
        long hit = helper.getTick();
        helper.succeedWhen(() -> {
            helper.assertFalse(r.isRolled(), "still rolled");
            helper.assertTrue(helper.getTick() - hit >= ScreeRoller.ROLL_TIME - 2, "unrolled too early");
        });
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void gloamHopperCloudComesBack(GameTestHelper helper) {
        floor(helper, com.pizzaparker07.spacebreaker.content.uranus.Umbriel.REGOLITH.get());
        GloamHopper h = helper.spawn(UranusEntities.GLOAM_HOPPER.get(), new BlockPos(3, 2, 3));
        h.hurt(helper.getLevel().damageSources().playerAttack(player(helper)), 1F);
        helper.assertFalse(h.cloudReady(), "the cloud is spent");
        helper.succeedWhen(() -> helper.assertTrue(h.cloudReady(), "the cloud is ready again after its cooldown"));
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 240)
    public static void rimBurrowerPopsBackOut(GameTestHelper helper) {
        floor(helper, Oberon.REGOLITH.get());
        RimBurrower b = helper.spawn(UranusEntities.RIM_BURROWER.get(), new BlockPos(3, 2, 3));
        helper.runAfterDelay(10, () -> {
            b.hurt(helper.getLevel().damageSources().playerAttack(player(helper)), 1F);
            helper.assertTrue(b.isBuried(), "dug in");
            long hit = helper.getTick();
            helper.succeedWhen(() -> {
                helper.assertFalse(b.isBuried(), "still buried");
                helper.assertTrue(helper.getTick() - hit >= RimBurrower.BURY_TIME - 2, "out too early");
                float before = b.getHealth();
                b.invulnerableTime = 0;
                b.hurt(helper.getLevel().damageSources().generic(), 1F);
                helper.assertTrue(b.getHealth() < before, "out again, it can be hurt");
            });
        });
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void cliffClingerClimbsAWall(GameTestHelper helper) {
        floor(helper, UranusModule.MIRANDAN_REGOLITH.get());
        for (int y = 2; y < 8; y++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(5, y, z), UranusModule.MIRANDAN_ICE.base().get());
            }
        }
        var c = helper.spawn(UranusEntities.CLIFF_CLINGER.get(), new BlockPos(4, 2, 3));
        BlockPos top = helper.absolutePos(new BlockPos(5, 8, 3));
        helper.succeedWhen(() -> {
            if (!c.getNavigation().isInProgress()) {
                c.getNavigation().moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 1.2);
            }
            helper.assertTrue(c.getY() > helper.absolutePos(BlockPos.ZERO).getY() + 5, "it is up the wall: y " + c.getY());
        });
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 400)
    public static void outerGrazersBreed(GameTestHelper helper) {
        floor(helper, Oberon.REGOLITH.get());
        Player p = player(helper);
        for (EntityType<? extends OuterGrazer> type : java.util.List.of(UranusEntities.SCREE_ROLLER.get(), UranusEntities.GLOAM_HOPPER.get(),
                UranusEntities.RIM_BURROWER.get())) {
            helper.assertTrue(type.getCategory() == MobCategory.CREATURE, type + " is a creature");
            OuterGrazer a = helper.spawn(type, new BlockPos(2, 2, 2));
            OuterGrazer b = helper.spawn(type, new BlockPos(3, 2, 2));
            a.setInLove(p);
            b.setInLove(p);
        }
        helper.succeedWhen(() -> {
            for (EntityType<?> type : java.util.List.of(UranusEntities.SCREE_ROLLER.get(), UranusEntities.GLOAM_HOPPER.get(),
                    UranusEntities.RIM_BURROWER.get())) {
                long babies = helper.getEntities(type).stream().filter(e -> e instanceof OuterGrazer g && g.isBaby()).count();
                helper.assertTrue(babies >= 1, type + " has a baby");
            }
        });
    }

    /**
     * The frost moth takes to the wing. Hardened against a rare flake: its chunks are force-loaded, it is set flying (not
     * hanging at rest), success is "it has flown at least a block from where it started" whenever that happens, and a
     * failure says where it is and how fast it is moving.
     */
    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 400, batch = "frost_moth")
    public static void frostMothFlies(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        java.util.List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var c = new net.minecraft.world.level.ChunkPos((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz);
                level.setChunkForced(c.x, c.z, true);
                forced.add(c);
            }
        }
        var m = helper.spawn(UranusEntities.FROST_MOTH.get(), new BlockPos(3, 4, 3));
        helper.assertTrue(m.getType().getCategory() == MobCategory.AMBIENT, "an ambient creature");
        helper.assertTrue(m instanceof net.minecraft.world.entity.ambient.Bat, "it flies like a bat");
        ((net.minecraft.world.entity.ambient.Bat) m).setResting(false);
        var start = m.position();
        double[] farthest = {0};
        helper.onEachTick(() -> farthest[0] = Math.max(farthest[0], m.position().distanceTo(start)));
        helper.succeedWhen(() -> {
            String state = String.format(" (at %.2f %.2f %.2f, moved %.2f, speed %.3f, resting %s, alive %s)", m.getX(), m.getY(), m.getZ(),
                    farthest[0], m.getDeltaMovement().length(), ((net.minecraft.world.entity.ambient.Bat) m).isResting(), m.isAlive());
            helper.assertTrue(m.isAlive(), "the moth is alive" + state);
            helper.assertTrue(farthest[0] > 1.0, "the moth is on the wing" + state);
            forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
        });
    }
}
