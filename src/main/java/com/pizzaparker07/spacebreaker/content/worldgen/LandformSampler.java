package com.pizzaparker07.spacebreaker.content.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates the optional {@link Landforms} of a profile for one column. Pure, seeded maths like
 * {@link PlanetTerrain} (which owns one of these); safe on worldgen threads.
 */
final class LandformSampler {
    /** One shield volcano. */
    record Volcano(double x, double z, double radius, double height, double caldera, long bits) {
    }

    /** One lobate scarp: an arc of a large circle. */
    record Scarp(double cx, double cz, double arcRadius, double midAngle, double span, double height) {
    }

    private final TerrainProfile profile;
    private final Landforms lf;
    private final long seed;
    private final SimplexNoise duneField;
    private final SimplexNoise duneWarp;
    private final SimplexNoise valleyNoise;
    private final SimplexNoise valleyWarp;
    private final SimplexNoise riftNoise;
    private final SimplexNoise flowNoise;
    private final SimplexNoise capNoise;
    private final SimplexNoise strataNoise;
    private final SimplexNoise grooveNoise;
    private final SimplexNoise tessNoise;
    private final SimplexNoise[] patchNoise;
    private final List<Volcano> anchors = new ArrayList<>();
    private final ConcurrentHashMap<Long, List<Volcano>> volcanoCells = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Scarp>> scarpCells = new ConcurrentHashMap<>();

    private final RupesShape[] rupes;

    LandformSampler(TerrainProfile profile, long seed) {
        this.profile = profile;
        this.lf = profile.landforms();
        this.seed = seed;
        this.rupes = lf.rupes().stream().map(r -> new RupesShape(r, profile.poleDistance(), seed)).toArray(RupesShape[]::new);
        this.duneField = noise(31);
        this.duneWarp = noise(32);
        this.valleyNoise = noise(33);
        this.valleyWarp = noise(34);
        this.riftNoise = noise(35);
        this.flowNoise = noise(36);
        this.capNoise = noise(37);
        this.strataNoise = noise(38);
        this.grooveNoise = noise(39);
        this.tessNoise = noise(40);
        this.patchNoise = new SimplexNoise[lf.patches().size()];
        for (int i = 0; i < patchNoise.length; i++) {
            patchNoise[i] = noise(60 + i);
        }
        lf.volcanoes().ifPresent(v -> {
            int n = 0;
            for (Landforms.Anchor a : v.anchors()) {
                anchors.add(new Volcano(a.x(), a.z(), a.radius(), a.height(), v.caldera(), PlanetTerrain.mix(seed, 0xA4C0L + n++)));
            }
        });
    }

    private SimplexNoise noise(int salt) {
        return new SimplexNoise(new XoroshiroRandomSource(PlanetTerrain.mix(seed, 0x1A4DL * salt + salt)));
    }

    boolean active() {
        return !lf.equals(Landforms.NONE);
    }

    // ------------------------------------------------------------------ before craters

    /** Relief-building landforms (volcanoes, scarps, tesserae): returns the height offset. */
    double build(double x, double z, PlanetTerrain.Column out) {
        double h = 0;
        if (lf.volcanoes().isPresent()) {
            h += volcanoes(x, z, out);
        }
        if (lf.scarps().isPresent()) {
            h += scarps(x, z);
        }
        if (lf.tesserae().isPresent()) {
            h += tesserae(x, z, out);
        }
        return h;
    }

    // ------------------------------------------------------------------ after craters

    /** Erosion and deposits (rift, valleys, grooves, dunes, ice cap, soil patches): returns the new height. */
    /** The giant cliffs of the profile. */
    RupesShape[] rupes() {
        return rupes;
    }

