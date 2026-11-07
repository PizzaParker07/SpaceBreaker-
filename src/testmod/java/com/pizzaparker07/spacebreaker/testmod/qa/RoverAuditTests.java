package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rover bug sweep (1.0.0 play feedback): seats against the roof solar panel, getting in and out, handing the wheel
 * over, steps and slopes. Measures with the real riding code and writes {@code run-gametest/qa/rover.md}; nothing here
 * fails the build yet — the findings go to industry first.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RoverAuditTests {
    /**
     * The solar panel in RoverModel since industry 44a83bab: on the rear deck, model y 5..6 (y down, ground at 24,
     * renderer lifts 1.5), model z 15..31 (behind the centre).
     */
    static final double PANEL_BOTTOM = 1.5 - 6 / 16.0, PANEL_Z_FROM = 15 / 16.0, PANEL_Z_TO = 31 / 16.0;
    /** Half the depth of a seated player's head. */
    static final double HEAD_HALF = 0.25;
    /** Top of a riding player's head above its position (vanilla player model). */
    static final double HEAD_TOP = 1.8;

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void roverSweep(GameTestHelper h) {
        var level = h.getLevel();
        List<String> out = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                level.setBlock(h.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        RoverEntity rover = VehiclesModule.ROVER.get().create(level);
        Vec3 at = h.absoluteVec(new Vec3(4, 1, 4));
        rover.moveTo(at.x, at.y, at.z, 0F, 0F);
        level.addFreshEntity(rover);
        rover.installSolar();
        ServerPlayer driver = h.makeMockServerPlayerInLevel();
        ServerPlayer second = h.makeMockServerPlayerInLevel();
        driver.moveTo(at.x + 2, at.y, at.z);
        second.moveTo(at.x - 2, at.y, at.z);
        boolean d = driver.startRiding(rover);
        boolean s = second.startRiding(rover);
        rover.positionRider(driver);
        rover.positionRider(second);
        out.add("- two seats: driver " + d + ", passenger " + s + "; a third rider refused: "
                + !h.makeMockServerPlayerInLevel().startRiding(rover));
        // heads against the roof panel (rover facing +z: "behind" is -z)
        for (ServerPlayer p : List.of(driver, second)) {
            double head = p.getY() + HEAD_TOP - rover.getY();
            double back = -(p.getZ() - rover.getZ());
            boolean under = back + HEAD_HALF >= PANEL_Z_FROM && back - HEAD_HALF <= PANEL_Z_TO;
            String who = p == driver ? "driver" : "passenger";
            out.add(String.format(Locale.ROOT, "- %s: seat %.2f above the rover, %.2f behind its centre; head top at %.2f, panel underside at %.2f%s",
                    who, p.getY() - rover.getY(), back, head, PANEL_BOTTOM,
                    under && head > PANEL_BOTTOM ? " → **head inside the solar panel** (by " + String.format(Locale.ROOT, "%.2f", head - PANEL_BOTTOM) + ")" : " (clear)"));
        }
        // driver leaves: does the passenger take the wheel and move forward?
        Vec3 dismount = rover.getDismountLocationForPassenger(driver);
        driver.stopRiding();
        rover.positionRider(second);
        out.add(String.format(Locale.ROOT, "- driver gets out at %.2f above the rover's base (%s), %.2f to the side",
                dismount.y - rover.getY(), dismount.y - rover.getY() > 1.0 ? "**on the roof**" : "on the ground",
                Math.hypot(dismount.x - rover.getX(), dismount.z - rover.getZ())));
        Entity controller = rover.getControllingPassenger();
        out.add("- after the driver leaves, the passenger " + (controller == second ? "takes the wheel" : "does not drive")
                + String.format(Locale.ROOT, " and sits %.2f behind the centre", -(second.getZ() - rover.getZ())));
        second.stopRiding();
        out.add(String.format(Locale.ROOT, "- step height %.1f block; bounding box %.1f × %.1f", rover.maxUpStep(), rover.getBbWidth(), rover.getBbHeight()));
        ProgressionReport.write("rover.md", "## Rover sweep (`qa/RoverAuditTests`)\n\n" + String.join("\n", out) + "\n");
        rover.discard();
        h.succeed();
    }

    private RoverAuditTests() {
    }
}
