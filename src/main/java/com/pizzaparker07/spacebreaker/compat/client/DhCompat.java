package com.pizzaparker07.spacebreaker.compat.client;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * What Distant Horizons is drawing, through its public API by reflection (DH stays optional; every query answers
 * "no DH" when it is missing or its API cannot be reached). For effects that hand the ground over to something else
 * high up, such as the planet ball of a seamless ascent: with DH the real (LOD) ground reaches much further than the
 * vanilla view distance, so the hand-over should follow {@link #lodReachBlocks()} and {@link #lodUnder(double, double)}
 * instead of the vanilla view distance.
 * <p>
 * Results are cached for a few ticks; the terrain query asks DH's data store for one column.
 */
public final class DhCompat {
    private static final String DH = "distanthorizons";
    private static boolean resolved;
    private static Field configs;
    private static Field terrainRepo;
    private static Field worldProxy;
    private static Method graphics;
    private static Method chunkRenderDistance;
    private static Method renderingEnabled;
    private static Method curvature;
    private static Method configValue;
    private static Method worldLoaded;
    private static Method singlePlayerLevel;
    private static Method columnData;
    private static boolean warned;

    private static long cachedTick = Long.MIN_VALUE;
    private static int cachedReach;
    private static int cachedCurvature;
    private static long columnTick = Long.MIN_VALUE;
    private static int columnX = Integer.MIN_VALUE, columnZ = Integer.MIN_VALUE;
    private static boolean columnFound;

    private DhCompat() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!Compat.isInstalled(DH)) {
            return;
        }
        try {
            Class<?> delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
            configs = delayed.getField("configs");
            terrainRepo = delayed.getField("terrainRepo");
            worldProxy = delayed.getField("worldProxy");
            graphics = Class.forName("com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig").getMethod("graphics");
            Class<?> g = Class.forName("com.seibel.distanthorizons.api.interfaces.config.client.IDhApiGraphicsConfig");
            chunkRenderDistance = g.getMethod("chunkRenderDistance");
            renderingEnabled = g.getMethod("renderingEnabled");
            curvature = g.getMethod("earthCurvatureRatio");
            configValue = Class.forName("com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue").getMethod("getValue");
            Class<?> world = Class.forName("com.seibel.distanthorizons.api.interfaces.world.IDhApiWorldProxy");
            worldLoaded = world.getMethod("worldLoaded");
            singlePlayerLevel = world.getMethod("getSinglePlayerLevel");
            Class<?> repo = Class.forName("com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo");
            Class<?> level = Class.forName("com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper");
            Class<?> cache = Class.forName("com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache");
            columnData = repo.getMethod("getColumnDataAtBlockPos", level, int.class, int.class, cache);
        } catch (ReflectiveOperationException | LinkageError e) {
            configs = null;
            Spacebreaker.LOGGER.warn("Distant Horizons is installed but its config/terrain API could not be reached", e);
        }
    }

    /** Whether DH is installed, its API reachable and its rendering switched on. */
    public static boolean active() {
        refresh();
        return cachedReach > 0;
    }

    /** How far DH draws LOD terrain around the camera, in blocks; 0 without DH or with its rendering off. */
    public static int lodReachBlocks() {
        refresh();
        return cachedReach;
    }

    /**
     * DH's earth curvature setting (0 = flat; otherwise LOD terrain drops away as on a ball this many times smaller
     * than the real Earth, DH's own look). Effects that draw their own curved horizon may want to match or note it.
     */
    public static int curvatureRatio() {
        refresh();
        return cachedCurvature;
    }

    /**
     * Whether DH holds LOD terrain for the column at (x, z) in the current world, i.e. whether LOD ground is drawn
     * there once it is inside {@link #lodReachBlocks()}. Asked at most once a second for the same column.
     */
    public static boolean lodUnder(double x, double z) {
        if (!active()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        long tick = mc.level == null ? 0 : mc.level.getGameTime();
        int bx = Mth.floor(x), bz = Mth.floor(z);
        if (tick - columnTick < 20 && Math.abs(bx - columnX) < 16 && Math.abs(bz - columnZ) < 16) {
            return columnFound;
        }
        columnTick = tick;
        columnX = bx;
        columnZ = bz;
        columnFound = false;
        try {
            Object proxy = worldProxy.get(null);
            Object repo = terrainRepo.get(null);
            if (proxy == null || repo == null || !(Boolean) worldLoaded.invoke(proxy)) {
                return false;
            }
            Object level = singlePlayerLevel.invoke(proxy);
            if (level == null) {
                return false;
            }
            Object result = columnData.invoke(repo, level, bx, bz, null);
            if (!result.getClass().getField("success").getBoolean(result)) {
                return false;
            }
            Object points = result.getClass().getField("payload").get(result);
            columnFound = points instanceof Object[] array && array.length > 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
        return columnFound;
    }

    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        long tick = mc.level == null ? 0 : mc.level.getGameTime();
        if (tick == cachedTick) {
            return;
        }
        cachedTick = tick;
        cachedReach = 0;
        cachedCurvature = 0;
        resolve();
        if (configs == null) {
            return;
        }
        try {
            Object config = configs.get(null);
            if (config == null) {
                return;
            }
            Object g = graphics.invoke(config);
            if (!Boolean.TRUE.equals(configValue.invoke(renderingEnabled.invoke(g)))) {
                return;
            }
            Object chunks = configValue.invoke(chunkRenderDistance.invoke(g));
            cachedReach = chunks instanceof Integer c ? Math.max(0, c) * 16 : 0;
            Object ratio = configValue.invoke(curvature.invoke(g));
            cachedCurvature = ratio instanceof Integer r ? r : 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
    }

    private static void warn(Exception e) {
        if (!warned) {
            warned = true;
            Spacebreaker.LOGGER.warn("Could not read Distant Horizons' state", e);
        }
    }
}
