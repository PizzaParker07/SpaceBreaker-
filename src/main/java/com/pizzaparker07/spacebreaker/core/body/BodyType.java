package com.pizzaparker07.spacebreaker.core.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum BodyType implements StringRepresentable {
    STAR("star"),
    TERRESTRIAL("terrestrial"),
    GAS_GIANT("gas_giant"),
    ICE_GIANT("ice_giant"),
    DWARF("dwarf"),
    MOON("moon"),
    ASTEROID("asteroid"),
    STATION("station");

    public static final Codec<BodyType> CODEC = StringRepresentable.fromEnum(BodyType::values);
    private final String name;

    BodyType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
