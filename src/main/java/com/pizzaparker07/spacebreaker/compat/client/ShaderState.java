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
    private static MethodHandle packName;

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
            try {
                // internal, only used to pick per-pack light tuning; optional
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                packName = lookup.findStatic(iris, "getCurrentPackName", MethodType.methodType(String.class));
            } catch (Throwable ignored) {
                packName = null;
            }
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

    /** File name of the active shader pack in lower case, or "" if unknown. */
    public static String packName() {
        resolve();
        if (packName == null) {
            return "";
        }
        try {
            String name = (String) packName.invoke();
            return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
        } catch (Throwable t) {
            return "";
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

    private static boolean depthResolved;
    private static java.lang.reflect.Method pipelineManager;
    private static java.lang.reflect.Method pipeline;
    private static java.lang.reflect.Field renderTargets;
    private static java.lang.reflect.Method noTranslucents;
    private static java.lang.reflect.Method textureId;

    /**
     * GL id of Iris's depth copy taken just before the translucent pass (the pack's depthtex1: terrain and entities,
     * no glass or water), or 0 when it cannot be read. Iris internals, read by reflection: any change there only
     * turns the feature off.
     */
    public static int opaqueDepthTexture() {
        if (!depthResolved) {
            depthResolved = true;
            try {
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                pipelineManager = iris.getMethod("getPipelineManager");
                pipeline = pipelineManager.getReturnType().getMethod("getPipelineNullable");
                Class<?> irisPipeline = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
                renderTargets = irisPipeline.getDeclaredField("renderTargets");
                renderTargets.setAccessible(true);
                noTranslucents = renderTargets.getType().getMethod("getDepthTextureNoTranslucents");
                textureId = noTranslucents.getReturnType().getMethod("getTextureId");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                pipelineManager = null;
            }
        }
        if (pipelineManager == null) {
            return 0;
        }
        try {
            Object p = pipeline.invoke(pipelineManager.invoke(null));
            if (p == null || !renderTargets.getDeclaringClass().isInstance(p)) {
                return 0;
            }
            Object targets = renderTargets.get(p);
            Object depth = targets == null ? null : noTranslucents.invoke(targets);
            return depth == null ? 0 : (Integer) textureId.invoke(depth);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }
}
