package com.pizzaparker07.spacebreaker.core.astro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Builds a Δv map of a star system as Markdown tables (used for docs/ASTRO.md and the guide). */
public final class DeltaVMap {

    public static String markdown(BodySystem sys, String originId) {
        StringBuilder sb = new StringBuilder();
        AstroBody origin = sys.require(originId);
        List<AstroBody> bodies = ordered(sys);

        sb.append("#### Surface and orbit\n\n");
        sb.append("| Body | g (m/s²) | v_esc (km/s) | Low orbit (km) | Surface → orbit | Orbit → surface | Orbit → escape |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|\n");
        for (AstroBody b : bodies) {
            if (b.isStar()) {
                continue;
            }
            String up = b.landable() ? km(TransferPlanner.ascent(b)) : "—";
            String down = b.landable() ? km(TransferPlanner.descent(b, true)) + (b.hasAtmosphere() ? " ¹" : "") : "—";
            sb.append(String.format(Locale.ROOT, "| %s | %.2f | %.2f | %.0f | %s | %s | %s |\n", name(b), b.surfaceGravity(),
                    b.escapeVelocity() / 1000, (b.lowOrbitRadius() - b.radius()) / 1000, up, down, km(TransferPlanner.escape(b))));
        }
        sb.append("\n¹ with aerobraking and parachutes; a fully powered landing costs about the same as the ascent.\n\n");

        sb.append("#### From low ").append(name(origin)).append(" orbit\n\n");
        sb.append("| Target | Departure | Capture | Capture (aerobraking) | Total | Transfer | Window every | Phase angle |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (AstroBody b : bodies) {
            if (b.isStar() || b == origin) {
                continue;
            }
            TransferPlanner.Plan p;
            TransferPlanner.Plan aero;
            try {
                p = TransferPlanner.plan(sys, originId, b.id(), false);
                aero = TransferPlanner.plan(sys, originId, b.id(), true);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String aeroCap = b.hasAtmosphere() ? km(aero.deltaV("capture")) : "—";
            sb.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %s | %s | %s | %.0f° |\n", name(b),
                    km(p.deltaV("departure")), km(p.deltaV("capture")), aeroCap, km(p.total()), duration(p.transferTime()),
                    duration(p.synodicPeriod()), Math.toDegrees(p.phaseAngle())));
        }

        sb.append("\n#### Moons from their planet's low orbit\n\n");
        sb.append("| Planet → moon | Departure | Capture | Total | Transfer |\n");
        sb.append("|---|---:|---:|---:|---:|\n");
        for (AstroBody b : bodies) {
            AstroBody parent = sys.parent(b).orElse(null);
            if (parent == null || parent.isStar() || b == origin && parent == null) {
                continue;
            }
            TransferPlanner.Plan p = TransferPlanner.plan(sys, parent.id(), b.id(), false);
            sb.append(String.format(Locale.ROOT, "| %s → %s | %s | %s | %s | %s |\n", name(parent), name(b),
                    km(p.deltaV("departure")), km(p.deltaV("capture")), km(p.total()), duration(p.transferTime())));
        }
        return sb.toString();
    }

    /** Planets by distance, each followed by its moons. */
    private static List<AstroBody> ordered(BodySystem sys) {
        List<AstroBody> out = new ArrayList<>();
        for (AstroBody root : sys.all()) {
            if (root.parent() == null) {
                add(sys, root, out);
            }
        }
        return out;
    }

    private static void add(BodySystem sys, AstroBody b, List<AstroBody> out) {
        out.add(b);
        List<AstroBody> kids = new ArrayList<>(sys.children(b.id()));
        kids.sort(Comparator.comparingDouble(k -> k.orbit() == null ? 0 : k.orbit().semiMajorAxis()));
        for (AstroBody k : kids) {
            add(sys, k, out);
        }
    }

    private static String name(AstroBody b) {
        String n = b.id().substring(b.id().indexOf(':') + 1);
        n = n.substring(n.lastIndexOf('/') + 1).replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static String km(double ms) {
        return String.format(Locale.ROOT, "%.2f", ms / 1000);
    }

    /** Human-friendly duration: hours, days or years. */
    public static String duration(double s) {
        if (!Double.isFinite(s)) {
            return "—";
        }
        double days = s / AstroConstants.DAY;
        if (days < 2) {
            return String.format(Locale.ROOT, "%.1f h", s / 3600);
        }
        if (days < 1000) {
            return String.format(Locale.ROOT, "%.0f d", days);
        }
        return String.format(Locale.ROOT, "%.1f y", s / AstroConstants.YEAR);
    }

    private DeltaVMap() {
    }
}
