package com.pizzaparker07.spacebreaker.content.rocketry.item;

import java.util.Locale;

/** Number formatting for tooltips and screens (no locale-dependent grouping surprises). */
public final class Fmt {
    private Fmt() {
    }

    /**
     * Whether numbers are written the Turkish way ("6.449", "1,25"). The client sets this from the selected
     * language; on a server it stays off.
     */
    public static volatile java.util.function.BooleanSupplier localDots = () -> false;

    private static String local(String s) {
        if (!localDots.getAsBoolean()) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            b.append(c == ',' ? '.' : c == '.' ? ',' : c);
        }
        return b.toString();
    }

    public static String num(double v) {
        return local(String.format(Locale.ROOT, "%,.0f", v));
    }

    public static String kg(double kg) {
        if (kg >= 10_000) {
            return local(String.format(Locale.ROOT, "%.1f t", kg / 1000.0));
        }
        return local(String.format(Locale.ROOT, "%,.0f kg", kg));
    }

    public static String ms(double v) {
        return local(String.format(Locale.ROOT, "%,.0f m/s", v));
    }

    public static String twr(double v) {
        return local(String.format(Locale.ROOT, "%.2f", v));
    }
}
