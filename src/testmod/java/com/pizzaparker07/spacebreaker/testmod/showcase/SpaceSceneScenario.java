package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.SpaceSceneLayout;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.flight.SceneScale;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.client.CameraType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The Earth–Moon travel scene seen from a free development camera (no ship yet): from low Earth orbit out to the Moon
 * in five frames. The camera's scene position goes to the sky through -Dspacebreaker.spaceCamera; the player only
 * supplies the view direction, hanging in the empty orbit dimension.
 */
final class SpaceSceneScenario implements ShowcaseScenario {
    private static final ResourceKey<Level> ORBIT = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("earth_orbit"));

    /** Where to put the scene camera and what to look at, from the scene's layout. */
    private interface Framing {
        Vector3d[] frame(Vector3d earth, double earthR, Vector3d moon, double moonR, Vector3d sun);
    }

    @Override
    public ResourceKey<Level> dimension() {
        return ORBIT;
    }

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        // 1: low orbit over the day side, looking along the horizon
        s.add(shot("01_low_earth_orbit", (e, er, m, mr, sun) -> {
            Vector3d up = new Vector3d(sun).add(0, 0.5, 0).normalize();
            Vector3d side = new Vector3d(up).cross(0, 1, 0).normalize();
            Vector3d cam = new Vector3d(up).mul(er + 15).add(e);
            return new Vector3d[]{cam, new Vector3d(side).mul(er * 1.3).add(new Vector3d(up).mul(er * 0.55)).add(e)};
        }));
        // 2: leaving Earth: the whole globe, half lit
        s.add(shot("02_leaving_earth", (e, er, m, mr, sun) -> {
            Vector3d side = new Vector3d(sun).cross(0, 1, 0).normalize();
            Vector3d cam = new Vector3d(side).mul(er * 4).add(new Vector3d(sun).mul(er * 1.5)).add(0, er * 0.8, 0).add(e);
            return new Vector3d[]{cam, e};
        }));
        // 3: halfway, looking ahead at the small Moon
        s.add(nearMoon("03_toward_the_moon", (e, er, m, mr, sun) -> {
            Vector3d cam = new Vector3d(m).sub(e).mul(0.5).add(e).add(0, er * 0.5, 0);
            return new Vector3d[]{cam, m};
        }));
        // 4: closing in on the Moon: finer blocks
        s.add(nearMoon("04_near_the_moon", (e, er, m, mr, sun) -> {
            Vector3d back = new Vector3d(e).sub(m).normalize();
            Vector3d cam = new Vector3d(back).add(new Vector3d(sun).mul(0.8)).normalize().mul(mr * 3.2).add(m).add(0, mr * 0.6, 0);
            return new Vector3d[]{cam, m};
        }));
        // 5: low over the Moon, Earth hanging beyond its limb
        s.add(nearMoon("05_earth_over_the_moon", (e, er, m, mr, sun) -> {
            Vector3d toEarth = new Vector3d(e).sub(m).normalize();
            Vector3d side = new Vector3d(toEarth).cross(0, 1, 0).normalize();
            Vector3d cam = new Vector3d(toEarth).mul(-0.35).add(new Vector3d(side).mul(0.8)).normalize().mul(mr * 1.25).add(m);
            return new Vector3d[]{cam, e};
        }));
        return s;
    }

    private static ShowcaseDriver.Shot shot(String name, Framing framing) {
        return new ShowcaseDriver.Shot(name, 0, 0.5, 200, 0.5, 0F, 0F, true, CameraType.FIRST_PERSON, 80, p -> aim(p, framing, false), null);
    }

    /** A frame whose camera rides with the Moon (it moves tens of blocks while a frame waits). */
    private static ShowcaseDriver.Shot nearMoon(String name, Framing framing) {
        return new ShowcaseDriver.Shot(name, 0, 0.5, 200, 0.5, 0F, 0F, true, CameraType.FIRST_PERSON, 80, p -> aim(p, framing, true), null);
    }

    /** Sets the clock to a waxing gibbous Moon (lit side toward Earth, terminator in view), once. */
    private static boolean timed;

    private static void gibbous(ServerPlayer p) {
        if (timed) {
            return;
        }
        timed = true;
        var sys = BodyAdapter.system(p.server.registryAccess()).orElseThrow();
        double t0 = BodyAdapter.simTime(p.serverLevel());
        var earth = sys.require("spacebreaker:earth");
        var moon = sys.require("spacebreaker:moon");
        for (double t = t0; t < t0 + 32 * 86400; t += 3600) {
            var e = sys.position(earth, t);
            var toMoon = sys.position(moon, t).sub(e).normalize();
            var toSun = e.scale(-1).normalize();
            double elong = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, toMoon.dot(toSun)))));
            if (elong > 120 && elong < 125) {
                long ticks = com.pizzaparker07.spacebreaker.core.astro.GameClock.ticks(t, BodyAdapter.timeScale());
                ((net.minecraft.world.level.storage.ServerLevelData) p.server.getWorldData().overworldData()).setGameTime(ticks);
                return;
            }
        }
    }

    private static void aim(ServerPlayer p, Framing framing, boolean rideMoon) {
        gibbous(p);
        var access = p.server.registryAccess();
        var sys = BodyAdapter.system(access).orElseThrow();
        var reg = Bodies.registry(access).orElseThrow();
        double t = BodyAdapter.simTime(p.serverLevel());
        Function<String, com.pizzaparker07.spacebreaker.core.astro.BodyLook> looks = id -> BodyAdapter.look(sys, ResourceLocation.parse(id),
                reg.get(ResourceLocation.parse(id)), sys.position(sys.require("spacebreaker:earth"), t), t);
        var bodies = SpaceSceneLayout.layout(sys, SceneScale.EARTH_MOON, "spacebreaker:earth", List.of("spacebreaker:earth", "spacebreaker:moon"), t, looks);
        var earth = bodies.get(0);
        var moon = bodies.get(1);
        Vector3d sun = SpaceSceneLayout.sunDirection(sys, "spacebreaker:earth", t);
        Vector3d[] f = framing.frame(earth.center(), earth.radius(), moon.center(), moon.radius(), sun);
        Vector3d look = new Vector3d(f[1]).sub(f[0]);
        if (rideMoon) {
            Vector3d off = new Vector3d(f[0]).sub(moon.center());
            System.setProperty("spacebreaker.spaceCamera", "spacebreaker:moon|" + off.x + "," + off.y + "," + off.z);
        } else {
            System.setProperty("spacebreaker.spaceCamera", f[0].x + "," + f[0].y + "," + f[0].z);
        }
        Spacebreaker.LOGGER.info("space scene camera {} looking at {} (Earth r {}, Moon {} r {})", f[0], f[1], earth.radius(), moon.center(), moon.radius());
        p.teleportTo(p.serverLevel(), 0.5, 200, 0.5, MoonScenario.yaw(look.x, look.z), MoonScenario.pitch(look.x, look.y, look.z));
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }
}
