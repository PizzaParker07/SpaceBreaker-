package com.pizzaparker07.spacebreaker.content.moon;

import net.minecraft.util.StringRepresentable;

/** Boot print left in loose soil. Left and right alternate with the block position so walks leave a natural trail. */
public enum Footprint implements StringRepresentable {
    NONE("none"),
    LEFT("left"),
    RIGHT("right");

    private final String name;

    Footprint(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
