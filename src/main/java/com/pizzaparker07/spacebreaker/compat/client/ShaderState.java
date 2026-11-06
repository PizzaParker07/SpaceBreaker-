package com.pizzaparker07.spacebreaker.compat.client;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.Compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Reads Iris's state through its public, stable API ({@code net.irisshaders.iris.api.v0.IrisApi}) by reflection, so
 * Iris stays an optional dependency. Every failure means "no shader pack".
 */
public final class ShaderState {
    private static boolean resolved;
    private static Object api;
    private static MethodHandle inUse;
    private static MethodHandle shadowPass;

    private ShaderState() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!Compat.shaderModInstalled()) {
            return;
        }
        try {
            Class<?> cls = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            api = lookup.findStatic(cls, "getInstance", MethodType.methodType(cls)).invoke();
            inUse = lookup.findVirtual(cls, "isShaderPackInUse", MethodType.methodType(boolean.class));
            shadowPass = lookup.findVirtual(cls, "isRenderingShadowPass", MethodType.methodType(boolean.class));
            Spacebreaker.LOGGER.info("Iris detected: space skies are drawn over shader packs (see spacebreaker-compat-client.toml)");
        } catch (Throwable t) {
            api = null;
            Spacebreaker.LOGGER.warn("Iris is installed but its API could not be reached; shader-pack sky fixes are off", t);
        }
    }

    /** True while an Iris shader pack is active. */
    public static boolean shaderPackInUse() {
        resolve();
        if (api == null) {
            return false;
        }
        try {
            return (boolean) inUse.invoke(api);
        } catch (Throwable t) {
            api = null;
            return false;
        }
    }

    /** True while Iris renders its shadow map (our sky helpers must not draw then). */
    public static boolean renderingShadowPass() {
        resolve();
        if (api == null) {
            return false;
        }
        try {
            return (boolean) shadowPass.invoke(api);
        } catch (Throwable t) {
            return false;
        }
    }
}
