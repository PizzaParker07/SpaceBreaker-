package com.pizzaparker07.spacebreaker.content.suit;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Upgrade modules fitted to a suit piece at a smithing table (Suit Upgrade Template + piece + module).
 * One module per piece; fitting another replaces it.
 */
public enum SuitUpgrade implements StringRepresentable {
    /** Wider temperature range (any piece, stacks). */
    THERMAL_REGULATOR("thermal_regulator", null),
    /** Less radiation (any piece, stacks). */
    RADIATION_SHIELD("radiation_shield", null),
    /** A third oxygen tank slot in the backpack (torso). */
    EXTRA_OXYGEN("extra_oxygen", EquipmentSlot.CHEST),
    /** Stronger, more efficient Maneuvering Unit thrust (torso). */
    JETPACK_BOOSTER("jetpack_booster", EquipmentSlot.CHEST);

    public static final Codec<SuitUpgrade> CODEC = StringRepresentable.fromEnum(SuitUpgrade::values);
    public static final StreamCodec<ByteBuf, SuitUpgrade> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    private final String name;
    @Nullable
    private final EquipmentSlot onlySlot;

    SuitUpgrade(String name, @Nullable EquipmentSlot onlySlot) {
        this.name = name;
        this.onlySlot = onlySlot;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean fits(EquipmentSlot slot) {
        return onlySlot == null || onlySlot == slot;
    }

    public String descriptionKey() {
        return "upgrade.spacebreaker." + name;
    }

    @Nullable
    public static SuitUpgrade of(ItemStack stack) {
        return stack.get(SuitModule.SUIT_UPGRADE.get());
    }
}
