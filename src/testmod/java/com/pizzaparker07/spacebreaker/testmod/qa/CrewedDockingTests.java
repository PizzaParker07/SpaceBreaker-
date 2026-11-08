package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A crewed rocket docks like an uncrewed one: the same approach as rocketry's docking test, with a player in the seat.
 * Measured (written to {@code run-gametest/qa/crewed_docking.md}), not asserted, until rocketry has looked at it.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrewedDockingTests {
    @GameTest(template = "empty", timeoutTicks = 900, batch = StationAuditTests.QA_BATCH)
    public static void crewedRocketDocks(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos port = StationAuditTests.away(h, 3).above(10);
        level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlock.FACING, Direction.UP), 3);
        RocketEntity rocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, port.getCenter().add(0, 1, 0), 0F);
        level.addFreshEntity(rocket);
        rocket.stageScene(FlightState.ORBIT, 0, 0, false);
        var player = h.makeMockServerPlayerInLevel();
        player.moveTo(rocket.getX(), rocket.getY() + 1, rocket.getZ());
        boolean seated = player.startRiding(rocket, true);
        boolean[] started = {false};
        int[] t = {0};
        h.onEachTick(() -> {
            t[0]++;
            if (!started[0] && rocket.tickCount >= 5) {
                started[0] = true;
                rocket.approachPort(port, Direction.UP, 6);
            }
            if (t[0] == 700) {
                var end = RocketEntity.dockPoint(port, Direction.UP, rocket.getBbHeight(), rocket.getBbWidth());
                String line = String.format(java.util.Locale.ROOT,
                        "- crewed approach (player seated %s, still riding %s): docked %s after 700 ticks, %.2f blocks to go, state %s",
                        seated, player.getVehicle() == rocket, rocket.docked(), rocket.position().distanceTo(end), rocket.state());
                ProgressionReport.write("crewed_docking.md", "## Crewed docking\n\n" + line + "\n");
                rocket.discard();
                level.removeBlock(port, false);
                StationAuditTests.clear(level, port);
                h.succeed();
            }
        });
    }

    private CrewedDockingTests() {
    }
}
