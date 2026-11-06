package com.pizzaparker07.spacebreaker.compat.mixin.iris;

import com.pizzaparker07.spacebreaker.compat.client.ShaderSkyPass;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shader packs draw their own volumetric clouds at Iris's {@code cloudHeight} uniform, which is this value (a NaN
 * "no clouds" is read as the default height 192). In space worlds, while a pack is active, the clouds are moved
 * far above the camera, beyond the packs' cloud draw distance. Vanilla never reads it there: our sky effects
 * draw (no) clouds themselves. Only applied when Iris is installed.
 */
@Mixin(DimensionSpecialEffects.class)
public abstract class PackCloudsMixin {
    private static final float OUT_OF_SIGHT = 25000F;

    @Inject(method = "getCloudHeight", at = @At("HEAD"), cancellable = true)
    private void spacebreaker$hidePackClouds(CallbackInfoReturnable<Float> cir) {
        if (ShaderSkyPass.hidePackClouds((DimensionSpecialEffects) (Object) this)) {
            cir.setReturnValue(OUT_OF_SIGHT);
        }
    }
}
