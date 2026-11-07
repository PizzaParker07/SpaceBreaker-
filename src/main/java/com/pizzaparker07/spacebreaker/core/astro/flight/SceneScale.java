package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.Vec3;

/**
 * Maps real distances around a scene's central body to blocks and back. Physics runs in real metres; only drawing and
 * block positions are compressed. Inside the body the scale is linear; outside, it is logarithmic:
 * b(d) = Rb · (1 + k · ln(d / R)) for d > R, so low orbits stay roomy and far moons still fit in loaded terrain.
 * Directions are kept, so the mapping is radial and invertible.
 *
 * @param centralRadius   radius of the central body, m
 * @param radiusBlocks    its drawn radius, blocks
 * @param compression     k: blocks per e-fold of distance, in units of radiusBlocks
 */
public record SceneScale(double centralRadius, double radiusBlocks, double compression) {
    /** The Earth–Moon scene of the first release: Earth 128 blocks, the Moon ≈ 2 200 blocks away. */
    public static final SceneScale EARTH_MOON = new SceneScale(6.371e6, 128, 4);

    public SceneScale {
        if (centralRadius <= 0 || radiusBlocks <= 0 || compression <= 0) throw new IllegalArgumentException("scale must be positive");
    }

    /** Distance from the centre in blocks. */
    public double blocks(double metres) {
        double x = metres / centralRadius;
        return x <= 1 ? radiusBlocks * x : radiusBlocks * (1 + compression * Math.log(x));
    }

    /** Inverse of {@link #blocks}. */
    public double metres(double blocks) {
        double y = blocks / radiusBlocks;
        return y <= 1 ? centralRadius * y : centralRadius * Math.exp((y - 1) / compression);
    }

    public Vec3 toBlocks(Vec3 metres) {
        double d = metres.length();
        return d == 0 ? Vec3.ZERO : metres.scale(blocks(d) / d);
    }

    public Vec3 toMetres(Vec3 blocks) {
        double b = blocks.length();
        return b == 0 ? Vec3.ZERO : blocks.scale(metres(b) / b);
    }

    /** Local radial scale at a distance, blocks per metre (for speeds and small offsets). */
    public double blocksPerMetre(double metres) {
        return metres <= centralRadius ? radiusBlocks / centralRadius : radiusBlocks * compression / metres;
    }

    /**
     * Drawn radius of another body at {@code distance} from the centre: its real size at the local scale, but never
     * below {@code minBlocks}, so a distant moon is still a visible, landable target.
     */
    public double displayRadius(double bodyRadius, double distance, double minBlocks) {
        return Math.max(minBlocks, bodyRadius * blocksPerMetre(distance));
    }
}
