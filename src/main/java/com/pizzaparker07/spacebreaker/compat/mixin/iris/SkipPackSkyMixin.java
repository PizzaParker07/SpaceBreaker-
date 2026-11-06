package com.pizzaparker07.spacebreaker.compat.mixin.iris;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pizzaparker07.spacebreaker.compat.client.ShaderSkyPass;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While our sky replaces a shader pack's sky ({@link ShaderSkyPass}), drawing the same sky inside the pack's world pass
 * is wasted work: the pack paints over it, then our pass draws it again on top of the final frame. The effects'
 * renderSky call is answered with "handled" instead, so LevelRenderer.renderSky returns normally (Iris's own hooks
 * on that method still run). Only applied when Iris is installed; with the sky replacement off nothing changes.
 */
@Mixin(LevelRenderer.class)
public abstract class SkipPackSkyMixin {
    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;renderSky(Lnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Matrix4f;Lnet/minecraft/client/Camera;Lorg/joml/Matrix4f;ZLjava/lang/Runnable;)Z"))
    private boolean spacebreaker$skipSkyUnderPack(DimensionSpecialEffects effects, ClientLevel level, int ticks, float partialTick, Matrix4f modelView,
                                                 Camera camera, Matrix4f projection, boolean foggy, Runnable setupFog, Operation<Boolean> original) {
        if (ShaderSkyPass.active()) {
            return true;
        }
        return original.call(effects, level, ticks, partialTick, modelView, camera, projection, foggy, setupFog);
    }
}
