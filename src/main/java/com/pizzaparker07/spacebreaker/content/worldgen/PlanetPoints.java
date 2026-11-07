package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.SkyEphemeris;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Bright planets as points in another world's (or Earth's) sky, placed in the game sun's frame: each keeps its
 * real angle from the sun along the ecliptic (astro sky ephemeris), so evening stars set after the sun and
 * morning stars rise before it. Pure maths; drawn by client/sky.
 */
public final class PlanetPoints {
    /** Fainter planets than this are left to the star field. */
    public static final double FAINTEST = 2.6;
    /** Discs bigger than this (the Moon from Earth, Earth from the Moon) are drawn by the sky itself. */
    public static final double MAX_POINT_DEG = 0.2;

    /** Gap (sky-sphere units at radius 100) kept between a planet point and the point of one of its moons. */
    public static final float PAIR_GAP = 0.15F;

    /**
     * One planet point: direction in the sun frame (+y toward the sun), size (half-width at radius 100), colour,
     * brightness and twilight boost (1 for most; up to 2 for very bright points, which show earlier at dusk).
     */
    public record Dot(String id, Vector3f local, float size, float r, float g, float b, float alpha, float twilight) {
        /** Brightness to draw with when the sky lets {@code visibility} (0..1) of the stars through. */
        public float shown(float visibility) {
            return alpha * Math.min(1F, visibility * twilight);
        }
    }

    /** Planet points for an observer at time t (pure maths, also used by tests). */
    public static List<Dot> compute(BodySystem sys, String observer, double t) {
        List<Dot> out = new ArrayList<>();
        List<SkyEphemeris.SkyObject> all;
        try {
            all = SkyEphemeris.visible(sys, observer, t, FAINTEST);
        } catch (RuntimeException e) {
            return out;
        }
        Vec3 sun = null;
        for (SkyEphemeris.SkyObject o : all) {
            if (o.star()) {
                sun = o.direction();
                break;
            }
        }
        if (sun == null) {
            return out;
        }
        // sun-relative ecliptic basis: e1 toward the sun, e2 toward increasing longitude, n ecliptic north
        Vector3f e1 = new Vector3f((float) sun.x(), (float) sun.y(), (float) sun.z()).normalize();
        Vector3f n = new Vector3f(0, 0, 1);
        Vector3f e2 = new Vector3f(n).cross(e1);
        if (e2.lengthSquared() < 1e-6) {
            return out;
        }
        e2.normalize();
        n = new Vector3f(e1).cross(e2).normalize();
        for (SkyEphemeris.SkyObject o : all) {
            if (o.star() || o.angularDiameterDeg() > MAX_POINT_DEG) {
                continue;
            }
            // the observer's own moons are drawn as discs by its sky (Phobos and Deimos over Mars)
            if (sys.get(o.id()).map(b -> observer.equals(b.parent())).orElse(false)) {
                continue;
            }
            Vector3f d = new Vector3f((float) o.direction().x(), (float) o.direction().y(), (float) o.direction().z()).normalize();
            // local sun frame: +y toward the sun; bodies east of the sun lag behind it (-z), ecliptic north = +x
            Vector3f local = new Vector3f(d.dot(n), d.dot(e1), -d.dot(e2)).normalize();
            double mag = o.apparentMagnitude();
            float size = (float) Mth.clamp(0.22 + 0.075 * (2.6 - mag), 0.2, 0.6);
            float alpha = (float) Mth.clamp(0.55 + 0.12 * (2.6 - mag), 0.45, 1.0);
            // brighter than magnitude -1 (Venus, Jupiter, Earth from Mars): visible earlier in twilight
            float twilight = (float) (1 + Mth.clamp((-1 - mag) / 3, 0, 1));
            int rgb = o.colorRgb();
            out.add(new Dot(o.id(), local, size, (rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, alpha, twilight));
        }
        separatePairs(sys, out);
        return out;
    }

    /**
     * A moon that would overlap its planet's point (the Moon next to Earth seen from Mars, a fraction of a degree apart)
     * is pushed out along its true direction from the planet until the two points just clear each other; real
     * separations bigger than that are kept.
     */
    public static void separatePairs(BodySystem sys, List<Dot> dots) {
        for (int i = 0; i < dots.size(); i++) {
            Dot moon = dots.get(i);
            String parent = sys.get(moon.id()).map(b -> b.parent()).orElse(null);
            if (parent == null) {
                continue;
            }
            for (Dot planet : dots) {
                if (!planet.id().equals(parent)) {
                    continue;
                }
                double need = (planet.size() + moon.size() + PAIR_GAP + 0.01) / 100.0; // a hair more: float rounding
                double sep = Math.acos(Mth.clamp(planet.local().dot(moon.local()), -1, 1));
                if (sep >= need) {
                    break;
                }
                // tangent at the planet pointing toward the moon (any sideways direction if they coincide)
                Vector3f tangent = new Vector3f(moon.local()).sub(new Vector3f(planet.local()).mul(planet.local().dot(moon.local())));
                if (tangent.lengthSquared() < 1e-12) {
                    tangent = new Vector3f(planet.local()).cross(0, 0, 1);
                    if (tangent.lengthSquared() < 1e-12) {
                        tangent = new Vector3f(planet.local()).cross(1, 0, 0);
                    }
                }
                tangent.normalize();
                Vector3f moved = new Vector3f(planet.local()).mul((float) Math.cos(need)).add(tangent.mul((float) Math.sin(need))).normalize();
                dots.set(i, new Dot(moon.id(), moved, moon.size(), moon.r(), moon.g(), moon.b(), moon.alpha(), moon.twilight()));
                break;
            }
        }
    }

    private PlanetPoints() {
    }
}
