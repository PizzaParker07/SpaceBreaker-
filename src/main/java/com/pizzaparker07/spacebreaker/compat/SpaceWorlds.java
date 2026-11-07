package com.pizzaparker07.spacebreaker.compat;

import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Classifies dimensions for renderer compatibility. Every world that belongs to a celestial body (surface or
 * orbit) is classified from the body data, so future worlds are handled without code changes:
 * <ul>
 *   <li>{@link Kind#ORBIT} — orbital space around a body: black sky, the body below</li>
 *   <li>{@link Kind#AIRLESS} — surface without an atmosphere (Moon, Phobos, Deimos, Mercury, ...)</li>
 *   <li>{@link Kind#THIN_ATMOSPHERE} — a thin atmosphere with its own sky colours (Mars)</li>
 *   <li>{@link Kind#THICK_ATMOSPHERE} — dense atmospheres (Venus, Titan)</li>
 *   <li>{@link Kind#VANILLA} — vanilla or unknown dimensions (the Overworld is Earth's surface): left alone</li>
 * </ul>
 */
public final class SpaceWorlds {
    /** Surface pressure (kPa) below which an atmosphere counts as thin. Mars: 0.6, Titan: 147, Earth: 101. */
    public static final double THIN_LIMIT_KPA = 20.0;

    public enum Kind {
        VANILLA, ORBIT, AIRLESS, THIN_ATMOSPHERE, THICK_ATMOSPHERE;

        /** Whether this world draws its own sky (and the shader-pack sky must be replaced by ours). */
        public boolean ownSky() {
            return this != VANILLA;
        }

        /**
         * Whether vanilla weather (rain, thunder, clouds) exists here. Space worlds share the Overworld's weather
         * data in vanilla, which shader packs would otherwise show as rain and wet ground on the Moon.
         */
        public boolean vanillaWeather() {
            return this == VANILLA;
        }

        /** Whether the world has no air at all (sky black by day). */
        public boolean airless() {
            return this == ORBIT || this == AIRLESS;
        }
    }

    private SpaceWorlds() {
    }

    public static Kind classify(RegistryAccess access, ResourceKey<Level> dimension) {
        if (Level.OVERWORLD.equals(dimension) || Level.NETHER.equals(dimension) || Level.END.equals(dimension)) {
            return Kind.VANILLA;
        }
        if (com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.Orbits.SHARED.equals(dimension)) {
            return Kind.ORBIT; // the orbit space shared by every body without an orbit world of its own (Mars, ...)
        }
        Optional<CelestialBody> body = Bodies.forDimension(access, dimension);
        return body.map(b -> classify(b, dimension)).orElse(Kind.VANILLA);
    }

    public static Kind classify(CelestialBody body, ResourceKey<Level> dimension) {
        if (body.orbitDimension().filter(dimension::equals).isPresent()) {
            return Kind.ORBIT;
        }
        if (body.dimension().filter(dimension::equals).isEmpty()) {
            return Kind.VANILLA;
        }
        CelestialBody.Atmosphere air = body.atmosphere();
        if (air.isVacuum()) {
            return Kind.AIRLESS;
        }
        if (air.breathable()) {
            return Kind.VANILLA; // an Earth-like world keeps the shader pack's normal sky
        }
        return air.pressureKpa() < THIN_LIMIT_KPA ? Kind.THIN_ATMOSPHERE : Kind.THICK_ATMOSPHERE;
    }

    public static Kind classify(Level level) {
        return classify(level.registryAccess(), level.dimension());
    }
}