    double finish(double x, double z, double height, PlanetTerrain.Column out) {
        // giant cliffs last, after the craters: the low side keeps its craters, only dropped
        for (int i = 0; i < rupes.length; i++) {
            if (!rupes[i].near(x, z)) {
                continue;
            }
            double[] uv = rupes[i].local(x, z);
            height += rupes[i].offset(uv);
            if (rupes[i].face(uv)) {
                out.rupes = i;
                out.rupesU = uv[0];
            }
        }
        if (lf.rift().isPresent()) {
            height -= rift(x, z, out);
        }
        if (lf.valleys().isPresent()) {
            height += valleys(x, z, out);
        }
        if (lf.grooves().isPresent()) {
            height -= grooves(x, z);
        }
        if (lf.dunes().isPresent()) {
            height += dunes(x, z, out);
        }
        if (lf.iceCap().isPresent()) {
            height += iceCap(x, z, out);
        }
        for (int i = 0; i < patchNoise.length; i++) {
            Landforms.SoilPatch p = lf.patches().get(i);
            boolean fits = switch (p.where()) {
                case "lowlands" -> out.lowland > 0.5;
                case "highlands" -> out.lowland <= 0.5;
                case "polar" -> out.cap < 1 && Math.abs(out.latitude) >= profile.poles().polarLatitude() - 10;
                default -> true;
            };
            if (fits && (fbm(patchNoise[i], x / p.scale(), z / p.scale(), 2) + 1) * 0.5 > p.threshold()) {
                out.patch = i;
                break;
            }
        }
        return height;
    }

    // ------------------------------------------------------------------ volcanoes

