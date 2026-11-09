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
    private static Method setApiValue;
    private static Method clearApiValue;
    private static boolean hiding;
    private static Method fog;
    private static Method enableDhFog;
    private static boolean hidingFog;
    private static boolean fogWarned;
    private static Method worldLoaded;
    private static Method singlePlayerLevel;
    private static Method columnData;
    private static boolean warned;

    private static long cachedTick = Long.MIN_VALUE;
    private static long cachedAt;
    private static Object cachedLevel;
    private static boolean reported;
    private static int cachedReach;
    private static int cachedCurvature;
    private static long columnAt;
    private static Object columnLevel;
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
            graphics = Class.forName("com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig").getMethod("graphics");
            Class<?> g = Class.forName("com.seibel.distanthorizons.api.interfaces.config.client.IDhApiGraphicsConfig");
            chunkRenderDistance = g.getMethod("chunkRenderDistance");
            renderingEnabled = g.getMethod("renderingEnabled");
            Class<?> value = Class.forName("com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue");
            configValue = value.getMethod("getValue");
            setApiValue = value.getMethod("setValue", Object.class);
            clearApiValue = value.getMethod("clearValue");
        } catch (ReflectiveOperationException | LinkageError e) {
            configs = null;
            Spacebreaker.LOGGER.warn("Distant Horizons is installed but its config API could not be reached", e);
            return;
        }
        // The rest is optional: each part that a DH version lacks only switches off what needs it.
        try {
            curvature = Class.forName("com.seibel.distanthorizons.api.interfaces.config.client.IDhApiGraphicsConfig")
                    .getMethod("earthCurvatureRatio");
        } catch (ReflectiveOperationException | LinkageError e) {
            curvature = null;
            Spacebreaker.LOGGER.warn("Distant Horizons' earth curvature setting could not be reached", e);
        }
        try {
            fog = Class.forName("com.seibel.distanthorizons.api.interfaces.config.client.IDhApiGraphicsConfig").getMethod("fog");
            enableDhFog = Class.forName("com.seibel.distanthorizons.api.interfaces.config.client.IDhApiFogConfig").getMethod("enableDhFog");
        } catch (ReflectiveOperationException | LinkageError e) {
            fog = null;
            enableDhFog = null;
            Spacebreaker.LOGGER.warn("Distant Horizons' fog settings could not be reached", e);
        }
        try {
            Class<?> delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
            terrainRepo = delayed.getField("terrainRepo");
            worldProxy = delayed.getField("worldProxy");
            Class<?> world = Class.forName("com.seibel.distanthorizons.api.interfaces.world.IDhApiWorldProxy");
            worldLoaded = world.getMethod("worldLoaded");
            singlePlayerLevel = world.getMethod("getSinglePlayerLevel");
            Class<?> repo = Class.forName("com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo");
            Class<?> level = Class.forName("com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper");
            Class<?> cache = Class.forName("com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache");
            columnData = repo.getMethod("getColumnDataAtBlockPos", level, int.class, int.class, cache);
        } catch (ReflectiveOperationException | LinkageError e) {
            columnData = null;
            Spacebreaker.LOGGER.warn("Distant Horizons' terrain data API could not be reached", e);
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
        if (!active() || columnData == null) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        int bx = Mth.floor(x), bz = Mth.floor(z);
        if (now - columnAt < 1_000_000_000L && mc.level == columnLevel
                && Math.abs(bx - columnX) < 16 && Math.abs(bz - columnZ) < 16) {
            return columnFound;
        }
        columnAt = now;
        columnLevel = mc.level;
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

    /**
     * Stops DH drawing its LOD terrain while {@code hide} is true (an API override of its "rendering enabled" setting:
     * the player's own setting is untouched and comes back with {@code false}). For a view that covers the whole
     * ground with something else, such as the planet ball high in a seamless ascent: LOD terrain is drawn after it,
     * wherever the depth is still the far value, so its far edge would show around the ball. Cheap to call every
     * frame; only a change is passed on to DH.
     */
    public static void hideLods(boolean hide) {
        if (hide == hiding) {
            return;
        }
        resolve();
        if (configs == null) {
            hiding = false;
            return;
        }
        try {
            Object config = configs.get(null);
            if (config == null) {
                return;
            }
            Object setting = renderingEnabled.invoke(graphics.invoke(config));
            if (hide) {
                if (!Boolean.TRUE.equals(setApiValue.invoke(setting, Boolean.FALSE))) {
                    if (!warned) {
                        warned = true;
                        Spacebreaker.LOGGER.warn("Distant Horizons did not accept an API override of its rendering setting; LODs stay on");
                    }
                    return;
                }
            } else {
                clearApiValue.invoke(setting);
            }
            hiding = hide;
            cachedTick = Long.MIN_VALUE;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
    }

    /** Whether {@link #hideLods} is holding DH's LOD terrain back right now. */
    public static boolean hidingLods() {
        return hiding;
    }

    /**
     * Stops DH drawing its own fog (far fog and height fog together: an API override of its "enable DH fog" setting,
     * the player's setting untouched and back with {@code false}). For a flight that climbs far above the ground, where
     * DH's fog, tuned for standing on it, would wash the LOD terrain and the sky behind it. Vanilla's fog and DH's
     * "disable vanilla fog" setting are left alone. Cheap to call every frame; only a change is passed on to DH.
     */
    public static void hideFog(boolean hide) {
        if (hide == hidingFog) {
            return;
        }
        resolve();
        if (configs == null || enableDhFog == null) {
            hidingFog = false;
            return;
        }
        try {
            Object config = configs.get(null);
            if (config == null) {
                return;
            }
            Object setting = enableDhFog.invoke(fog.invoke(graphics.invoke(config)));
            if (hide) {
                if (!Boolean.TRUE.equals(setApiValue.invoke(setting, Boolean.FALSE))) {
                    if (!fogWarned) {
                        fogWarned = true;
                        Spacebreaker.LOGGER.warn("Distant Horizons did not accept an API override of its fog setting; its fog stays on");
                    }
                    return;
                }
            } else {
                clearApiValue.invoke(setting);
            }
            hidingFog = hide;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
    }

    /** Whether {@link #hideFog} is holding DH's fog back right now. */
    public static boolean hidingFog() {
        return hidingFog;
    }

    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        long tick = mc.level == null ? 0 : mc.level.getGameTime();
        long now = System.nanoTime();
        // Once per game tick, but also again after a quarter second and on a world change: DH's API objects appear only
        // after its own start-up, and the game time can stand still (a frozen tick rate, the same time in a new world).
        if (tick == cachedTick && mc.level == cachedLevel && now - cachedAt < 250_000_000L) {
            return;
        }
        cachedTick = tick;
        cachedAt = now;
        cachedLevel = mc.level;
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
            if (!hiding && !Boolean.TRUE.equals(configValue.invoke(renderingEnabled.invoke(g)))) {
                return;
            }
            Object chunks = configValue.invoke(chunkRenderDistance.invoke(g));
            cachedReach = chunks instanceof Integer c ? Math.max(0, c) * 16 : 0;
            if (!reported && mc.level != null) {
                reported = true;
                Spacebreaker.LOGGER.info("Distant Horizons seen: LOD reach {} blocks, fog switch {}, terrain data {}",
                        cachedReach, enableDhFog != null ? "reachable" : "missing", columnData != null ? "reachable" : "missing");
            }
            if (curvature != null) {
                Object ratio = configValue.invoke(curvature.invoke(g));
                cachedCurvature = ratio instanceof Integer r ? r : 0;
            }
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
