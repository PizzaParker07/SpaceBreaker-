package com.pizzaparker07.spacebreaker.content.machines.net;

import net.minecraft.util.StringRepresentable;

public enum ConduitSide implements StringRepresentable {
    NONE("none"), CONNECTED("connected"), EXTRACT("extract");

    private final String name;

    ConduitSide(String name) {
        this.name = name;
    }

    public boolean connected() {
        return this != NONE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
