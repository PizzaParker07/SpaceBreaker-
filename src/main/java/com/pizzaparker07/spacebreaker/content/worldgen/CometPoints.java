package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Active comets in another world's (or Earth's) sky, in the same sun frame as {@link PlanetPoints}: a glowing head
 * where the comet is, a straight blue ion tail pointing exactly away from the sun and a broad, curved, yellowish
 * dust tail that lags behind along the orbit. Only comets the
 * astro activity model calls ACTIVE or GREAT are shown; GREAT ones are bright. Pure maths; drawn by client/sky.
 */
public final class CometPoints {
    /** A comet in the sky: head and tail tip directions in the sun frame, head radius (sky units at 100 m), brightness. */
    /**
     * @param tip     end of the straight blue ion (gas) tail, exactly away from the sun
     * @param dustMid control point of the curved dust tail (it lags behind along the orbit)
     * @param dustTip end of the dust tail
     */
    public record Sighting(String id, String nameEn, String nameTr, Vector3f head, Vector3f tip, Vector3f dustMid, Vector3f dustTip,
                           float headSize, float alpha, CometActivity.Level level) {
        public boolean great() {
            return level == CometActivity.Level.GREAT;
        }
    }

    private CometPoints() {
    }

    /** The comets visible from an observer body at time t. */
    public static List<Sighting> compute(BodySystem sys, String observer, double t) {
        List<Sighting> out = new ArrayList<>();
        Vec3 obs;
        try {
            obs = sys.position(sys.require(observer), t);
        } catch (RuntimeException e) {
            return out;
        }
        Vec3 toSun = obs.scale(-1);
        if (toSun.length() < 1) {
            return out;
        }
        Basis b = new Basis(toSun);
        for (Comet c : Comet.CATALOGUE) {
            CometActivity.Activity a = CometActivity.at(c, t);
            if (a.level() != CometActivity.Level.ACTIVE && a.level() != CometActivity.Level.GREAT) {
                continue;
            }
            Vec3 pos = c.position(t);
            Vec3 rel = pos.sub(obs);
            double d = rel.length();
            boolean great = a.level() == CometActivity.Level.GREAT;
            double tail = a.tailLengthKm() * 1000 * (great ? 1.0 : 0.5);
            Vec3 away = a.tailDirection().normalize();
            // the dust tail curves back along the orbit: grains leave slowly and fall behind the nucleus
            Vec3 motion = c.position(t + 86400).sub(c.position(t - 86400)).normalize();
            Vec3 dustTipPos = pos.add(away.scale(tail * 0.7)).sub(motion.scale(tail * 0.45));
            Vec3 dustMidPos = pos.add(away.scale(tail * 0.45)).sub(motion.scale(tail * 0.1));
            Vec3 ionTipPos = pos.add(away.scale(tail));
            Vector3f head = b.local(rel);
            Vector3f tip = b.local(ionTipPos.sub(obs));
            Vector3f dustMid = b.local(dustMidPos.sub(obs));
            Vector3f dustTip = b.local(dustTipPos.sub(obs));
            // apparent coma radius at the 100-block sky sphere: a clear halo for great comets, a small smudge otherwise
            float size = (float) Mth.clamp(100 * a.comaRadiusKm() * 1000 / d, great ? 1.3 : 0.45, great ? 4.0 : 1.5);
            double au = d / 1.495978707e11;
            float alpha = (float) Mth.clamp((great ? 0.95 : 0.4) * Math.min(1.0, 1.2 / Math.max(0.05, au)), 0.12, 1.0);
            out.add(new Sighting(c.id(), c.nameEn(), c.nameTr(), head, tip, dustMid, dustTip, size, alpha, a.level()));
        }
        return out;
    }

    /** Sun frame of {@link PlanetPoints}: +y toward the sun, ecliptic north = +x, bodies east of the sun at -z. */
    static final class Basis {
        final Vector3f e1;
        final Vector3f e2;
        final Vector3f n;

        Basis(Vec3 toSun) {
            e1 = new Vector3f((float) toSun.x(), (float) toSun.y(), (float) toSun.z()).normalize();
            Vector3f north = new Vector3f(0, 0, 1);
            e2 = new Vector3f(north).cross(e1).normalize();
            n = new Vector3f(e1).cross(e2).normalize();
        }

        Vector3f local(Vec3 v) {
            Vector3f d = new Vector3f((float) v.x(), (float) v.y(), (float) v.z()).normalize();
            return new Vector3f(d.dot(n), d.dot(e1), -d.dot(e2)).normalize();
        }
    }
}
