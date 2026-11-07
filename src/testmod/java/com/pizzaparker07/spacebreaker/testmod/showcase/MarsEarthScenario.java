package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetPoints;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Earth and the Moon from Mars: a bright blue-white evening (or morning) star low in the twilight, with the faint Moon
 * beside it. The camera is aimed at Earth's dot from the same sky ephemeris the sky draws it with.
 */
final class MarsEarthScenario implements ShowcaseScenario {
    private PlanetTerrain terrain;

    @Override
    public ResourceKey<Level> dimension() {
        return MarsModule.MARS;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        com.pizzaparker07.spacebreaker.content.mars.MarsWeather.force(level, 0F, 0);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
        if (terrain == null) {
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        var system = BodyAdapter.system(level.registryAccess()).orElse(null);
        if (system == null) {
            return s;
        }
        PlanetPoints.Dot earth = null;
        for (PlanetPoints.Dot d : PlanetPoints.compute(system, "spacebreaker:mars", BodyAdapter.simTime(level))) {
            if (d.id().endsWith(":earth")) {
                earth = d;
            }
        }
        if (earth == null) {
            return s;
        }
        // east of the sun (lagging it, -z in the sun frame): an evening star; otherwise a morning star
        boolean evening = earth.local().z() < 0;
        double fraction = evening ? 0.548 : 0.952;
        long dayTicks = PlanetTime.clock(level).map(PlanetTime.Clock::dayTicks).orElse(24000L);
        float tod = new PlanetTime.Clock(dayTicks, 0).timeOfDay(Math.round(fraction * dayTicks));
        // the sky's sun frame (PlanetDots.vanillaSunFrame): turn the dot's sun-frame direction into the world
        Vector3f dir = new Quaternionf().rotateY((float) Math.toRadians(-90)).rotateX((float) (tod * Math.PI * 2))
                .transform(new Vector3f(earth.local()));
        float yaw = MoonScenario.yaw(dir.x, dir.z);
        float pitch = MoonScenario.pitch(dir.x, dir.y, dir.z);
        double gy = Math.floor(terrain.height(0.5, 0.5)) + 3;
        String name = evening ? "31_earth_evening_star" : "31_earth_morning_star";
        s.add(new ShowcaseDriver.Shot(name, 0, 0.5, gy, 0.5, yaw, pitch, true, CameraType.FIRST_PERSON, 120,
                p -> PlanetTime.setDayFraction(p.serverLevel(), fraction), () -> Minecraft.getInstance().options.fov().set(30)));
        return s;
    }
}
