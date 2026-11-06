package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.Vec3;

/**
 * A star as placed in a galaxy sector, before its planets are generated. Cheap to create in bulk.
 * A star is addressed by its sector and index ({@link Galaxy#star}); fixed real stars use negative indices.
 *
 * @param id        stable 64-bit id (seeds the system generator)
 * @param sx        sector x
 * @param sy        sector y
 * @param sz        sector z
 * @param index     index within the sector
 * @param position  galactocentric position, pc
 * @param mass      initial mass, M☉
 * @param ageYears  age of the star
 * @param fixedKey  key of a real star ({@link NearbyStars}), or {@code null} for a generated one
 */
public record StarSeed(long id, int sx, int sy, int sz, int index, Vec3 position, double mass, double ageYears,
                       String fixedKey) {
    public boolean fixed() {
        return fixedKey != null;
    }

    /** Distance from the Sun, pc. */
    public double distanceFromSun() {
        return position.distance(GalaxyModel.SUN);
    }
}
