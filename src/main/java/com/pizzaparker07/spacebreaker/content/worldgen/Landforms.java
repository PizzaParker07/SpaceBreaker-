package com.pizzaparker07.spacebreaker.content.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Optional large-scale landforms of a {@link TerrainProfile}, beyond highlands, lowlands and craters:
 * wind-blown dunes, a great rift canyon, shield volcanoes, ancient river valleys with deltas, layered strata,
 * polar ice caps, soil patches, thrust-fault scarps, groove chains, tessera ridges and giant fault cliffs. Every part is optional,
 * so worlds without it (the Moon) generate exactly as before. All lengths are in blocks.
 */
public record Landforms(Optional<Dunes> dunes, Optional<Rift> rift, Optional<Volcanoes> volcanoes, Optional<Valleys> valleys,
                        Optional<Strata> strata, Optional<IceCap> iceCap, List<SoilPatch> patches, Optional<Scarps> scarps,
                        Optional<Grooves> grooves, Optional<Tesserae> tesserae, Optional<Peaks> peaks, List<Rupes> rupes,
                        List<NamedCrater> namedCraters) {
    public static final Landforms NONE = new Landforms(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of());

    /** The earlier form without named craters. */
    public Landforms(Optional<Dunes> dunes, Optional<Rift> rift, Optional<Volcanoes> volcanoes, Optional<Valleys> valleys,
                     Optional<Strata> strata, Optional<IceCap> iceCap, List<SoilPatch> patches, Optional<Scarps> scarps,
                     Optional<Grooves> grooves, Optional<Tesserae> tesserae, Optional<Peaks> peaks, List<Rupes> rupes) {
        this(dunes, rift, volcanoes, valleys, strata, iceCap, patches, scarps, grooves, tesserae, peaks, rupes, List.of());
    }

    /** The earlier form without giant cliffs. */
    public Landforms(Optional<Dunes> dunes, Optional<Rift> rift, Optional<Volcanoes> volcanoes, Optional<Valleys> valleys,
                     Optional<Strata> strata, Optional<IceCap> iceCap, List<SoilPatch> patches, Optional<Scarps> scarps,
                     Optional<Grooves> grooves, Optional<Tesserae> tesserae, Optional<Peaks> peaks) {
        this(dunes, rift, volcanoes, valleys, strata, iceCap, patches, scarps, grooves, tesserae, peaks, List.of());
    }

    public static final Codec<Landforms> CODEC = RecordCodecBuilder.create(i -> i.group(
            Dunes.CODEC.optionalFieldOf("dunes").forGetter(Landforms::dunes),
            Rift.CODEC.optionalFieldOf("rift").forGetter(Landforms::rift),
            Volcanoes.CODEC.optionalFieldOf("volcanoes").forGetter(Landforms::volcanoes),
            Valleys.CODEC.optionalFieldOf("valleys").forGetter(Landforms::valleys),
            Strata.CODEC.optionalFieldOf("strata").forGetter(Landforms::strata),
            IceCap.CODEC.optionalFieldOf("ice_cap").forGetter(Landforms::iceCap),
            SoilPatch.CODEC.listOf().optionalFieldOf("patches", List.of()).forGetter(Landforms::patches),
            Scarps.CODEC.optionalFieldOf("scarps").forGetter(Landforms::scarps),
            Grooves.CODEC.optionalFieldOf("grooves").forGetter(Landforms::grooves),
            Tesserae.CODEC.optionalFieldOf("tesserae").forGetter(Landforms::tesserae),
            Peaks.CODEC.optionalFieldOf("peaks").forGetter(Landforms::peaks),
            Rupes.CODEC.listOf().optionalFieldOf("rupes", List.of()).forGetter(Landforms::rupes),
            NamedCrater.CODEC.listOf().optionalFieldOf("named_craters", List.of()).forGetter(Landforms::namedCraters)
    ).apply(i, Landforms::new));

    /**
     * One real, named crater at a fixed place (e.g. Phobos' Stickney), shaped like any other crater of its size but
     * always there, so structures and guides can point to it.
     *
     * @param name     id used by code and guides (e.g. "stickney")
     * @param x        centre (block x)
     * @param latitude centre (latitude)
     * @param radius   rim radius in blocks
     * @param peak     central peak height as a share of the depth (Stickney has none)
     * @param depth    floor depth in blocks; 0 = from the size like any crater. A depth also makes it a simple bowl
     */
    public record NamedCrater(String name, double x, double latitude, double radius, double peak, double depth) {
        public static final Codec<NamedCrater> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(NamedCrater::name),
                Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(NamedCrater::x),
                Codec.DOUBLE.optionalFieldOf("latitude", 0.0).forGetter(NamedCrater::latitude),
                Codec.DOUBLE.fieldOf("radius").forGetter(NamedCrater::radius),
                Codec.DOUBLE.optionalFieldOf("peak", 0.0).forGetter(NamedCrater::peak),
                Codec.DOUBLE.optionalFieldOf("depth", 0.0).forGetter(NamedCrater::depth)
        ).apply(i, NamedCrater::new));
    }

    /**
     * A giant fault cliff (rupes), like Miranda's: along a straight, slightly wavering line the ground on one side
     * drops by a great height in a near-vertical, layered rock wall that fades out toward the ends. Built straight
     * into the terrain (see {@link RupesShape}).
     *
     * @param x        centre of the cliff (block x)
     * @param latitude centre of the cliff (latitude)
     * @param length   length of the cliff
     * @param angleDeg direction of the cliff line (0 = along x)
     * @param height   drop at the middle of the cliff
     * @param wall     horizontal width over which the ground falls (smaller = steeper)
     * @param rock     rock of the cliff face
     * @param band     optional second rock in layers across the face
     */
    public record Rupes(double x, double latitude, double length, double angleDeg, double height, double wall, BlockState rock,
                        Optional<BlockState> band) {
        public static final Codec<Rupes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Rupes::x),
                Codec.DOUBLE.optionalFieldOf("latitude", 0.0).forGetter(Rupes::latitude),
                Codec.DOUBLE.optionalFieldOf("length", 800.0).forGetter(Rupes::length),
                Codec.DOUBLE.optionalFieldOf("angle", 0.0).forGetter(Rupes::angleDeg),
                Codec.DOUBLE.optionalFieldOf("height", 80.0).forGetter(Rupes::height),
                Codec.DOUBLE.optionalFieldOf("wall", 8.0).forGetter(Rupes::wall),
                BlockState.CODEC.fieldOf("rock").forGetter(Rupes::rock),
                BlockState.CODEC.optionalFieldOf("band").forGetter(Rupes::band)
        ).apply(i, Rupes::new));
    }

    /**
     * A different surface on the highest ground, like snow lines (e.g. Venus' metallic frost on its mountain tops).
     *
     * @param soil   surface block above the line
     * @param height absolute y of the line (blurred by a few blocks)
     */
    public record Peaks(BlockState soil, int height) {
        public static final Codec<Peaks> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("soil").forGetter(Peaks::soil),
                Codec.INT.fieldOf("height").forGetter(Peaks::height)
        ).apply(i, Peaks::new));
    }

    /**
     * Transverse dune fields: asymmetric ridges (gentle windward slope, steep slip face) across the wind.
     *
     * @param scale      size of dune fields
     * @param threshold  field noise above which dunes grow (higher = fewer fields)
     * @param wavelength crest spacing
     * @param height     crest height
     * @param windDeg    wind direction in degrees (0 = toward +x, 90 = toward +z)
     * @param lowlandOnly dunes only on lowland plains and crater floors
     * @param soil       dune sand
     * @param altSoil    second sand colour in some fields (e.g. olivine sand)
     * @param altShare   share of fields made of the second sand
     */
    public record Dunes(double scale, double threshold, double wavelength, double height, double windDeg, boolean lowlandOnly,
                        BlockState soil, Optional<BlockState> altSoil, double altShare) {
        public static final Codec<Dunes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("scale", 700.0).forGetter(Dunes::scale),
                Codec.DOUBLE.optionalFieldOf("threshold", 0.25).forGetter(Dunes::threshold),
                Codec.DOUBLE.optionalFieldOf("wavelength", 26.0).forGetter(Dunes::wavelength),
                Codec.DOUBLE.optionalFieldOf("height", 5.0).forGetter(Dunes::height),
                Codec.DOUBLE.optionalFieldOf("wind_deg", 20.0).forGetter(Dunes::windDeg),
                Codec.BOOL.optionalFieldOf("lowland_only", false).forGetter(Dunes::lowlandOnly),
                BlockState.CODEC.fieldOf("soil").forGetter(Dunes::soil),
                BlockState.CODEC.optionalFieldOf("alt_soil").forGetter(Dunes::altSoil),
                Codec.DOUBLE.optionalFieldOf("alt_share", 0.25).forGetter(Dunes::altShare)
        ).apply(i, Dunes::new));
    }

    /**
     * One great rift valley (like Valles Marineris) running east-west at a fixed latitude: a flat floor with
     * landslide hummocks, terraced walls cut by gullies, closed ends.
     *
     * @param latitude    latitude of the centre line (degrees, + north)
     * @param x           centre of the rift along x
     * @param length      length along x
     * @param width       full width at the rim
     * @param depth       floor depth below the plateau
     * @param meander     sideways wander of the centre line
     * @param terraces    number of benches in the walls
     * @param floorSoil   soil on the floor (landslide debris, wind-blown sand)
     */
    public record Rift(double latitude, double x, double length, double width, double depth, double meander, int terraces,
                       Optional<BlockState> floorSoil) {
        public static final Codec<Rift> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("latitude", -8.0).forGetter(Rift::latitude),
                Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Rift::x),
                Codec.DOUBLE.optionalFieldOf("length", 6000.0).forGetter(Rift::length),
                Codec.DOUBLE.optionalFieldOf("width", 170.0).forGetter(Rift::width),
                Codec.DOUBLE.optionalFieldOf("depth", 70.0).forGetter(Rift::depth),
                Codec.DOUBLE.optionalFieldOf("meander", 120.0).forGetter(Rift::meander),
                Codec.INT.optionalFieldOf("terraces", 4).forGetter(Rift::terraces),
                BlockState.CODEC.optionalFieldOf("floor_soil").forGetter(Rift::floorSoil)
        ).apply(i, Rift::new));
    }

    /** A volcano placed by hand (in addition to the random population). */
    public record Anchor(double x, double z, double radius, double height) {
        public static final Codec<Anchor> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Anchor::x),
                Codec.DOUBLE.fieldOf("z").forGetter(Anchor::z),
                Codec.DOUBLE.fieldOf("radius").forGetter(Anchor::radius),
                Codec.DOUBLE.fieldOf("height").forGetter(Anchor::height)
        ).apply(i, Anchor::new));
    }

    /**
     * Shield volcanoes: broad, gently sloping cones of lava flows with a summit caldera and radial flow ridges.
     *
     * @param cell        grid cell for the random population (at least 2.2 × max radius)
     * @param chance      chance for a cell to hold a volcano
     * @param minRadius   smallest base radius
     * @param maxRadius   largest base radius
     * @param heightRatio summit height / base radius
     * @param maxHeight   cap on the summit height
     * @param caldera     caldera radius / base radius
     * @param escarpment  height of a basal cliff ring (0 = none)
     * @param anchors     volcanoes at fixed places
     * @param rock        flank rock (lava)
     * @param soil        dust on the gentle flanks
     */
    public record Volcanoes(int cell, double chance, double minRadius, double maxRadius, double heightRatio, double maxHeight,
                            double caldera, double escarpment, List<Anchor> anchors, BlockState rock, Optional<BlockState> soil) {
        public static final Codec<Volcanoes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("cell", 3200).forGetter(Volcanoes::cell),
                Codec.DOUBLE.optionalFieldOf("chance", 0.35).forGetter(Volcanoes::chance),
                Codec.DOUBLE.optionalFieldOf("min_radius", 250.0).forGetter(Volcanoes::minRadius),
                Codec.DOUBLE.optionalFieldOf("max_radius", 700.0).forGetter(Volcanoes::maxRadius),
                Codec.DOUBLE.optionalFieldOf("height_ratio", 0.17).forGetter(Volcanoes::heightRatio),
                Codec.DOUBLE.optionalFieldOf("max_height", 170.0).forGetter(Volcanoes::maxHeight),
                Codec.DOUBLE.optionalFieldOf("caldera", 0.09).forGetter(Volcanoes::caldera),
                Codec.DOUBLE.optionalFieldOf("escarpment", 0.0).forGetter(Volcanoes::escarpment),
                Anchor.CODEC.listOf().optionalFieldOf("anchors", List.of()).forGetter(Volcanoes::anchors),
                BlockState.CODEC.fieldOf("rock").forGetter(Volcanoes::rock),
                BlockState.CODEC.optionalFieldOf("soil").forGetter(Volcanoes::soil)
        ).apply(i, Volcanoes::new));
    }

    /**
     * Ancient river valleys: a meandering, branching network cut into the highlands, ending in sediment fans (deltas)
     * where it reaches the lowland basins.
     *
     * @param scale     size of the network meshes
     * @param width     valley width (noise units, ~ 0.02-0.06)
     * @param depth     valley depth
     * @param floorSoil soil on valley floors (clays)
     * @param deltaSoil soil of the fans at the basin shores
     */
    public record Valleys(double scale, double width, double depth, BlockState floorSoil, Optional<BlockState> deltaSoil) {
        public static final Codec<Valleys> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("scale", 900.0).forGetter(Valleys::scale),
                Codec.DOUBLE.optionalFieldOf("width", 0.035).forGetter(Valleys::width),
                Codec.DOUBLE.optionalFieldOf("depth", 9.0).forGetter(Valleys::depth),
                BlockState.CODEC.fieldOf("floor_soil").forGetter(Valleys::floorSoil),
                BlockState.CODEC.optionalFieldOf("delta_soil").forGetter(Valleys::deltaSoil)
        ).apply(i, Valleys::new));
    }

    /**
     * Horizontal sedimentary layers in the highland rock, gently warped: canyon and crater walls show stripes.
     *
     * @param blocks    the layer sequence (repeats)
     * @param thickness average layer thickness
     * @param warp      vertical warping of the layers
     */
    public record Strata(List<BlockState> blocks, double thickness, double warp) {
        public static final Codec<Strata> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.listOf().fieldOf("blocks").forGetter(Strata::blocks),
                Codec.DOUBLE.optionalFieldOf("thickness", 4.0).forGetter(Strata::thickness),
                Codec.DOUBLE.optionalFieldOf("warp", 3.0).forGetter(Strata::warp)
        ).apply(i, Strata::new));
    }

    /**
     * Polar ice caps: a dome of layered water ice that thickens toward the pole, cut by curved troughs, with
     * seasonal frost (e.g. CO₂) on top near the pole.
     *
     * @param latitude      latitude where the cap starts
     * @param thickness     ice thickness at the pole
     * @param frostLatitude latitude above which the cap wears a frost layer
     * @param troughs       depth of the troughs (0 = none)
     * @param ice           cap ice
     * @param frost         frost on top
     * @param dustyIce      darker dust-rich ice layers seen in trough walls
     */
    public record IceCap(double latitude, double thickness, double frostLatitude, double troughs, BlockState ice,
                         Optional<BlockState> frost, Optional<BlockState> dustyIce) {
        public static final Codec<IceCap> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("latitude", 80.0).forGetter(IceCap::latitude),
                Codec.DOUBLE.optionalFieldOf("thickness", 28.0).forGetter(IceCap::thickness),
                Codec.DOUBLE.optionalFieldOf("frost_latitude", 86.0).forGetter(IceCap::frostLatitude),
                Codec.DOUBLE.optionalFieldOf("troughs", 8.0).forGetter(IceCap::troughs),
                BlockState.CODEC.fieldOf("ice").forGetter(IceCap::ice),
                BlockState.CODEC.optionalFieldOf("frost").forGetter(IceCap::frost),
                BlockState.CODEC.optionalFieldOf("dusty_ice").forGetter(IceCap::dustyIce)
        ).apply(i, IceCap::new));
    }

    /**
     * Patches of a different soil where a noise field is high, e.g. hematite plains or perchlorate crusts.
     *
     * @param soil      soil block
     * @param scale     patch size
     * @param threshold noise level (0..1) above which the patch shows
     * @param where     "any", "lowlands" or "highlands"
     */
    public record SoilPatch(BlockState soil, double scale, double threshold, String where) {
        public static final Codec<SoilPatch> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("soil").forGetter(SoilPatch::soil),
                Codec.DOUBLE.optionalFieldOf("scale", 300.0).forGetter(SoilPatch::scale),
                Codec.DOUBLE.optionalFieldOf("threshold", 0.6).forGetter(SoilPatch::threshold),
                Codec.STRING.optionalFieldOf("where", "any").forGetter(SoilPatch::where)
        ).apply(i, SoilPatch::new));
    }

    /**
     * Lobate scarps: long curved thrust-fault cliffs where the crust shrank, a steep front and a gentle back slope.
     *
     * @param cell   grid cell, one possible scarp per cell
     * @param chance chance for a cell to hold a scarp
     * @param height cliff height
     * @param front  width of the steep front face
     * @param back   width of the gentle back slope
     */
    public record Scarps(int cell, double chance, double height, double front, double back) {
        public static final Codec<Scarps> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("cell", 1400).forGetter(Scarps::cell),
                Codec.DOUBLE.optionalFieldOf("chance", 0.6).forGetter(Scarps::chance),
                Codec.DOUBLE.optionalFieldOf("height", 22.0).forGetter(Scarps::height),
                Codec.DOUBLE.optionalFieldOf("front", 7.0).forGetter(Scarps::front),
                Codec.DOUBLE.optionalFieldOf("back", 90.0).forGetter(Scarps::back)
        ).apply(i, Scarps::new));
    }

    /**
     * Parallel groove chains (like on Phobos): lines of linked pits.
     *
     * @param spacing distance between grooves
     * @param width   groove width
     * @param depth   groove depth
     * @param angleDeg main direction (degrees from +x)
     */
    public record Grooves(double spacing, double width, double depth, double angleDeg) {
        public static final Codec<Grooves> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("spacing", 34.0).forGetter(Grooves::spacing),
                Codec.DOUBLE.optionalFieldOf("width", 6.0).forGetter(Grooves::width),
                Codec.DOUBLE.optionalFieldOf("depth", 3.0).forGetter(Grooves::depth),
                Codec.DOUBLE.optionalFieldOf("angle_deg", 30.0).forGetter(Grooves::angleDeg)
        ).apply(i, Grooves::new));
    }

    /**
     * Tesserae: highland blocks crumpled by crossing sets of ridges and troughs (Venus).
     *
     * @param scale     size of the tessera blocks
     * @param threshold noise level above which the ground is tessera
     * @param height    ridge relief
     * @param spacing   ridge spacing
     * @param rock      the deformed rock exposed on tessera ground
     */
    public record Tesserae(double scale, double threshold, double height, double spacing, Optional<BlockState> rock) {
        public static final Codec<Tesserae> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("scale", 1600.0).forGetter(Tesserae::scale),
                Codec.DOUBLE.optionalFieldOf("threshold", 0.3).forGetter(Tesserae::threshold),
                Codec.DOUBLE.optionalFieldOf("height", 14.0).forGetter(Tesserae::height),
                Codec.DOUBLE.optionalFieldOf("spacing", 40.0).forGetter(Tesserae::spacing),
                BlockState.CODEC.optionalFieldOf("rock").forGetter(Tesserae::rock)
        ).apply(i, Tesserae::new));
    }
}
