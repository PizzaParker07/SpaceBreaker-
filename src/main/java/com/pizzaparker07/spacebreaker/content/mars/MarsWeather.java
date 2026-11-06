package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Martian dust storms. Each dusty world keeps its own storm state (saved with the level): long calm spells, then
 * a storm that builds up over a minute or two, rages for about a sol and dies down again. The strength (0..1) is
 * synced to clients for fog, sky tint and blowing dust; the server uses it for sunlight, wind-erased boot prints and
 * creatures that only come out in storms.
 */
public final class MarsWeather {
    /** Average calm time between storms, in ticks (about five sols). */
    public static final int MEAN_CALM_TICKS = 5 * 24000;
    private static final Map<ResourceKey<Level>, Float> CLIENT = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Float> CLIENT_SHOWN = new ConcurrentHashMap<>();

    /** Server → client: storm strength of one dimension. */
    public record Payload(ResourceKey<Level> dimension, float strength) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Spacebreaker.id("dust_storm"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION), Payload::dimension,
                ByteBufCodecs.FLOAT, Payload::strength,
                Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private MarsWeather() {
    }

    /** True for worlds that have dust storms. */
    public static boolean dusty(Level level) {
        return level.dimension() == MarsModule.MARS;
    }

    /** Current storm strength 0..1 (client: smoothed synced value). */
    public static float stormStrength(Level level) {
        if (!dusty(level)) {
            return 0F;
        }
        if (level.isClientSide) {
            return CLIENT_SHOWN.getOrDefault(level.dimension(), 0F);
        }
        return data((ServerLevel) level).strength;
    }

    /** Starts (strength > 0) or ends (0) a storm now; used by the command and the showcase. */
    public static void force(ServerLevel level, float strength, int durationTicks) {
        Data d = data(level);
        d.target = Mth.clamp(strength, 0F, 1F);
        d.remaining = strength > 0 ? durationTicks : 0;
        d.strength = d.target;
        d.setDirty();
        sync(level, d);
    }

    static void tick(ServerLevel level) {
        if (!dusty(level)) {
            return;
        }
        Data d = data(level);
        if (d.remaining > 0) {
            d.remaining--;
            if (d.remaining == 0) {
                d.target = 0;
            }
        } else if (d.target == 0 && level.random.nextInt(MEAN_CALM_TICKS) == 0) {
            d.target = 0.55F + 0.45F * level.random.nextFloat();
            d.remaining = 14000 + level.random.nextInt(24000);
        }
        float before = d.strength;
        d.strength += Mth.clamp(d.target - d.strength, -1F / 2400F, 1F / 1600F);
        if (Math.abs(d.strength - d.synced) > 0.02F || (d.strength != d.synced && level.getGameTime() % 100 == 0)) {
            sync(level, d);
        }
        if (before != d.strength && level.getGameTime() % 200 == 0) {
            d.setDirty();
        }
    }

    private static void sync(ServerLevel level, Data d) {
        d.synced = d.strength;
        PacketDistributor.sendToPlayersInDimension(level, new Payload(level.dimension(), d.strength));
    }

    static void syncTo(ServerPlayer player) {
        if (dusty(player.level())) {
            PacketDistributor.sendToPlayer(player, new Payload(player.level().dimension(), data(player.serverLevel()).strength));
        }
    }

    static void receive(Payload payload) {
        CLIENT.put(payload.dimension(), payload.strength());
    }

    /** Client tick: ease the shown strength toward the synced one. */
    public static void clientTick(Level level) {
        if (!dusty(level)) {
            return;
        }
        float target = CLIENT.getOrDefault(level.dimension(), 0F);
        float shown = CLIENT_SHOWN.getOrDefault(level.dimension(), target);
        CLIENT_SHOWN.put(level.dimension(), shown + Mth.clamp(target - shown, -0.01F, 0.01F));
    }

    public static void clearClient() {
        CLIENT.clear();
        CLIENT_SHOWN.clear();
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), "spacebreaker_dust_storm");
    }

    static final class Data extends SavedData {
        float strength;
        float target;
        int remaining;
        float synced = -1;

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            d.strength = tag.getFloat("strength");
            d.target = tag.getFloat("target");
            d.remaining = tag.getInt("remaining");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putFloat("strength", strength);
            tag.putFloat("target", target);
            tag.putInt("remaining", remaining);
            return tag;
        }
    }
}
