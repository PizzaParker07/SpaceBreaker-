package com.pizzaparker07.spacebreaker.client.hud;

import java.util.Locale;

/** Short number formats for the cockpit HUD (pure, so it can be tested anywhere). */
public final class FlightFormat {
    private FlightFormat() {
    }

    /** Speed: "12.3 m/s", "845 m/s", "1.23 km/s", "11.0 km/s". */
    public static String speed(double ms) {
        double a = Math.abs(ms);
        String sign = ms < 0 ? "-" : "";
        if (a < 100) {
            return sign + String.format(Locale.ROOT, "%.1f m/s", a);
        }
        if (a < 1000) {
            return sign + String.format(Locale.ROOT, "%.0f m/s", a);
        }
        return sign + String.format(Locale.ROOT, a < 10000 ? "%.2f km/s" : "%.1f km/s", a / 1000);
    }

    /** Distance: "850 m", "12.4 km", "384,400 km"; "∞" for an escape apoapsis, "--" when unknown. */
    public static String distance(double m) {
        if (Double.isNaN(m)) {
            return "--";
        }
        if (Double.isInfinite(m)) {
            return "∞";
        }
        double a = Math.abs(m);
        String sign = m < 0 ? "-" : "";
        if (a < 1000) {
            return sign + String.format(Locale.ROOT, "%.0f m", a);
        }
        if (a < 100_000) {
            return sign + String.format(Locale.ROOT, "%.1f km", a / 1000);
        }
        return sign + String.format(Locale.ROOT, "%,.0f km", a / 1000);
    }

    /** Time to an event: "0:45", "12:03", "3:05:12". */
    public static String time(double seconds) {
        if (Double.isNaN(seconds) || seconds < 0) {
            return "--:--";
        }
        long s = Math.round(seconds);
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) : String.format(Locale.ROOT, "%d:%02d", m, sec);
    }
}
