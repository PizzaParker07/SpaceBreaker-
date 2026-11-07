package com.pizzaparker07.spacebreaker.core.astro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Loads the shipped celestial_body JSON files into a {@link BodySystem}, mirroring the game's codec defaults. */
final class SolarSystemData {
    static final Path DIR = Path.of("src/main/resources/data/spacebreaker/spacebreaker/celestial_body");
    private static BodySystem cached;

    static synchronized BodySystem system() {
        if (cached == null) {
            List<AstroBody> list = new ArrayList<>();
            for (Path p : files()) {
                list.add(load(p));
            }
            cached = new BodySystem(list);
        }
        return cached;
    }

    static List<Path> files() {
        try (Stream<Path> s = Files.list(DIR)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> json(Path p) {
        try {
            return (Map<String, Object>) MiniJson.parse(Files.readString(p, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String id(Path p) {
        String f = p.getFileName().toString();
        return "spacebreaker:" + f.substring(0, f.length() - 5);
    }

    static AstroBody body(String name) {
        return system().require("spacebreaker:" + name);
    }

    @SuppressWarnings("unchecked")
    static AstroBody load(Path p) {
        Map<String, Object> j = json(p);
        Map<String, Object> phys = (Map<String, Object>) j.get("physical");
        Map<String, Object> orbit = (Map<String, Object>) j.getOrDefault("orbit", Map.of());
        Map<String, Object> rot = (Map<String, Object>) j.get("rotation");
        Map<String, Object> atm = (Map<String, Object>) j.getOrDefault("atmosphere", Map.of());
        Map<String, Object> clim = (Map<String, Object>) j.get("climate");
        Map<String, Double> comp = (Map<String, Double>) (Map<?, ?>) atm.getOrDefault("composition", Map.of());
        AstroBody b = AstroBody.fromDataUnits(id(p), (String) j.get("parent"), AstroBody.Kind.byName((String) j.get("type")),
                d(phys, "mass_kg", 0), d(phys, "radius_km", 0),
                d(orbit, "semi_major_axis_km", 0), d(orbit, "eccentricity", 0), d(orbit, "inclination_deg", 0),
                d(orbit, "longitude_ascending_node_deg", 0), d(orbit, "argument_periapsis_deg", 0),
                d(orbit, "mean_anomaly_deg", 0), d(orbit, "period_days", 0),
                d(rot, "day_length_hours", 0), Boolean.TRUE.equals(rot.get("tidally_locked")),
                d(atm, "pressure_kpa", 0), comp, d(clim, "day_temp_c", 0), d(clim, "night_temp_c", 0),
                d(j, "albedo", 0.3), j.containsKey("dimension"), d(j, "geometric_albedo", -1));
        double node = d(orbit, "node_rate_deg_per_year", 0), arg = d(orbit, "periapsis_rate_deg_per_year", 0);
        return b.orbit() != null && (node != 0 || arg != 0) ? b.withOrbit(b.orbit().withSecularRates(node, arg)) : b;
    }

    private static double d(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        return v == null ? def : (Double) v;
    }

    private SolarSystemData() {
    }
}
