package com.pizzaparker07.spacebreaker.content.moon;

import net.minecraft.util.StringRepresentable;

/**
 * Print left in loose soil. Left and right boot prints alternate with the block position so walks leave a natural
 * trail; {@link #TRACK} is a rover wheel rut along the driving direction.
 */
public enum Footprint implements StringRepresentable {
    NONE("none"),
    LEFT("left"),
    RIGHT("right"),
    TRACK("track");

    private final String name;

    Footprint(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
