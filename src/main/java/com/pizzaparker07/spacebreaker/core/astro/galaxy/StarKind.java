package com.pizzaparker07.spacebreaker.core.astro.galaxy;

public enum StarKind {
    MAIN_SEQUENCE, WHITE_DWARF, NEUTRON_STAR, BLACK_HOLE;

    public boolean hasPlanets() {
        return this == MAIN_SEQUENCE || this == WHITE_DWARF;
    }
}
