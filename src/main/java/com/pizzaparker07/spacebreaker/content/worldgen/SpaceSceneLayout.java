package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodyLook;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;
import com.pizzaparker07.spacebreaker.core.astro.flight.SceneScale;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Where the bodies of a travel scene (the Earth–Moon space dimension) stand in blocks, and how they are turned and lit:
 * the pure part of the space scene sky, so it can be checked without a client.
 * <p>
 * Scene frame: the central body's centre is block {@link #ORIGIN} (the scene dimension spans y −1024..1024); Minecraft x is ecliptic x, Minecraft y is ecliptic
 * north, Minecraft z is minus ecliptic y (right-handed, like the world). Distances follow {@link SceneScale}: the central
 * body at its drawn radius, the others at their real size for the local scale but never below {@link #MIN_RADIUS}.
 */
public final class SpaceSceneLayout {
    /** Block position of the central body's centre. */
    public static final Vector3d ORIGIN = new Vector3d(0, 0, 0);
    /** Smallest drawn radius of a body other than the central one, blocks: far moons stay visible targets. */
    public static final double MIN_RADIUS = 8;
    /** The Moon is drawn at least this big (blocks) so it reads as a landable target; at the true local scale it is about 2. */
    public static final double MOON_MIN_RADIUS = 24;

    /** Smallest drawn radius for a body. */
    public static double minRadius(String id) {
        return id.equals("spacebreaker:moon") ? MOON_MIN_RADIUS : MIN_RADIUS;
    }

    /**
     * One body of the scene.
     *
     * @param center   centre, block coordinates
     * @param radius   drawn radius, blocks
     * @param pole     north pole direction (Minecraft frame)
     * @param meridian direction of longitude 0 on the equator (Minecraft frame)
     * @param sun      direction from the body toward the Sun (Minecraft frame)
     */
    public record Body(String id, Vector3d center, double radius, Vector3d pole, Vector3d meridian, Vector3d sun) {
    }

    private SpaceSceneLayout() {
    }

    /**
     * Ecliptic (m or unit) to the Minecraft scene frame: a proper rotation (determinant +1), so handedness is kept. The
     * ship code uses the same pair.
     */
    public static Vector3d toScene(Vec3 v) {
        return new Vector3d(v.x(), v.z(), -v.y());
    }

    /** Minecraft scene frame back to ecliptic. */
    public static Vec3 fromScene(Vector3d v) {
        return new Vec3(v.x, -v.z, v.y);
    }

    /**
     * The bodies around {@code central} at simulated time {@code t}.
     *
     * @param looks how each body is turned (BodyAdapter.look with the client's registry), by id
     */
    public static List<Body> layout(BodySystem sys, SceneScale scale, String central, List<String> ids, double t,
                                    Function<String, BodyLook> looks) {
        AstroBody c = sys.require(central);
        Vec3 p0 = sys.position(c, t);
        List<Body> out = new ArrayList<>();
        for (String id : ids) {
            AstroBody b = sys.require(id);
            Vec3 rel = sys.position(b, t).sub(p0);
            double d = rel.length();
            Vector3d centre = toScene(scale.toBlocks(rel)).add(ORIGIN);
            double radius = id.equals(central) ? scale.radiusBlocks() : scale.displayRadius(b.radius(), Math.max(d, c.radius()), minRadius(id));
            BodyLook look = looks.apply(id);
            out.add(new Body(id, centre, radius, toScene(look.spinAxis()).normalize(), toScene(look.primeMeridian()).normalize(),
                    toScene(look.sunDirection()).normalize()));
        }
        return out;
    }

    /** Direction toward the Sun seen from the scene (from the central body; the scene is tiny next to the Sun's distance). */
    public static Vector3d sunDirection(BodySystem sys, String central, double t) {
        return toScene(sys.position(sys.require(central), t).scale(-1)).normalize();
    }
}
