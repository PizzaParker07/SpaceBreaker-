package com.pizzaparker07.spacebreaker.content.rocketry.item;

import java.util.Locale;

/** Number formatting for tooltips and screens (no locale-dependent grouping surprises). */
public final class Fmt {
    private Fmt() {
    }

    public static String num(double v) {
        return String.format(Locale.ROOT, "%,.0f", v);
    }

    public static String kg(double kg) {
        if (kg >= 10_000) {
            return String.format(Locale.ROOT, "%.1f t", kg / 1000.0);
        }
        return String.format(Locale.ROOT, "%,.0f kg", kg);
    }

    public static String ms(double v) {
        return String.format(Locale.ROOT, "%,.0f m/s", v);
    }

    public static String twr(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
