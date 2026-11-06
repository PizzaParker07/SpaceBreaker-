package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.MoonletDungeonPiece;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Phobos or Deimos: Mars hanging in the black sky at several phases, the grooved or dusty ground, and the moon's dungeon. */
final class MarsMoonScenario implements ShowcaseScenario {
    private final boolean deimos;
    private PlanetTerrain terrain;
    private static final int DX = 40;
    private static final int DZ = -30;

    MarsMoonScenario(boolean deimos) {
        this.deimos = deimos;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return deimos ? MarsModule.DEIMOS : MarsModule.PHOBOS;
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        int y = (int) Math.floor(terrain.height(DX + 7, DZ + 7));
        MarsScenario.placePiece(level, new MoonletDungeonPiece(77L, deimos, DX, y, DZ));
    }

    private static Consumer<ServerPlayer> at(double f) {
        return p -> PlanetTime.setDayFraction(p.serverLevel(), f);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        PlanetTerrain t = terrain();
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        String w = deimos ? "deimos" : "phobos";
        double gy = Math.floor(t.height(0.5, 0.5)) + 1;
        Vector3d mars = SkyMath.parentDirection(t.latitude(0.5), t.longitude(0.5), 0.25);
        float yaw = MoonScenario.yaw(mars.x, mars.z);
        float pitch = MoonScenario.pitch(mars.x, mars.y, mars.z);
        s.add(new ShowcaseDriver.Shot("01_" + w + "_mars_half", 0, 0.5, gy + 2, 0.5, yaw, pitch * 0.8F, true, fp, 220, at(0.0), MoonScenario::farView));
        s.add(new ShowcaseDriver.Shot("02_" + w + "_mars_full", 0, 0.5, gy + 2, 0.5, yaw, pitch * 0.8F, true, fp, 80, at(0.75), null));
        s.add(new ShowcaseDriver.Shot("03_" + w + "_ground", 0, 0.5, gy + 6, 0.5, yaw + 120, 20F, true, fp, 80, at(0.12), null));
        int y = (int) Math.floor(t.height(DX + 7, DZ + 7));
        Consumer<ServerPlayer> lit = p -> {
            PlanetTime.setDayFraction(p.serverLevel(), 0.2);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
        };
        s.add(new ShowcaseDriver.Shot("04_" + w + "_dungeon", 0, DX + 7.5, y + (deimos ? -4.4 : 5), DZ + (deimos ? 7.5 : 13.5), 180F,
                deimos ? 10F : 38F, true, fp, 120, deimos ? lit : at(0.2), null));
        return s;
    }
}
