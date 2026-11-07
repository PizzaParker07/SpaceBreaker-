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

    /** One planet point: direction in the sun frame (+y toward the sun), size, colour and brightness. */
    public record Dot(String id, Vector3f local, float size, float r, float g, float b, float alpha) {
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
            Vector3f d = new Vector3f((float) o.direction().x(), (float) o.direction().y(), (float) o.direction().z()).normalize();
            // local sun frame: +y toward the sun; bodies east of the sun lag behind it (-z), ecliptic north = +x
            Vector3f local = new Vector3f(d.dot(n), d.dot(e1), -d.dot(e2)).normalize();
            double mag = o.apparentMagnitude();
            float size = (float) Mth.clamp(0.22 + 0.075 * (2.6 - mag), 0.2, 0.6);
            float alpha = (float) Mth.clamp(0.55 + 0.12 * (2.6 - mag), 0.45, 1.0);
            int rgb = o.colorRgb();
            out.add(new Dot(o.id(), local, size, (rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, alpha));
        }
        return out;
    }

    private PlanetPoints() {
    }
}
