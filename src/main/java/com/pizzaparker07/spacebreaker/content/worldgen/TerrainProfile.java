package com.pizzaparker07.spacebreaker.content.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * How a rocky planet or moon looks: relief, lowland basins (maria), impact craters, poles and the blocks
 * its crust is made of. Loaded from {@code data/<ns>/spacebreaker/terrain/<name>.json} and used by
 * {@link PlanetChunkGenerator}. Every length is in blocks; the generator is deterministic for the world seed.
 *
 * @param body         celestial body this terrain belongs to (gravity, sky, time)
 * @param baseHeight   mean surface height (y)
 * @param poleDistance distance in blocks along z from the equator (z = 0) to a pole; latitude grows with |z|
 * @param relief       highland relief and fine detail
 * @param lowlands     smooth dark basins (lunar maria, martian plains)
 * @param craters      impact crater population and shape
 * @param poles        polar climate: permanently shadowed, icy crater floors
 * @param materials    blocks of the crust
 * @param landforms    optional larger landforms: dunes, rifts, volcanoes, valleys, strata, ice caps, scarps ...
 */
public record TerrainProfile(ResourceLocation body, int baseHeight, double poleDistance, Relief relief, Lowlands lowlands,
                             Craters craters, Poles poles, Materials materials, Landforms landforms) {
    public static final ResourceKey<Registry<TerrainProfile>> REGISTRY = ResourceKey.createRegistryKey(Spacebreaker.id("terrain"));

    public static final Codec<TerrainProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("body").forGetter(TerrainProfile::body),
            Codec.INT.optionalFieldOf("base_height", 80).forGetter(TerrainProfile::baseHeight),
            Codec.DOUBLE.optionalFieldOf("pole_distance", 8000.0).forGetter(TerrainProfile::poleDistance),
            Relief.CODEC.optionalFieldOf("relief", Relief.DEFAULT).forGetter(TerrainProfile::relief),
            Lowlands.CODEC.optionalFieldOf("lowlands", Lowlands.DEFAULT).forGetter(TerrainProfile::lowlands),
            Craters.CODEC.optionalFieldOf("craters", Craters.DEFAULT).forGetter(TerrainProfile::craters),
            Poles.CODEC.optionalFieldOf("poles", Poles.DEFAULT).forGetter(TerrainProfile::poles),
            Materials.CODEC.fieldOf("materials").forGetter(TerrainProfile::materials),
            Landforms.CODEC.optionalFieldOf("landforms", Landforms.NONE).forGetter(TerrainProfile::landforms)
    ).apply(i, TerrainProfile::new));

    /** Profile without extra landforms (airless, cratered worlds). */
    public TerrainProfile(ResourceLocation body, int baseHeight, double poleDistance, Relief relief, Lowlands lowlands,
                          Craters craters, Poles poles, Materials materials) {
        this(body, baseHeight, poleDistance, relief, lowlands, craters, poles, materials, Landforms.NONE);
    }

    /**
     * @param amplitude       height of highland hills and massifs
     * @param scale           horizontal size of highland hills
     * @param detailAmplitude small bumps everywhere
     * @param detailScale     size of the small bumps
     * @param roughness       fine, block-scale unevenness that breaks smooth slopes into ragged, natural steps (0 = none)
     */
    public record Relief(double amplitude, double scale, double detailAmplitude, double detailScale, double roughness) {
        public static final Relief DEFAULT = new Relief(22, 520, 2.5, 36, 0);

        public Relief(double amplitude, double scale, double detailAmplitude, double detailScale) {
            this(amplitude, scale, detailAmplitude, detailScale, 0);
        }
        public static final Codec<Relief> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("amplitude", DEFAULT.amplitude).forGetter(Relief::amplitude),
                Codec.DOUBLE.optionalFieldOf("scale", DEFAULT.scale).forGetter(Relief::scale),
                Codec.DOUBLE.optionalFieldOf("detail_amplitude", DEFAULT.detailAmplitude).forGetter(Relief::detailAmplitude),
                Codec.DOUBLE.optionalFieldOf("detail_scale", DEFAULT.detailScale).forGetter(Relief::detailScale),
                Codec.DOUBLE.optionalFieldOf("roughness", 0.0).forGetter(Relief::roughness)
        ).apply(i, Relief::new));
    }

    /**
     * @param threshold     noise level above which ground is lowland (higher = fewer lowlands)
     * @param scale         size of the basins
     * @param depth         how far lowland floors lie below the mean height
     * @param edge          width of the shore between lowland and highland (noise units)
     * @param ridgeHeight   wrinkle ridges on the lowland floor
     * @param volcanicPatch chance-like threshold for pyroclastic (volcanic glass) deposits near the shores; 1 = none
     */
    public record Lowlands(double threshold, double scale, double depth, double edge, double ridgeHeight, double volcanicPatch,
                           java.util.Optional<Dichotomy> dichotomy) {
        public static final Lowlands DEFAULT = new Lowlands(0.18, 2400, 14, 0.12, 1.6, 0.72, java.util.Optional.empty());

        public Lowlands(double threshold, double scale, double depth, double edge, double ridgeHeight, double volcanicPatch) {
            this(threshold, scale, depth, edge, ridgeHeight, volcanicPatch, java.util.Optional.empty());
        }
        public static final Codec<Lowlands> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("threshold", DEFAULT.threshold).forGetter(Lowlands::threshold),
                Codec.DOUBLE.optionalFieldOf("scale", DEFAULT.scale).forGetter(Lowlands::scale),
                Codec.DOUBLE.optionalFieldOf("depth", DEFAULT.depth).forGetter(Lowlands::depth),
                Codec.DOUBLE.optionalFieldOf("edge", DEFAULT.edge).forGetter(Lowlands::edge),
                Codec.DOUBLE.optionalFieldOf("ridge_height", DEFAULT.ridgeHeight).forGetter(Lowlands::ridgeHeight),
                Codec.DOUBLE.optionalFieldOf("volcanic_patch", DEFAULT.volcanicPatch).forGetter(Lowlands::volcanicPatch),
                Dichotomy.CODEC.optionalFieldOf("dichotomy").forGetter(Lowlands::dichotomy)
        ).apply(i, Lowlands::new));
    }

    /**
     * A hemispheric split (like Mars): lowlands favoured on one side of a wavy boundary latitude, highlands on the other.
     *
     * @param latitude boundary latitude (degrees, + north); lowlands lie poleward of it on the north side
     * @param width    width of the transition in degrees
     * @param strength how strongly the noise threshold is shifted (noise units)
     * @param wave     amplitude of the boundary's wander in degrees
     */
    public record Dichotomy(double latitude, double width, double strength, double wave) {
        public static final Codec<Dichotomy> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("latitude", 15.0).forGetter(Dichotomy::latitude),
                Codec.DOUBLE.optionalFieldOf("width", 12.0).forGetter(Dichotomy::width),
                Codec.DOUBLE.optionalFieldOf("strength", 0.45).forGetter(Dichotomy::strength),
                Codec.DOUBLE.optionalFieldOf("wave", 12.0).forGetter(Dichotomy::wave)
        ).apply(i, Dichotomy::new));
    }

    /**
     * One size class of craters, scattered on a jittered grid.
     *
     * @param minRadius smallest rim radius
     * @param maxRadius largest rim radius (sizes follow a power law between the two)
     * @param cell      grid cell size; each cell holds up to {@code perCell} craters
     * @param chance    probability for each slot of a cell to hold a crater
     * @param perCell   crater slots per cell
     */
    public record CraterClass(double minRadius, double maxRadius, int cell, double chance, int perCell) {
        public static final Codec<CraterClass> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("min_radius").forGetter(CraterClass::minRadius),
                Codec.DOUBLE.fieldOf("max_radius").forGetter(CraterClass::maxRadius),
                Codec.INT.fieldOf("cell").forGetter(CraterClass::cell),
                Codec.DOUBLE.fieldOf("chance").forGetter(CraterClass::chance),
                Codec.INT.optionalFieldOf("per_cell", 1).forGetter(CraterClass::perCell)
        ).apply(i, CraterClass::new));
    }

    /**
     * @param classes        size classes (small to large)
     * @param density        global multiplier on every class's chance
     * @param lowlandDensity multiplier inside lowlands (young lava plains have fewer craters)
     * @param depthRatio     depth / diameter of simple (bowl) craters
     * @param rimRatio       rim height / diameter
     * @param complexRadius  above this radius craters get flat floors, terraces and central peaks
     * @param centralPeak    central peak height relative to the crater depth
     * @param freshChance    share of large craters that are young: bright ejecta rays, sharp rims
     * @param rayLength      length of bright rays in crater radii
     */
    public record Craters(List<CraterClass> classes, double density, double lowlandDensity, double depthRatio, double rimRatio,
                          double complexRadius, double centralPeak, double freshChance, double rayLength) {
        public static final Craters DEFAULT = new Craters(List.of(
                new CraterClass(2.5, 7, 40, 0.55, 2),
                new CraterClass(7, 18, 110, 0.5, 2),
                new CraterClass(18, 48, 300, 0.5, 1),
                new CraterClass(48, 130, 900, 0.55, 1)),
                1.0, 0.35, 0.2, 0.05, 36, 0.45, 0.22, 7);
        public static final Codec<Craters> CODEC = RecordCodecBuilder.create(i -> i.group(
                CraterClass.CODEC.listOf().optionalFieldOf("classes", DEFAULT.classes).forGetter(Craters::classes),
                Codec.DOUBLE.optionalFieldOf("density", DEFAULT.density).forGetter(Craters::density),
                Codec.DOUBLE.optionalFieldOf("lowland_density", DEFAULT.lowlandDensity).forGetter(Craters::lowlandDensity),
                Codec.DOUBLE.optionalFieldOf("depth_ratio", DEFAULT.depthRatio).forGetter(Craters::depthRatio),
                Codec.DOUBLE.optionalFieldOf("rim_ratio", DEFAULT.rimRatio).forGetter(Craters::rimRatio),
                Codec.DOUBLE.optionalFieldOf("complex_radius", DEFAULT.complexRadius).forGetter(Craters::complexRadius),
                Codec.DOUBLE.optionalFieldOf("central_peak", DEFAULT.centralPeak).forGetter(Craters::centralPeak),
                Codec.DOUBLE.optionalFieldOf("fresh_chance", DEFAULT.freshChance).forGetter(Craters::freshChance),
                Codec.DOUBLE.optionalFieldOf("ray_length", DEFAULT.rayLength).forGetter(Craters::rayLength)
        ).apply(i, Craters::new));
    }

    /**
     * @param polarLatitude  latitude (degrees) where the polar region starts: no lowlands, more and older craters
     * @param iceLatitude    latitude above which deep crater floors never see the sun and hold ice
     * @param iceDepth       thickness of the ice layer under icy soil
     * @param polarCraters   crater density multiplier in the polar region
     */
    public record Poles(double polarLatitude, double iceLatitude, int iceDepth, double polarCraters) {
        public static final Poles DEFAULT = new Poles(72, 80, 4, 1.5);
        public static final Codec<Poles> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("polar_latitude", DEFAULT.polarLatitude).forGetter(Poles::polarLatitude),
                Codec.DOUBLE.optionalFieldOf("ice_latitude", DEFAULT.iceLatitude).forGetter(Poles::iceLatitude),
                Codec.INT.optionalFieldOf("ice_depth", DEFAULT.iceDepth).forGetter(Poles::iceDepth),
                Codec.DOUBLE.optionalFieldOf("polar_craters", DEFAULT.polarCraters).forGetter(Poles::polarCraters)
        ).apply(i, Poles::new));
    }

    /**
     * Crust blocks. Soils form the top {@code soilDepth} blocks; steep slopes show bare rock.
     */
    public record Materials(BlockState highlandSoil, BlockState lowlandSoil, BlockState raySoil, BlockState highlandRock,
                            BlockState lowlandRock, BlockState breccia, Optional<BlockState> polarSoil, Optional<BlockState> ice,
                            Optional<BlockState> volcanicSoil, BlockState bedrock, int soilDepth, int lowlandRockDepth) {
        public static final Codec<Materials> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("highland_soil").forGetter(Materials::highlandSoil),
                BlockState.CODEC.fieldOf("lowland_soil").forGetter(Materials::lowlandSoil),
                BlockState.CODEC.fieldOf("ray_soil").forGetter(Materials::raySoil),
                BlockState.CODEC.fieldOf("highland_rock").forGetter(Materials::highlandRock),
                BlockState.CODEC.fieldOf("lowland_rock").forGetter(Materials::lowlandRock),
                BlockState.CODEC.fieldOf("breccia").forGetter(Materials::breccia),
                BlockState.CODEC.optionalFieldOf("polar_soil").forGetter(Materials::polarSoil),
                BlockState.CODEC.optionalFieldOf("ice").forGetter(Materials::ice),
                BlockState.CODEC.optionalFieldOf("volcanic_soil").forGetter(Materials::volcanicSoil),
                BlockState.CODEC.optionalFieldOf("bedrock", Blocks.BEDROCK.defaultBlockState()).forGetter(Materials::bedrock),
                Codec.INT.optionalFieldOf("soil_depth", 4).forGetter(Materials::soilDepth),
                Codec.INT.optionalFieldOf("lowland_rock_depth", 18).forGetter(Materials::lowlandRockDepth)
        ).apply(i, Materials::new));
    }
}
