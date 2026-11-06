package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Data for the star map: a top-down picture of the whole galaxy (density and colour per cell) and markers for
 * the stars around a point, with names and apparent brightness. Pure data; the client draws it.
 */
public final class GalaxyMap {
    /** Colour of an old stellar population (bulge, inter-arm disk). */
    public static final int OLD_STARS = 0xFFD9A0;
    /** Colour of young, hot stars along the spiral arms. */
    public static final int YOUNG_STARS = 0xA8C4FF;
    /** Absolute visual magnitude of the Sun. */
    public static final double SUN_ABSOLUTE_MAGNITUDE = 4.83;

    /**
     * One star on the map.
     *
     * @param seed               the star (open its system with {@code galaxy.system(seed)})
     * @param offsetLy           position relative to the viewpoint, light years (x towards the galactic centre, z up)
     * @param distanceLy         distance from the viewpoint
     * @param nameEn             English name
     * @param nameTr             Turkish name
     * @param designation        catalogue designation (real stars: their name)
     * @param colorRgb           star colour, 0xRRGGBB
     * @param luminosity         L☉
     * @param apparentMagnitude  brightness seen from the viewpoint (smaller = brighter; naked eye ≲ 6)
     * @param summary            kind, class, mass
     */
    public record Marker(StarSeed seed, Vec3 offsetLy, double distanceLy, String nameEn, String nameTr,
                         String designation, int colorRgb, double luminosity, double apparentMagnitude,
                         StellarSummary summary) {
    }

    /**
     * A top-down picture of the galaxy.
     *
     * @param cells       cells per side
     * @param cellSizePc  edge of a cell, pc
     * @param halfSizePc  the map spans −halfSize..+halfSize in x and y
     * @param brightness  per cell [x][y], 0..1 (log-scaled surface density)
     * @param color       per cell [x][y], 0xRRGGBB (old yellow population ↔ young blue arms)
     * @param sun         the Sun's cell coordinates {x, y}
     */
    public record Overview(int cells, double cellSizePc, double halfSizePc, double[][] brightness, int[][] color, int[] sun) {
    }

    /** Absolute visual magnitude from luminosity (bolometric approximation). */
    public static double absoluteMagnitude(double luminosity) {
        return SUN_ABSOLUTE_MAGNITUDE - 2.5 * Math.log10(Math.max(luminosity, 1e-12));
    }

    /** Apparent magnitude at a distance in parsecs. */
    public static double apparentMagnitude(double luminosity, double distancePc) {
        return absoluteMagnitude(luminosity) + 5 * Math.log10(Math.max(distancePc, 1e-6)) - 5;
    }

    /**
     * Stars within {@code radiusLy} of a viewpoint, brightest first, at most {@code maxCount}.
     *
     * @param from star the view is centred on (e.g. {@code galaxy.sun()})
     */
    public static List<Marker> local(Galaxy galaxy, StarSeed from, double radiusLy, int maxCount) {
        double ly = AstroConstants.LIGHT_YEAR / AstroConstants.PARSEC; // pc per ly
        List<Marker> out = new ArrayList<>();
        for (StarSeed s : galaxy.starsWithin(from.position(), radiusLy * ly)) {
            if (s.id() == from.id()) {
                continue;
            }
            Vec3 offPc = s.position().sub(from.position());
            double dPc = offPc.length();
            StellarSummary sum = StellarSummary.of(s);
            String en, tr, des;
            if (s.fixed()) {
                NearbyStars.Entry e = NearbyStars.entry(s.fixedKey()).orElseThrow();
                en = e.nameEn();
                tr = e.nameTr();
                des = e.nameEn();
            } else {
                en = tr = StarNames.properName(s.id());
                des = StarSystemGenerator.designation(s.id());
            }
            out.add(new Marker(s, offPc.scale(1 / ly), dPc / ly, en, tr, des, sum.colorRgb(), sum.luminosity(),
                    apparentMagnitude(sum.luminosity(), dPc), sum));
        }
        out.sort(Comparator.comparingDouble(Marker::apparentMagnitude));
        return out.size() > maxCount ? List.copyOf(out.subList(0, maxCount)) : out;
    }

    /** The whole galaxy seen from above, {@code cells} × {@code cells} (e.g. 128). Same for every seed. */
    public static Overview overview(int cells) {
        double half = GalaxyModel.DISK_RADIUS + 2000;
        double cell = 2 * half / cells;
        double[][] col = new double[cells][cells];
        double[][] armShare = new double[cells][cells];
        double max = 0;
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                double x = -half + (i + 0.5) * cell, y = -half + (j + 0.5) * cell;
                // column density: integrate a few heights through the disk
                double sum = 0;
                for (double z : new double[]{0, 150, 300, 600, 1200}) {
                    sum += GalaxyModel.density(x, y, z) * (z == 0 ? 1 : 2);
                }
                col[i][j] = sum;
                max = Math.max(max, sum);
                double r = Math.hypot(x, y);
                armShare[i][j] = r < 3000 ? 0 : GalaxyModel.armFactor(x, y, r);
            }
        }
        double[][] bright = new double[cells][cells];
        int[][] color = new int[cells][cells];
        double floor = max * 1e-4;
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                bright[i][j] = Math.max(0, Math.log(Math.max(col[i][j], floor) / floor) / Math.log(max / floor));
                color[i][j] = mix(OLD_STARS, YOUNG_STARS, 0.85 * armShare[i][j]);
            }
        }
        int[] sun = {(int) ((GalaxyModel.SUN.x() + half) / cell), (int) ((GalaxyModel.SUN.y() + half) / cell)};
        return new Overview(cells, cell, half, bright, color, sun);
    }

    /** Blend of two colours, t in 0..1. */
    static int mix(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    /** A star colour for a spectral class (for labels and legends). */
    public static int classColor(StellarModel.SpectralClass c) {
        return StellarModel.blackbodyColor(Math.max(1500, c.minTemperature + 500));
    }

    private GalaxyMap() {
    }
}
