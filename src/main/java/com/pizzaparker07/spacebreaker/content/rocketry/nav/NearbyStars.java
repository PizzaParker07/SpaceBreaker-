package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.core.astro.galaxy.Galaxy;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.GalaxyMap;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * The stars around the Sun for the star map's "nearby stars" view. The galaxy depends on the world seed, which
 * clients do not know (and should not), so the server works the list out once and sends it on request.
 */
public final class NearbyStars {
    /** How far out the map shows stars, light years. */
    public static final double RADIUS_LY = 25;
    public static final int MAX_STARS = 60;

    /** One star as the map shows it. */
    public record Star(String nameEn, String nameTr, String designation, float x, float y, float z, float distanceLy,
                       int colorRgb, String kind, String spectralClass, float mass, float luminosity, int planets,
                       float magnitude) {
        public static final StreamCodec<ByteBuf, Star> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, s.nameEn);
            ByteBufCodecs.STRING_UTF8.encode(buf, s.nameTr);
            ByteBufCodecs.STRING_UTF8.encode(buf, s.designation);
            buf.writeFloat(s.x).writeFloat(s.y).writeFloat(s.z).writeFloat(s.distanceLy);
            buf.writeInt(s.colorRgb);
            ByteBufCodecs.STRING_UTF8.encode(buf, s.kind);
            ByteBufCodecs.STRING_UTF8.encode(buf, s.spectralClass);
            buf.writeFloat(s.mass).writeFloat(s.luminosity);
            ByteBufCodecs.VAR_INT.encode(buf, s.planets);
            buf.writeFloat(s.magnitude);
        }, buf -> new Star(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), buf.readFloat(), buf.readFloat(),
                ByteBufCodecs.VAR_INT.decode(buf), buf.readFloat()));
    }

    private static volatile List<Star> server;
    private static volatile long serverSeed;
    /** What this client last received (empty until asked). */
    private static volatile List<Star> received = List.of();

    private NearbyStars() {
    }

    /** The stars around the Sun for this galaxy, brightest first. */
    public static List<Star> compute(Galaxy galaxy) {
        List<Star> out = new ArrayList<>();
        for (GalaxyMap.Marker m : GalaxyMap.local(galaxy, galaxy.sun(), RADIUS_LY, MAX_STARS)) {
            int planets = m.summary().kind().hasPlanets() ? galaxy.system(m.seed()).map(StarSystem::planets).map(List::size).orElse(0) : 0;
            out.add(new Star(m.nameEn(), m.nameTr(), m.designation(), (float) m.offsetLy().x(), (float) m.offsetLy().y(), (float) m.offsetLy().z(),
                    (float) m.distanceLy(), m.colorRgb(), m.summary().kind().name(), m.summary().spectralClass().name(),
                    (float) m.summary().mass(), (float) m.summary().luminosity(), planets, (float) m.apparentMagnitude()));
        }
        return List.copyOf(out);
    }

    /** The list for this server's world, worked out once. */
    public static List<Star> forServer(MinecraftServer srv) {
        long seed = srv.overworld().getSeed();
        if (server == null || serverSeed != seed) {
            server = compute(new Galaxy(seed));
            serverSeed = seed;
        }
        return server;
    }

    public static List<Star> received() {
        return received;
    }

    public static void receive(List<Star> stars) {
        received = List.copyOf(stars);
    }
}
