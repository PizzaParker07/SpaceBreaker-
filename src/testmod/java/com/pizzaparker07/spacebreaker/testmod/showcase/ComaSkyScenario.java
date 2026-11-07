package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.GameClock;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;

/** The sky of comet 67P from its own surface: the coma glowing around a bigger Sun near perihelion, a dark sky far out. */
final class ComaSkyScenario implements ShowcaseScenario {
    @Override
    public ResourceKey<Level> dimension() {
        return com.pizzaparker07.spacebreaker.content.belt.Comets.CHURYUMOV.level;
    }

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
        double y = level.getChunkSource().getGenerator().getBaseHeight(0, 0, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                level, level.getChunkSource().randomState()) + 3;
        Comet c = Comet.CHURYUMOV_GERASIMENKO;
        double peri = c.nextPerihelion(0);
        return List.of(shot("01_67p_perihelion_coma", peri, y), shot("02_67p_aphelion", peri + c.periodSeconds() / 2, y));
    }

    /** Morning view toward the low Sun in the east, at simulation time t. */
    private static ShowcaseDriver.Shot shot(String name, double t, double y) {
        long ticks = GameClock.ticks(t, BodyAdapter.timeScale());
        return new ShowcaseDriver.Shot(name, 0, 0.5, y, 0.5, -90F, -22F, true, CameraType.FIRST_PERSON, 160, p -> {
            ((net.minecraft.world.level.storage.ServerLevelData) p.server.getWorldData().overworldData()).setGameTime(ticks);
            PlanetTime.setDayFraction(p.serverLevel(), 0.06);
        }, null);
    }
}
