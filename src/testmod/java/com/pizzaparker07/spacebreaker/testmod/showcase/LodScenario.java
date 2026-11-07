package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Distant Horizons check (dev only): a short normal view distance and a long wait so DH builds LOD terrain beyond
 * it, on the Moon (space sky over LODs) and Venus (haze over LODs). Run with DH in run-mods and -PwithCompat=true.
 */
final class LodScenario implements ShowcaseScenario {
    @Override
    public void build(ServerLevel level) {
    }

    private static ShowcaseDriver.Shot shot(String name, ResourceKey<Level> dim, float yaw, float pitch, double dayFraction) {
        return new ShowcaseDriver.Shot(name, 6000, 0, 0, 0, 0F, 0F, true, CameraType.FIRST_PERSON, 1800,
                p -> {
                    ServerLevel level = p.server.getLevel(dim);
                    if (level == null) {
                        return;
                    }
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0) + 30;
                    p.teleportTo(level, 0.5, y, 0.5, yaw, pitch);
                    PlanetTime.setDayFraction(level, dayFraction);
                },
                () -> Minecraft.getInstance().options.renderDistance().set(6));
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        return List.of(shot("01_lod_moon", MoonModule.MOON, 30F, 12F, 0.15), shot("02_lod_venus", VenusModule.VENUS, 30F, 12F, 0.25));
    }
}
