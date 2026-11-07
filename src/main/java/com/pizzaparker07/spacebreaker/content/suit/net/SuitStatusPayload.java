package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: everything the helmet HUD shows, sent a few times per second to the wearer.
 *
 * @param oxygen    breathing ticks left in the suit's tanks
 * @param capacity  total capacity of the slotted tanks
 * @param breath    held breath left (0..300)
 * @param flags     bit 0 sealed, bit 1 breathable air, bit 2 hazards enabled, bit 3 maneuvering unit fitted,
 *                  bit 4 jetpack booster fitted
 * @param ratingMinC/ratingMaxC temperature range of the sealed suit (0/0 when unsealed)
 * @param mmuFuel   maneuvering unit gas, thrust ticks
 * @param oxygenRate running average of the breathing rate (tank ticks per tick, 1 = walking)
 */
public record SuitStatusPayload(int oxygen, int capacity, int breath, float bodyTempC, float ambientC, float doseMsv,
                                float doseRate, float pressureKpa, float gravity, int mmuFuel, int integrity, int flags,
                                int ratingMinC, int ratingMaxC, int shieldingPercent, float oxygenRate)
        implements CustomPacketPayload {
    public static final int SEALED = 1, BREATHABLE = 2, HAZARDS = 4, MMU = 8, BOOSTER = 16, MAGNETISED = 32, TETHERED = 64, PRESSURISED = 128, COLD_TRAP = 256, MICRO_G = 512;
    public static final Type<SuitStatusPayload> TYPE = new Type<>(Spacebreaker.id("suit_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SuitStatusPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.oxygen);
                buf.writeVarInt(p.capacity);
                buf.writeVarInt(p.breath);
                buf.writeFloat(p.bodyTempC);
                buf.writeFloat(p.ambientC);
                buf.writeFloat(p.doseMsv);
                buf.writeFloat(p.doseRate);
                buf.writeFloat(p.pressureKpa);
                buf.writeFloat(p.gravity);
                buf.writeVarInt(p.mmuFuel);
                buf.writeVarInt(p.integrity);
                buf.writeVarInt(p.flags);
                buf.writeInt(p.ratingMinC);
                buf.writeInt(p.ratingMaxC);
                buf.writeVarInt(p.shieldingPercent);
                buf.writeFloat(p.oxygenRate);
            },
            buf -> new SuitStatusPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readInt(), buf.readInt(), buf.readVarInt(), buf.readFloat()));

    /**
     * How much rime the visor rim should carry (0..1): full when the cold is beyond the suit's rating, a light
     * touch in a cold trap the suit copes with, none when unsealed or inside a pressurised module.
     */
    public float visorFrost() {
        if (!has(SEALED) || has(PRESSURISED)) {
            return 0F;
        }
        if (ambientC() < ratingMinC()) {
            return 1F;
        }
        return has(COLD_TRAP) ? 0.3F : 0F;
    }

    /** Game ticks of breathing left at the current average rate. */
    public int oxygenTicksLeft() {
        return com.pizzaparker07.spacebreaker.content.suit.OxygenUse.ticksLeft(oxygen, oxygenRate);
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
