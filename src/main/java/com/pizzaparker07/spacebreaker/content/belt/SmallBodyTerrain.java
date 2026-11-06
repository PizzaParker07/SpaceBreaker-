package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds terrain profiles for small airless bodies (dwarf planets, asteroids, small icy moons) from a handful of physical
 * numbers instead of hand-tuned JSON: the world's size follows the body's radius, crater sizes follow the world size,
 * and old, battered surfaces get more and bigger craters. Ceres and Vesta are made with it; a procedural asteroid
 * generator can call it with random materials and numbers to get a consistent rubble world.
 */
public final class SmallBodyTerrain {
    private final ResourceLocation body;
    private final double radiusKm;
    private int baseHeight = 72;
    private double roughness = 1.0;
    private double craterDensity = 1.0;
    private double age = 1.0;
    private double reliefAmplitude = -1;
    private Materials materials;
    private Landforms landforms = Landforms.NONE;
    private double iceLatitude = 80;

    /** The blocks of the crust. */
    public record Materials(BlockState soil, BlockState rock, BlockState breccia, BlockState raySoil, Optional<BlockState> polarSoil,
                            Optional<BlockState> ice) {
        public Materials(BlockState soil, BlockState rock) {
            this(soil, rock, rock, soil, Optional.empty(), Optional.empty());
        }
    }

    private SmallBodyTerrain(ResourceLocation body, double radiusKm) {
        this.body = body;
        this.radiusKm = radiusKm;
    }

    public static SmallBodyTerrain of(ResourceLocation body, double radiusKm) {
        return new SmallBodyTerrain(body, radiusKm);
    }

    public SmallBodyTerrain baseHeight(int y) {
        this.baseHeight = y;
        return this;
    }

    /** 1 = typical; higher gives taller hills and rougher ground. */
    public SmallBodyTerrain roughness(double r) {
        this.roughness = r;
        return this;
    }

    /** Overrides the hill height computed from the radius. */
    public SmallBodyTerrain relief(double amplitude) {
        this.reliefAmplitude = amplitude;
        return this;
    }

    /** Crater density multiplier (1 = typical saturated small body). */
    public SmallBodyTerrain craters(double density) {
        this.craterDensity = density;
        return this;
    }

    /** Surface age 0..1: young surfaces (resurfaced ice) have few large craters, old ones are saturated. */
    public SmallBodyTerrain age(double age) {
        this.age = age;
        return this;
    }

    public SmallBodyTerrain materials(Materials m) {
        this.materials = m;
        return this;
    }

    public SmallBodyTerrain landforms(Landforms l) {
        this.landforms = l;
        return this;
    }

    /** Latitude above which deep crater floors stay in permanent shadow. */
    public SmallBodyTerrain iceLatitude(double lat) {
        this.iceLatitude = lat;
        return this;
    }

    /**
     * Blocks from the equator to a pole. Grows slowly with the radius so a 10 km moonlet is a few minutes' walk around
     * and a 500 km dwarf planet still fits a long expedition (Phobos: 900, Ceres: about 3100).
     */
    public static double poleDistance(double radiusKm) {
        return Math.round(900 * Math.pow(Math.max(1, radiusKm) / 11.0, 1 / 3.0) / 50.0) * 50.0;
    }

    public TerrainProfile build() {
        if (materials == null) {
            throw new IllegalStateException("materials missing for " + body);
        }
        double pole = poleDistance(radiusKm);
        double size = pole / 3000.0;
        double amp = reliefAmplitude > 0 ? reliefAmplitude : (12 + 10 * Math.min(1.5, size)) * roughness;
        TerrainProfile.Relief relief = new TerrainProfile.Relief(amp, 260 + 140 * size, 1.6 * roughness, 26, 0.55 * roughness);
        // no maria on these bodies: lowlands effectively off
        TerrainProfile.Lowlands lowlands = new TerrainProfile.Lowlands(0.95, 2400, 6, 0.1, 0, 1.0);
        double big = 40 + 50 * Math.min(1.4, size);
        List<TerrainProfile.CraterClass> classes = new ArrayList<>();
        classes.add(new TerrainProfile.CraterClass(2.5, 6, 32, 0.55, 2));
        classes.add(new TerrainProfile.CraterClass(6, 16, 96, 0.55, 2));
        classes.add(new TerrainProfile.CraterClass(16, big * 0.45, 270, 0.4 + 0.2 * age, 1));
        classes.add(new TerrainProfile.CraterClass(big * 0.45, big, (int) (big * 7), 0.25 + 0.4 * age, 1));
        TerrainProfile.Craters craters = new TerrainProfile.Craters(classes, craterDensity * (0.5 + 0.6 * age), 1.0, 0.2, 0.05,
                30, 0.4, 0.06, 4);
        TerrainProfile.Poles poles = new TerrainProfile.Poles(Math.min(iceLatitude, 80) - 6, iceLatitude, 4, 1.3);
        TerrainProfile.Materials mat = new TerrainProfile.Materials(materials.soil(), materials.soil(), materials.raySoil(), materials.rock(),
                materials.rock(), materials.breccia(), materials.polarSoil(), materials.ice(), Optional.empty(),
                Blocks.BEDROCK.defaultBlockState(), 3, 18);
        return new TerrainProfile(body, baseHeight, pole, relief, lowlands, craters, poles, mat, landforms);
    }
}
