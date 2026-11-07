package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server → client: one reading of the Environment Sensor. {@code bodyKey} is the body's translation key (empty
 * when unknown), {@code ground} the translation key of the block underfoot, {@code dustRate} &lt; 0 when there is
 * no dust model for this world.
 */
public record SensorReadingPayload(String bodyKey, float pressureKpa, float temperatureC, boolean sunlit, float doseRateMsvDay,
                                   float gravity, Map<String, Double> composition, String ground, float dustRate)
        implements CustomPacketPayload {
    public static final Type<SensorReadingPayload> TYPE = new Type<>(Spacebreaker.id("sensor_reading"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SensorReadingPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.bodyKey);
                buf.writeFloat(p.pressureKpa);
                buf.writeFloat(p.temperatureC);
                buf.writeBoolean(p.sunlit);
                buf.writeFloat(p.doseRateMsvDay);
                buf.writeFloat(p.gravity);
                buf.writeVarInt(p.composition.size());
                p.composition.forEach((k, v) -> {
                    buf.writeUtf(k);
                    buf.writeDouble(v);
                });
                buf.writeUtf(p.ground);
                buf.writeFloat(p.dustRate);
            },
            buf -> {
                String body = buf.readUtf();
                float pressure = buf.readFloat();
                float temp = buf.readFloat();
                boolean sun = buf.readBoolean();
                float dose = buf.readFloat();
                float g = buf.readFloat();
                int n = Math.min(16, buf.readVarInt());
                Map<String, Double> comp = new LinkedHashMap<>();
                for (int i = 0; i < n; i++) {
                    comp.put(buf.readUtf(), buf.readDouble());
                }
                return new SensorReadingPayload(body, pressure, temp, sun, dose, g, comp, buf.readUtf(), buf.readFloat());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
