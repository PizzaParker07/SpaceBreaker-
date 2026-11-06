package com.pizzaparker07.spacebreaker.compat.client;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.Compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Distant Horizons' depth texture, read through its public API by reflection (DH stays optional). LOD terrain is not
 * in Minecraft's depth buffer, so our passes also test this texture: a pixel is sky only when both depths are at
 * their far value. Iris attaches the same texture to its DH framebuffers, so it holds the LOD depth with and without
 * a shader pack. Voxy has no such API: with Voxy the passes stay off in AUTO mode.
 */
public final class LodDepth {
    private static final String DH = "distanthorizons";
    private static boolean resolved;
    private static Field renderProxy;
    private static Method depthTexture;
    private static Method depthDirection;
    private static boolean warned;

    private LodDepth() {
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
            renderProxy = delayed.getField("renderProxy");
            Class<?> proxy = Class.forName("com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderProxy");
            depthTexture = proxy.getMethod("getDhDepthTextureId");
            depthDirection = proxy.getMethod("getDepthDirection");
        } catch (ReflectiveOperationException | LinkageError e) {
            renderProxy = null;
            Spacebreaker.LOGGER.warn("Distant Horizons is installed but its render API could not be reached; space skies stay off with it", e);
        }
    }

    /** True when DH is installed and its LOD depth texture can be read this frame. */
    public static boolean available() {
        return texture() > 0;
    }

    /** GL id of DH's depth texture, or 0. */
    public static int texture() {
        resolve();
        if (renderProxy == null) {
            return 0;
        }
        try {
            Object proxy = renderProxy.get(null);
            if (proxy == null) {
                return 0;
            }
            Object result = depthTexture.invoke(proxy);
            if (!result.getClass().getField("success").getBoolean(result)) {
                return 0;
            }
            Object payload = result.getClass().getField("payload").get(result);
            return payload instanceof Integer id ? id : 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                Spacebreaker.LOGGER.warn("Could not read Distant Horizons' depth texture", e);
            }
            return 0;
        }
    }

    /** Depth value of "nothing drawn" in DH's buffer (1 for forward Z, 0 for reversed Z). */
    public static float farDepth() {
        resolve();
        try {
            Object proxy = renderProxy == null ? null : renderProxy.get(null);
            if (proxy == null) {
                return 1F;
            }
            Object dir = depthDirection.invoke(proxy);
            return dir.getClass().getField("farDepth").getFloat(dir);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 1F;
        }
    }
}
