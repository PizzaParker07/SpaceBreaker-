package com.pizzaparker07.spacebreaker.content.rocketry.ship;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: save a spacecraft design on a shipyard console the player stands at. */
public record ShipDesignPayload(BlockPos console, ShipDesign design) implements CustomPacketPayload {
    public static final Type<ShipDesignPayload> TYPE = new Type<>(Spacebreaker.id("ship_design"));
    public static final StreamCodec<ByteBuf, ShipDesignPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShipDesignPayload::console,
            ShipDesign.STREAM_CODEC, ShipDesignPayload::design,
            ShipDesignPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShipDesignPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.distanceToSqr(p.console().getCenter()) > 64 || !(player.level().getBlockEntity(p.console()) instanceof ShipyardConsoleBlockEntity be)) {
                return; // only at the console
            }
            be.setDesign(p.design());
        });
    }
}
