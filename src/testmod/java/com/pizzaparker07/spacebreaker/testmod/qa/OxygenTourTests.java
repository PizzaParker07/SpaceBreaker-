package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.Navigation;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.OxygenUse;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Oxygen budget of typical Moon excursions, tick by tick with the suit's own breathing rate ({@link OxygenUse}) and
 * way-home warning ({@link Navigation#status}): walk or sprint out, work at the site until the TURN BACK warning, walk
 * home. Writes {@code run-gametest/qa/oxygen_tours.md}; fails if heeding the warning ever leaves the player without air
 * on the way home.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OxygenTourTests {
    /** Vanilla player speeds on flat ground, blocks per second. */
    static final double WALK = 4.317, SPRINT = 5.612;
    /** Moon surface gravity relative to Earth. */
    static final double MOON_G = 0.165;

    record Tour(String name, double distance, boolean sprintOut, int tanks, int tankTicks, double suit) {
    }

    record Result(double outSec, double workSec, double homeSec, double leftSec, boolean warned, boolean ranOut) {
    }

    /** One excursion: out to {@code distance}, work until TURN BACK, then walk home. */
    static Result fly(Tour t) {
        double oxygen = (double) t.tanks() * t.tankTicks();
        double avg = OxygenUse.WALK * t.suit();
        double pos = 0;
        int out = 0, work = 0, home = 0;
        boolean ranOut = false;
        // out
        double speed = (t.sprintOut() ? SPRINT : WALK) / 20.0;
        while (pos < t.distance() && oxygen > 0) {
            double rate = OxygenUse.activity(t.sprintOut(), false, speed) * t.suit();
            avg = OxygenUse.follow(avg, rate);
            oxygen -= rate;
            pos += speed;
            out++;
        }
        // work at the site until the warning
        boolean warned = false;
        while (oxygen > 0) {
            Navigation.Status s = Navigation.status(pos, MOON_G, OxygenUse.ticksLeft((int) oxygen, avg));
            if (s != Navigation.Status.OK) {
                warned = true;
                break;
            }
            double rate = OxygenUse.activity(false, true, 0) * t.suit();
            avg = OxygenUse.follow(avg, rate);
            oxygen -= rate;
            work++;
            if (work > 20 * 60 * 120) {
                break;
            }
        }
        // home, walking
        double back = WALK / 20.0;
        while (pos > 0) {
            if (oxygen <= 0) {
                ranOut = true;
                break;
            }
            double rate = OxygenUse.activity(false, false, back) * t.suit();
            oxygen -= rate;
            pos -= back;
            home++;
        }
        return new Result(out / 20.0, work / 20.0, home / 20.0, Math.max(0, oxygen) / (OxygenUse.WALK * t.suit()) / 20.0, warned, ranOut);
    }

    @GameTest(template = "empty")
    public static void moonExcursionsLeaveAirForTheWayHome(GameTestHelper h) {
        int small = OxygenTankItem.SMALL_CAPACITY, large = OxygenTankItem.LARGE_CAPACITY;
        List<Tour> tours = new ArrayList<>();
        for (double d : new double[]{50, 150, 300, 600}) {
            for (boolean sprint : new boolean[]{false, true}) {
                tours.add(new Tour("2 small", d, sprint, 2, small, 1.0));
                tours.add(new Tour("2 large", d, sprint, 2, large, 1.0));
                tours.add(new Tour("3 large", d, sprint, 3, large, 1.0));
            }
        }
        tours.add(new Tour("2 large, heavy suit", 300, true, 2, large, OxygenUse.HEAVY_SUIT));
        StringBuilder md = new StringBuilder("## Oxygen budget of Moon excursions (`qa/OxygenTourTests`)\n\n");
        md.append("Out to the site (walking 4.3 or sprinting 5.6 blocks/s), working there (×").append(OxygenUse.WORK)
                .append(") until TURN BACK shows, then walking home. Times in minutes:seconds.\n\n");
        md.append("| tanks | distance | out | way out | work until TURN BACK | walk home | air left at home |\n|---|---|---|---|---|---|---|\n");
        List<String> bad = new ArrayList<>();
        for (Tour t : tours) {
            Result r = fly(t);
            md.append(String.format(Locale.ROOT, "| %s | %.0f | %s | %s | %s | %s | %s |%n", t.name(), t.distance(),
                    t.sprintOut() ? "sprint" : "walk", mmss(r.outSec()), r.warned() ? mmss(r.workSec()) : "never (no warning)",
                    mmss(r.homeSec()), r.ranOut() ? "**ran out**" : mmss(r.leftSec())));
            if (r.ranOut()) {
                bad.add(t.name() + " " + t.distance() + (t.sprintOut() ? " sprint" : " walk"));
            }
        }
        ProgressionReport.write("oxygen_tours.md", md.toString());
        h.assertTrue(bad.isEmpty(), "turning back at the warning still runs out of air on: " + bad);
        h.succeed();
    }

    static String mmss(double sec) {
        int s = (int) Math.round(sec);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private OxygenTourTests() {
    }
}
