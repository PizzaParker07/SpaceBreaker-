package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.client.audio.MissionControlVoice;
import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.MissionCalls;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Mission control calls for the crew of a rocket, from the rocket's synced state (see {@link MissionCalls}).
 * Only the player riding the rocket hears them. The fuel warning is given once per flight.
 */
final class MissionVoice {
    @Nullable
    private static MissionCalls.Snapshot last;
    private static boolean warnedFuel;
    private static boolean canReturn = true;
    private static int check;

    private MissionVoice() {
    }

    static void tick(@Nullable RocketEntity rocket) {
        if (rocket == null) {
            // keep the last snapshot for a few ticks: the rocket is re-created after a dimension change
            if (last != null && ++check > 200) {
                last = null;
                warnedFuel = false;
                canReturn = true;
            }
            return;
        }
        if (++check % 20 == 0) {
            Location here = rocket.location();
            canReturn = here.equals(Location.EARTH_SURFACE) || rocket.isCapsuleOnly()
                    || !here.orbit() && com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.canRefuel(here.body())
                    || MissionPlanner.canReturnHome(rocket.layout(), rocket.activeStage(), rocket.fuel(), here, SbConfig.realism());
        }
        MissionCalls.Snapshot now = new MissionCalls.Snapshot(rocket.state(), rocket.timer(), rocket.activeStage(), canReturn);
        SbSounds.Line line = MissionCalls.call(last, now);
        if (line == SbSounds.Line.FUEL_LOW) {
            if (warnedFuel) {
                line = null;
            }
            warnedFuel = true;
        }
        // beyond the reach of the relays mission control cannot be heard (the HUD says so)
        boolean signal = com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.hasSignal(rocket.location().body());
        if (line != null && SbConfig.CLIENT_SPEC.isLoaded() && signal) {
            MissionControlVoice.say(line);
        }
        last = now;
        check = check % 20;
    }
}
