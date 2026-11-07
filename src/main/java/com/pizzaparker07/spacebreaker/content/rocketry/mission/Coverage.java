package com.pizzaparker07.spacebreaker.content.rocketry.mission;

import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * What the client knows about the satellites (sent by the server): which planet systems have a relay (mission
 * control can be heard there) and which worlds have been mapped.
 */
public final class Coverage {
    private static volatile Set<ResourceLocation> relays = Set.of(Location.EARTH);
    private static volatile Set<ResourceLocation> observed = Set.of();

    private Coverage() {
    }

    public static void set(Set<ResourceLocation> relaySystems, Set<ResourceLocation> observedWorlds) {
        relays = Set.copyOf(relaySystems);
        observed = Set.copyOf(observedWorlds);
    }

    /** Whether radio contact with home works around this body (its planet system has a relay). */
    public static boolean hasSignal(ResourceLocation body) {
        return covered(relays, body);
    }

    public static boolean covered(Set<ResourceLocation> relaySystems, ResourceLocation body) {
        return relaySystems.contains(Locations.parentPlanet(body).orElse(body));
    }

    public static boolean observed(ResourceLocation body) {
        return observed.contains(body);
    }
}
