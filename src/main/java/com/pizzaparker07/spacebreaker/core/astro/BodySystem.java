package com.pizzaparker07.spacebreaker.core.astro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A tree of bodies (a star system) with lookups and positions over time. Time t is seconds since J2000
 * of simulated orbital time (see {@link GameClock} for the game-time mapping).
 */
public final class BodySystem {
    private final Map<String, AstroBody> bodies = new LinkedHashMap<>();

    public BodySystem(Collection<AstroBody> list) {
        for (AstroBody b : list) {
            bodies.put(b.id(), b);
        }
    }

    public Optional<AstroBody> get(String id) {
        return Optional.ofNullable(bodies.get(id));
    }

    public AstroBody require(String id) {
        AstroBody b = bodies.get(id);
        if (b == null) {
            throw new IllegalArgumentException("unknown body " + id);
        }
        return b;
    }

    public Collection<AstroBody> all() {
        return Collections.unmodifiableCollection(bodies.values());
    }

    public Optional<AstroBody> parent(AstroBody b) {
        return b.parent() == null ? Optional.empty() : get(b.parent());
    }

    public List<AstroBody> children(String id) {
        List<AstroBody> out = new ArrayList<>();
        for (AstroBody b : bodies.values()) {
            if (id.equals(b.parent())) {
                out.add(b);
            }
        }
        return out;
    }

    /** Chain from the body up to the root, body first. */
    public List<AstroBody> ancestry(AstroBody b) {
        List<AstroBody> out = new ArrayList<>();
        AstroBody cur = b;
        while (cur != null) {
            out.add(cur);
            cur = cur.parent() == null ? null : bodies.get(cur.parent());
            if (out.size() > 32) {
                throw new IllegalStateException("parent cycle at " + b.id());
            }
        }
        return out;
    }

    /** The root body (star) of the chain. */
    public AstroBody root(AstroBody b) {
        List<AstroBody> chain = ancestry(b);
        return chain.get(chain.size() - 1);
    }

    /** Position relative to the parent at time t, m. */
    public Vec3 relativePosition(AstroBody b, double t) {
        return b.orbit() == null ? Vec3.ZERO : b.orbit().stateAt(t).position();
    }

    /** Position relative to the root (star), m. */
    public Vec3 position(AstroBody b, double t) {
        Vec3 p = Vec3.ZERO;
        for (AstroBody cur : ancestry(b)) {
            p = p.add(relativePosition(cur, t));
        }
        return p;
    }

    /** Velocity relative to the root, m/s. */
    public Vec3 velocity(AstroBody b, double t) {
        Vec3 v = Vec3.ZERO;
        for (AstroBody cur : ancestry(b)) {
            if (cur.orbit() != null) {
                v = v.add(cur.orbit().stateAt(t).velocity());
            }
        }
        return v;
    }

    /** Distance between two bodies at time t, m. */
    public double distance(AstroBody a, AstroBody b, double t) {
        return position(a, t).distance(position(b, t));
    }

    /** Distance from the body to its star at time t, m (for a star: 0). */
    public double distanceToStar(AstroBody b, double t) {
        AstroBody star = root(b);
        return position(b, t).distance(position(star, t));
    }

    /** Lowest common ancestor of two bodies, or empty when they are in different trees. */
    public Optional<AstroBody> commonAncestor(AstroBody a, AstroBody b) {
        List<AstroBody> ca = ancestry(a);
        for (AstroBody x : ancestry(b)) {
            if (ca.contains(x)) {
                return Optional.of(x);
            }
        }
        return Optional.empty();
    }
}
