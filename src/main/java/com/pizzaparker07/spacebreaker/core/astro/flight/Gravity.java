package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.List;

/** Real-scale gravity on a craft, in a frame that moves with a scene's central body. */
public final class Gravity {
    private Gravity() {
    }

    /**
     * Acceleration of a craft at {@code rel} (m, relative to {@code center}) at time t, from {@code bodies}
     * (the centre included), in the centre's non-rotating frame. The pull of the other bodies on the centre itself is
     * subtracted (third-body perturbation), so the frame's own acceleration does not appear.
     */
    public static Vec3 acceleration(BodySystem sys, AstroBody center, List<AstroBody> bodies, Vec3 rel, double t) {
        Vec3 c = sys.position(center, t);
        Vec3 craft = c.add(rel);
        Vec3 a = Vec3.ZERO;
        for (AstroBody b : bodies) {
            Vec3 pb = sys.position(b, t);
            a = a.add(pull(b.mu(), craft, pb));
            if (b != center) {
                a = a.sub(pull(b.mu(), c, pb));
            }
        }
        return a;
    }

    /** Acceleration at {@code at} towards a point mass {@code mu} at {@code source}. */
    public static Vec3 pull(double mu, Vec3 at, Vec3 source) {
        Vec3 d = source.sub(at);
        double r = d.length();
        return r == 0 ? Vec3.ZERO : d.scale(mu / (r * r * r));
    }
}
