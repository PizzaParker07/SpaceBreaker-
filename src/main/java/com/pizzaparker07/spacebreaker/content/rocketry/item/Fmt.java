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

    /** Between a number and its unit: a space that does not break a line ("84 s" stays together). */
    public static final String NBSP = "\u00A0";

    /** A number with {@code decimals} places and grouping, in the game's language. */
    public static String dec(double v, int decimals) {
        return local(String.format(Locale.ROOT, "%,." + decimals + "f", v));
    }

    /** A number and its unit, held together. */
    public static String unit(double v, int decimals, String unit) {
        return dec(v, decimals) + NBSP + unit;
    }

    public static String num(double v) {
        return local(String.format(Locale.ROOT, "%,.0f", v));
    }

    public static String kg(double kg) {
        if (kg >= 10_000) {
            return unit(kg / 1000.0, 1, "t");
        }
        return unit(kg, 0, "kg");
    }

    public static String ms(double v) {
        return unit(v, 0, "m/s");
    }

    public static String twr(double v) {
        return local(String.format(Locale.ROOT, "%.2f", v));
    }
}