    private double volcanoes(double x, double z, PlanetTerrain.Column out) {
        double h = 0;
        for (Volcano v : anchors) {
            h += volcano(v, x, z, out);
        }
        Landforms.Volcanoes cfg = lf.volcanoes().get();
        int cell = cfg.cell();
        int gx = Mth.floor(x / cell);
        int gz = Mth.floor(z / cell);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (Volcano v : volcanoCell(gx + dx, gz + dz)) {
                    h += volcano(v, x, z, out);
                }
            }
        }
        return h;
    }

    /** Volcanoes of one grid cell (at most one), cached. */
    List<Volcano> volcanoCell(int gx, int gz) {
        long key = ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
        List<Volcano> cached = volcanoCells.get(key);
        if (cached != null) {
            return cached;
        }
        Landforms.Volcanoes cfg = lf.volcanoes().get();
        long h = PlanetTerrain.mix(seed ^ 0x701CA40L, PlanetTerrain.mix(gx, gz));
        List<Volcano> list = List.of();
        double cx = (gx + 0.3 + 0.4 * PlanetTerrain.unit(h, 0)) * cfg.cell();
        double cz = (gz + 0.3 + 0.4 * PlanetTerrain.unit(h, 1)) * cfg.cell();
        double lat = PlanetTerrain.latitude(cz, profile.poleDistance());
        boolean polar = lf.iceCap().map(c -> Math.abs(lat) > c.latitude() - 8).orElse(false);
        if (PlanetTerrain.unit(h, 2) < cfg.chance() && !polar) {
            double u = PlanetTerrain.unit(h, 3);
            double radius = Mth.lerp(u * u, cfg.minRadius(), cfg.maxRadius());
            double height = Math.min(cfg.maxHeight(), radius * cfg.heightRatio() * (0.8 + 0.4 * PlanetTerrain.unit(h, 4)));
            list = List.of(new Volcano(cx, cz, radius, height, cfg.caldera() * (0.8 + 0.4 * PlanetTerrain.unit(h, 5)), h));
        }
        if (volcanoCells.size() > 4096) {
            volcanoCells.clear();
        }
        volcanoCells.put(key, list);
        return list;
    }

    /** Every volcano whose base reaches the rectangle (anchors and random ones), for structures and tests. */
    List<Volcano> volcanoesNear(double x0, double z0, double x1, double z1) {
        List<Volcano> out = new ArrayList<>();
        if (lf.volcanoes().isEmpty()) {
            return out;
        }
        int cell = lf.volcanoes().get().cell();
        List<Volcano> all = new ArrayList<>(anchors);
        for (int gx = Mth.floor(x0 / cell) - 1; gx <= Mth.floor(x1 / cell) + 1; gx++) {
            for (int gz = Mth.floor(z0 / cell) - 1; gz <= Mth.floor(z1 / cell) + 1; gz++) {
                all.addAll(volcanoCell(gx, gz));
            }
        }
        for (Volcano v : all) {
            double r = v.radius() * 1.3;
            if (v.x() + r >= x0 && v.x() - r <= x1 && v.z() + r >= z0 && v.z() - r <= z1) {
                out.add(v);
            }
        }
        return out;
    }

    private double volcano(Volcano v, double x, double z, PlanetTerrain.Column out) {
        double dx = x - v.x();
        double dz = z - v.z();
        double d = Math.sqrt(dx * dx + dz * dz);
        double reach = v.radius() * 1.3;
        if (d >= reach) {
            return 0;
        }
        double a = Math.atan2(dz, dx);
        // lobate margin: lava flows reach further in some directions
        double lobes = 1 + 0.035 * Math.sin(3 * a + (v.bits() & 0xFF)) + 0.025 * Math.sin(7 * a + ((v.bits() >>> 8) & 0xFF))
                + 0.04 * flowNoise.getValue(Math.cos(a) * 3 + 17, Math.sin(a) * 3 - 9);
        double s = d / (v.radius() * lobes);
        if (s < out.volcanoS) {
            out.volcanoS = s;
            out.volcanoRadius = v.radius();
        }
        double H = v.height();
        double h;
        if (s < 1) {
            double cone = Math.pow(1 - s, 1.15);
            double dome = (1 - s * s) * (1 - s * s);
            h = H * (0.5 * cone + 0.5 * dome);
        } else {
            // thin apron of the outermost flows
            h = H * 0.02 * (1 - Crater.smooth((s - 1) / 0.3));
        }
        Landforms.Volcanoes cfg = lf.volcanoes().get();
        if (cfg.escarpment() > 0) {
            h += cfg.escarpment() * Crater.smooth((0.93 - s) / 0.03) * Math.min(1, H / 60);
        }
        // radial lava-flow ridges and channels on the flanks
        if (s > 0.12 && s < 1.15) {
            double rn = flowNoise.getValue(Math.cos(a) * 14 + v.x() * 1e-3, Math.sin(a) * 14, s * 5);
            double ridge = 1 - Math.abs(rn);
            double fadeIn = Crater.smooth((s - 0.15) / 0.3);
            h += (Math.min(5, H * 0.025) * (ridge * ridge - 0.4) * Math.sin(Math.PI * Mth.clamp(s, 0, 1)) + 1.2 * rn) * fadeIn;
            if (s <= out.volcanoS + 1e-9) {
                out.volcanoFlow = ridge * ridge * fadeIn;
            }
        }
        // summit caldera: a flat-floored pit, with a second, younger pit nested off-centre
        double c = v.caldera();
        double depth = Math.min(H * 0.14, 26);
        if (s < c * 1.4) {
            h -= depth * Crater.smooth((c * 1.05 - s) / (c * 0.3));
        }
        double oa = PlanetTerrain.unit(v.bits(), 7) * Mth.TWO_PI;
        double ox = v.x() + Math.cos(oa) * c * 0.3 * v.radius();
        double oz = v.z() + Math.sin(oa) * c * 0.3 * v.radius();
        double s2 = Math.sqrt((x - ox) * (x - ox) + (z - oz) * (z - oz)) / (c * 0.45 * v.radius());
        if (s2 < 1.3) {
            h -= depth * 0.55 * Crater.smooth((1.05 - s2) / 0.25);
        }
        return h;
    }

    // ------------------------------------------------------------------ scarps

    private double scarps(double x, double z) {
        Landforms.Scarps cfg = lf.scarps().get();
        int gx = Mth.floor(x / cfg.cell());
        int gz = Mth.floor(z / cfg.cell());
        double best = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (Scarp s : scarpCell(gx + dx, gz + dz)) {
                    best = Math.max(best, scarp(s, x, z, cfg));
                }
            }
        }
        return best;
    }

    List<Scarp> scarpCell(int gx, int gz) {
        long key = ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
        List<Scarp> cached = scarpCells.get(key);
        if (cached != null) {
            return cached;
        }
        Landforms.Scarps cfg = lf.scarps().get();
        long h = PlanetTerrain.mix(seed ^ 0x5CA4FL, PlanetTerrain.mix(gx, gz));
        List<Scarp> list = List.of();
        if (PlanetTerrain.unit(h, 0) < cfg.chance()) {
            double px = (gx + 0.2 + 0.6 * PlanetTerrain.unit(h, 1)) * cfg.cell();
            double pz = (gz + 0.2 + 0.6 * PlanetTerrain.unit(h, 2)) * cfg.cell();
            double dir = PlanetTerrain.unit(h, 3) * Mth.TWO_PI;
            double ra = cfg.cell() * (0.9 + 1.2 * PlanetTerrain.unit(h, 4));
            double cx = px + Math.cos(dir) * ra;
            double cz = pz + Math.sin(dir) * ra;
            double mid = Math.atan2(pz - cz, px - cx);
            double span = cfg.cell() * (0.35 + 0.35 * PlanetTerrain.unit(h, 5)) / ra;
            double height = cfg.height() * (0.6 + 0.6 * PlanetTerrain.unit(h, 6));
            list = List.of(new Scarp(cx, cz, ra, mid, span, height));
        }
        if (scarpCells.size() > 4096) {
            scarpCells.clear();
        }
        scarpCells.put(key, list);
        return list;
    }

    private static double scarp(Scarp s, double x, double z, Landforms.Scarps cfg) {
        double dx = x - s.cx();
        double dz = z - s.cz();
        double r = Math.sqrt(dx * dx + dz * dz);
        double d = r - s.arcRadius(); // < 0 on the uplifted (inner) side
        if (d > cfg.front() || d < -cfg.back()) {
            return 0;
        }
        double delta = Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx) - s.midAngle())) * Mth.DEG_TO_RAD;
        double along = Crater.smooth((s.span() - Math.abs(delta)) / (s.span() * 0.35));
        if (along <= 0) {
            return 0;
        }
        double profile = d > 0 ? 1 - Crater.smooth(d / cfg.front()) : 1 - Math.pow(-d / cfg.back(), 1.6);
        return s.height() * along * profile;
    }

    // ------------------------------------------------------------------ tesserae

    private double tesserae(double x, double z, PlanetTerrain.Column out) {
        Landforms.Tesserae cfg = lf.tesserae().get();
        double m = Crater.smooth((fbm(tessNoise, x / cfg.scale(), z / cfg.scale(), 2) - cfg.threshold()) / 0.12);
        if (m <= 0) {
            return 0;
        }
        out.tessera = m;
        double w = tessNoise.getValue(x / 300.0 + 51, z / 300.0 - 17) * 0.8;
        double p1 = (x * 0.94 + z * 0.34) / cfg.spacing() + w;
        double p2 = (-x * 0.42 + z * 0.91) / (cfg.spacing() * 1.7) - w;
        // rounded crests (a raised cosine, smooth at the top) so the ridges step down in bands, not to one-block spines
        double r1 = 0.5 + 0.5 * Math.cos(p1 * Mth.TWO_PI);
        double r2 = 0.5 + 0.5 * Math.cos(p2 * Mth.TWO_PI);
        return cfg.height() * m * (0.35 + 0.45 * r1 * r1 * r1 + 0.3 * r2 * r2 * r2);
    }

    // ------------------------------------------------------------------ rift

    /** Centre line z of the rift at x. */
    double riftCentre(double x) {
        Landforms.Rift r = lf.rift().get();
        double z0 = -r.latitude() / 90 * profile.poleDistance();
        return z0 + r.meander() * fbm(riftNoise, x / 1800.0, 0.5, 2);
    }

    private double rift(double x, double z, PlanetTerrain.Column out) {
        Landforms.Rift r = lf.rift().get();
        double along = Math.abs(x - r.x()) / (r.length() / 2);
        if (along >= 1) {
            return 0;
        }
        double taper = Crater.smooth((1 - along) / 0.18);
        double half = r.width() / 2 * (0.78 + 0.32 * riftNoise.getValue(x / 900.0, 3.3)) * (0.3 + 0.7 * taper);
        // spurs and gullies along the walls
        half *= 1 + 0.1 * riftNoise.getValue(x / 46.0, z / 46.0 + 70) + 0.02 * riftNoise.getValue(x / 17.0, z / 17.0 - 40);
        double u = Math.abs(z - riftCentre(x)) / half;
        if (u >= 1) {
            return 0;
        }
        out.rift = u;
        double depth = r.depth() * (0.35 + 0.65 * taper) * (0.88 + 0.12 * riftNoise.getValue(x / 500.0, 9.1));
        double floor = 0.42;
        if (u < floor) {
            // landslide hummocks, thickest against the walls
            double hum = Math.max(0, riftNoise.getValue(x / 24.0, z / 24.0)) * 4 * Crater.smooth((u - 0.15) / 0.25);
            return depth - hum;
        }
        double t = (u - floor) / (1 - floor); // 0 at the foot of the wall, 1 at the rim
        int n = Math.max(1, r.terraces());
        double q = t * n;
        double k = Math.floor(q);
        double f = q - k;
        double stepped = (k + Crater.smooth(f * 1.7 - 0.35)) / n;
        return depth * (1 - stepped);
    }

    // ------------------------------------------------------------------ valleys

    private double valleys(double x, double z, PlanetTerrain.Column out) {
        Landforms.Valleys cfg = lf.valleys().get();
        double ws = 1.0 / cfg.scale();
        double wx = x + 140 * valleyWarp.getValue(x / 420.0, z / 420.0);
        double wz = z + 140 * valleyWarp.getValue(x / 420.0 + 91, z / 420.0 - 33);
        double n = fbm(valleyNoise, wx * ws, wz * ws, 3);
        double n2 = valleyNoise.getValue(wx * ws * 2.6 + 50, wz * ws * 2.6 - 50);
        double v = 1 - Math.abs(n) / cfg.width();
        double v2 = 1 - Math.abs(n2) / (cfg.width() * 0.8);
        double lat = Math.abs(out.latitude);
        double region = Crater.smooth((valleyWarp.getValue(x / (cfg.scale() * 2.5) - 300, z / (cfg.scale() * 2.5) + 300) + 0.15) / 0.35);
        double fade = region * (1 - Crater.smooth((out.lowland - 0.3) / 0.25)) * (1 - Crater.smooth((lat - profile.poles().polarLatitude() + 12) / 8));
        if (out.volcanoS < 1.1) {
            fade *= Crater.smooth((out.volcanoS - 0.9) / 0.2);
        }
        if (out.rift < 1) {
            fade *= Crater.smooth((out.rift - 0.85) / 0.15);
        }
        double h = 0;
        double main = v > 0 ? Crater.smooth(v * 1.6) : 0;
        double trib = v2 > 0 ? Crater.smooth(v2 * 1.6) : 0;
        if (fade > 0 && (main > 0 || trib > 0)) {
            h -= cfg.depth() * fade * Math.max(main, 0.45 * trib);
            out.valley = fade * Math.max(main, 0.8 * trib);
        }
        // sediment fans where the main valleys reach the basin shores
        double wide = (1 - Math.abs(n) / (cfg.width() * 5)) * region;
        if (wide > 0 && out.lowland > 0.25 && out.lowland < 0.8) {
            double shore = Math.sin(Math.PI * (out.lowland - 0.25) / 0.55);
            double fan = Crater.smooth(wide) * shore;
            out.delta = fan;
            h += 1.8 * fan;
        }
        return h;
    }

    // ------------------------------------------------------------------ grooves

    private double grooves(double x, double z) {
        Landforms.Grooves cfg = lf.grooves().get();
        double theta = Math.toRadians(cfg.angleDeg() + 25 * grooveNoise.getValue(x / 2200.0, z / 2200.0));
        double across = -x * Math.sin(theta) + z * Math.cos(theta);
        double along = x * Math.cos(theta) + z * Math.sin(theta);
        double g = across / cfg.spacing() + 0.25 * grooveNoise.getValue(along / 400.0, 7.7);
        double frac = g - Math.floor(g);
        double dist = Math.abs(frac - 0.5) * cfg.spacing();
        double half = cfg.width() / 2;
        if (dist >= half) {
            return 0;
        }
        double family = Crater.smooth((grooveNoise.getValue(x / 650.0 + 13, z / 650.0) + 0.25) / 0.4);
        double pits = 0.5 + 0.5 * Math.abs(Math.sin(along / (cfg.width() * 1.3)));
        return cfg.depth() * Crater.smooth(1 - dist / half) * pits * family;
    }

    // ------------------------------------------------------------------ dunes

    private double dunes(double x, double z, PlanetTerrain.Column out) {
        Landforms.Dunes cfg = lf.dunes().get();
        double f = fbm(duneField, x / cfg.scale(), z / cfg.scale(), 2);
        double m = Crater.smooth((f - cfg.threshold()) / 0.14);
        if (cfg.lowlandOnly()) {
            double low = Crater.smooth((out.lowland - 0.45) / 0.25);
            double craterFloor = out.anyCraterRadius > 14 && out.anyCraterS < 0.6 ? 1 : 0;
            double riftFloor = out.rift < 0.4 ? 1 : 0;
            m *= Math.max(low, Math.max(craterFloor, riftFloor));
        }
        m *= 1 - Crater.smooth((Math.abs(out.latitude) - profile.poles().polarLatitude() + 6) / 6);
        if (out.volcanoS < 1.2 || out.valley > 0.3) {
            m = 0;
        }
        if (m <= 0.02) {
            return 0;
        }
        double theta = Math.toRadians(cfg.windDeg());
        double along = x * Math.cos(theta) + z * Math.sin(theta);
        double across = -x * Math.sin(theta) + z * Math.cos(theta);
        double lambda = cfg.wavelength();
        double phase = along / lambda + 0.7 * duneWarp.getValue(across / (lambda * 5), along / (lambda * 9));
        double fr = phase - Math.floor(phase);
        double prof = fr < 0.72 ? fr / 0.72 : (1 - fr) / 0.28; // gentle windward slope, steep slip face
        prof = prof * prof * (3 - 2 * prof);
        double amp = cfg.height() * m * (0.55 + 0.45 * duneWarp.getValue(x / 140.0, z / 140.0 + 99));
        double h = Math.max(0, amp) * prof + 0.8 * m;
        out.dune = m;
        out.duneHeight = h;
        out.altDune = cfg.altSoil().isPresent() && fbm(duneField, x / cfg.scale() * 0.6 + 40, z / cfg.scale() * 0.6 - 40, 2)
                < -1 + 2 * cfg.altShare();
        return h;
    }

    // ------------------------------------------------------------------ ice cap

    private double iceCap(double x, double z, PlanetTerrain.Column out) {
        Landforms.IceCap cfg = lf.iceCap().get();
        double lat = Math.abs(out.latitude);
        double edge = cfg.latitude() + 1.6 * capNoise.getValue(x / 260.0, z / 260.0) + 0.8 * capNoise.getValue(x / 60.0, z / 60.0);
        if (lat < edge - 0.5) {
            return 0;
        }
        double k = Crater.smooth((lat - edge) / ((90 - cfg.latitude()) * 0.5));
        double t = cfg.thickness() * Math.pow(k, 0.7) * (0.88 + 0.12 * capNoise.getValue(x / 400.0 + 20, z / 400.0));
        t = Math.max(t, 1.2 * Crater.smooth((lat - edge + 0.5) / 0.5)); // thin ice right at the margin
        // curved troughs cut into the dome
        double band = Math.sin(Math.abs(z) / 150.0 + 2.4 * capNoise.getValue(x / 900.0, z / 900.0 + 7));
        double trough = cfg.troughs() * Crater.smooth((band - 0.82) / 0.12) * k;
        out.trough = trough;
        t = Math.max(0, t - trough);
        out.cap = t;
        out.frost = lat > cfg.frostLatitude() + capNoise.getValue(x / 120.0, z / 120.0 - 5);
        return t;
    }

    // ------------------------------------------------------------------ strata

    /** Rock of the layer sequence at height y in column (x, z), or null without strata. */
    BlockState stratum(int x, int y, int z) {
        if (lf.strata().isEmpty()) {
            return null;
        }
        Landforms.Strata s = lf.strata().get();
        double w = s.warp() * strataNoise.getValue(x / 110.0, z / 110.0) + 0.6 * s.warp() * strataNoise.getValue(x / 31.0, z / 31.0);
        int k = Mth.floor((y + w) / s.thickness());
        return s.blocks().get(Math.floorMod(k, s.blocks().size()));
    }

    static double fbm(SimplexNoise n, double x, double z, int octaves) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += n.getValue(x * f + i * 17.17, z * f - i * 9.31) * amp;
            norm += amp;
            amp *= 0.5;
            f *= 2.03;
        }
        return sum / norm;
    }
}
