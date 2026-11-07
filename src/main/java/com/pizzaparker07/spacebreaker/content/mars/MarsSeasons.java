package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Martian seasons and the seasonal CO₂ frost. Mars is tilted like Earth (25°), so its hemispheres take turns at
 * winter. In the winter hemisphere a quarter of the atmosphere freezes out as carbon-dioxide frost: the polar cap
 * spreads far toward the equator, and on clear nights thin frost coats the ground beyond it; in spring the frost turns
 * straight back to gas. The real Martian year is 669 sols; here it is compressed to {@link #YEAR_SOLS} so the seasons
 * come round in play.
 * <p>
 * Frost is placed and removed column by column around each player, a few columns per tick, so only loaded ground near
 * players is ever touched.
 */
public final class MarsSeasons {
    /** Sols in one (compressed) Martian year. */
    public static final int YEAR_SOLS = 24;
    /** Latitude of the cap edge in summer (the residual cap) and how far it spreads in deepest winter. */
    public static final double SUMMER_EDGE = 82;
    public static final double WINTER_SPREAD = 30;
    /** How much farther toward the equator night frost reaches beyond the cap edge, degrees. */
    public static final double NIGHT_REACH = 12;
    /** Ground columns checked per player per tick. */
    public static final int COLUMNS_PER_TICK = 4;
    /** Radius around players in which frost forms and sublimates, blocks. */
    public static final int RADIUS = 64;

    private MarsSeasons() {
    }

    // ------------------------------------------------------------------ the calendar

    /** Sols since the start of the clock (fractional), including any season set by command. */
    public static double sol(ServerLevel level) {
        long dayTicks = PlanetTime.clock(level).map(PlanetTime.Clock::dayTicks).orElse(24000L);
        long offset = PlanetTime.clock(level).map(PlanetTime.Clock::offset).orElse(0L);
        return (level.getDayTime() + offset) / (double) dayTicks + data(level).offsetSols;
    }

    /** Areocentric solar longitude Ls in degrees: 0 northern spring, 90 northern summer, 180 autumn, 270 winter. */
    public static double ls(ServerLevel level) {
        return 360.0 * Mth.frac(sol(level) / YEAR_SOLS);
    }

    /** How deep in winter a hemisphere is at this Ls: 0 (no winter) to 1 (solstice). */
    public static double winter(double ls, boolean north) {
        double s = Math.sin(Math.toRadians(ls));
        return Math.max(0, north ? -s : s);
    }

    /** Latitude (absolute degrees) of the seasonal cap's edge in a hemisphere. */
    public static double capEdge(double ls, boolean north) {
        return SUMMER_EDGE - WINTER_SPREAD * winter(ls, north);
    }

    /** Whether frost belongs on open ground at this latitude (positive north). */
    public static boolean wantsFrost(double latitude, double ls, boolean night) {
        double edge = capEdge(ls, latitude >= 0);
        double a = Math.abs(latitude);
        return a >= edge || (night && a >= edge - NIGHT_REACH);
    }

    /** Season index 0..3 (northern spring, summer, autumn, winter) for announcements. */
    public static int season(double ls) {
        return Mth.floor(Mth.positiveModulo(ls, 360) / 90) & 3;
    }

    /** Sets the season so that Ls is now {@code ls} (degrees). */
    public static void setLs(ServerLevel level, double ls) {
        Data d = data(level);
        double now = ls(level);
        d.offsetSols += (Mth.positiveModulo(ls - now, 360)) / 360.0 * YEAR_SOLS;
        d.lastSeason = season(ls);
        d.setDirty();
    }

    // ------------------------------------------------------------------ frost

    static void tick(ServerLevel level) {
        if (level.dimension() != MarsModule.MARS || level.players().isEmpty()) {
            return;
        }
        double ls = ls(level);
        announce(level, ls);
        boolean night = Environment.sunElevation(level) < 0;
        RandomSource r = level.random;
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < COLUMNS_PER_TICK; i++) {
                int x = Mth.floor(player.getX()) + r.nextInt(2 * RADIUS + 1) - RADIUS;
                int z = Mth.floor(player.getZ()) + r.nextInt(2 * RADIUS + 1) - RADIUS;
                if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                    continue;
                }
                double lat = MarsClimate.latitude(level, new BlockPos(x, 0, z));
                updateColumn(level, x, z, wantsFrost(lat, ls, night));
            }
        }
    }

    /**
     * Lays frost on the open ground of one column, or sublimates it away.
     *
     * @return 1 if frost formed, -1 if it sublimated, 0 if nothing changed
     */
    public static int updateColumn(ServerLevel level, int x, int z, boolean frost) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos top = new BlockPos(x, y - 1, z);
        BlockState state = level.getBlockState(top);
        BlockState layer = MarsModule.CO2_FROST_LAYER.get().defaultBlockState();
        if (state.is(MarsModule.CO2_FROST_LAYER.get())) {
            if (!frost) {
                level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
                return -1;
            }
            return 0;
        }
        BlockPos above = top.above();
        if (frost && state.isFaceSturdy(level, top, net.minecraft.core.Direction.UP) && !state.hasBlockEntity()
                && level.getBlockState(above).isAir() && level.canSeeSky(above)) {
            level.setBlock(above, layer, 3);
            return 1;
        }
        return 0;
    }

    private static void announce(ServerLevel level, double ls) {
        Data d = data(level);
        int s = season(ls);
        if (d.lastSeason == s) {
            return;
        }
        boolean first = d.lastSeason < 0;
        d.lastSeason = s;
        d.setDirty();
        if (first) {
            return;
        }
        Component msg = Component.translatable("message.spacebreaker.mars_season." + s);
        level.players().forEach(p -> p.displayClientMessage(msg, false));
    }

    // ------------------------------------------------------------------ saved state

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), "spacebreaker_mars_season");
    }

    static final class Data extends SavedData {
        double offsetSols;
        int lastSeason = -1;

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            d.offsetSols = tag.getDouble("offset_sols");
            d.lastSeason = tag.contains("last_season") ? tag.getInt("last_season") : -1;
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putDouble("offset_sols", offsetSols);
            tag.putInt("last_season", lastSeason);
            return tag;
        }
    }
}
