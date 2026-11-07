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
    /** Corner of Deimos' relay spire in the showcase. */
    private static final int SX = -70;
    private static final int SZ = 30;

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
        // a little group of Tether Spinners, one mid-hop on its silk line
        int gy = (int) Math.floor(terrain.height(SPX, SPZ)) + 1;
        spinner(level, SPX - 2, gy, SPZ + 1, 200F, false);
        spinner(level, SPX + 2, (int) Math.floor(terrain.height(SPX + 2, SPZ - 1)) + 1, SPZ - 1, 140F, false);
        var hop = spinner(level, SPX, gy + 3, SPZ, 180F, true);
        if (hop != null) {
            hop.anchorAt(new net.minecraft.world.phys.Vec3(SPX + 0.5, gy, SPZ + 4.5));
        }
        if (deimos) {
            int sy = (int) Math.floor(terrain.height(SX + 13, SZ + 13)) + 1;
            MarsScenario.placePiece(level, new com.pizzaparker07.spacebreaker.content.mars.dungeon.HopSpirePiece(19L, SX, sy, SZ));
        }
        if (!deimos) {
            // the showcase world is made without structures: build Stickney's lander where the world would put it
            com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderStructure.site(level.getChunkSource().getGenerator()).ifPresent(site -> {
                int half = com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderPiece.SIZE / 2;
                int ly = (int) Math.floor(terrain.height(site.getX() + half, site.getZ() + half)) + 1;
                MarsScenario.placePiece(level, new com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderPiece(31L, site.getX(), ly, site.getZ()));
            });
        }
    }

    /** Corner of the spinner group in the showcase. */
    private static final int SPX = 14;
    private static final int SPZ = -50;

    private static com.pizzaparker07.spacebreaker.content.mars.entity.TetherSpinner spinner(ServerLevel level, int x, int y, int z, float yaw, boolean air) {
        var mob = com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.TETHER_SPINNER.get().create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(x + 0.5, y, z + 0.5, yaw, 0F);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setNoAi(true);
        mob.setNoGravity(air);
        mob.setPersistenceRequired();
        mob.setSilent(true);
        level.addFreshEntity(mob);
        return mob;
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
        double spy = Math.floor(t.height(SPX, SPZ)) + 1;
        s.add(new ShowcaseDriver.Shot("07_" + w + "_tether_spinners", 0, SPX + 0.5, spy + 1.6, SPZ - 3.2, 0F, -4F, true, fp, 140, at(0.26), null));
        if (deimos) {
            double cx = SX + 13.5;
            double cz = SZ + 13.5 + 40;
            s.add(new ShowcaseDriver.Shot("05_deimos_relay_spire", 0, cx, Math.floor(t.height(SX + 13, SZ + 13)) + 16, cz, 180F, 2F,
                    true, fp, 200, at(0.26), MoonScenario::farView));
        }
        if (!deimos) {
            t.namedCrater("stickney").ifPresent(c -> {
                // the floor of Stickney from its western slope: the anchored lander small on the floor, the far wall behind
                double lx0 = c.x() + com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderStructure.OFFSET_X;
                double lz0 = c.z() + com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderStructure.OFFSET_Z;
                double rx = lx0 - 45;
                s.add(new ShowcaseDriver.Shot("05_phobos_stickney", 0, rx, Math.floor(t.height(rx, lz0)) + 18, lz0 + 0.5, -90F, 12F,
                        true, fp, 300, at(0.26), MoonScenario::farView));
                double lx = c.x() + com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderStructure.OFFSET_X + 0.5;
                double lz = c.z() + com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderStructure.OFFSET_Z + 0.5;
                s.add(new ShowcaseDriver.Shot("06_phobos_stickney_lander", 0, lx - 9, Math.floor(t.height(lx, lz)) + 7, lz + 9, -135F, 28F,
                        true, fp, 200, at(0.26), MoonScenario::farView));
            });
        }
        return s;
    }
}
