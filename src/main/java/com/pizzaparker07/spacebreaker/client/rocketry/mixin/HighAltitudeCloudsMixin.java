package com.pizzaparker07.spacebreaker.client.rocketry.mixin;

import com.pizzaparker07.spacebreaker.client.rocketry.RocketClientEvents;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * High above the cloud layer the terrain under it is out of render distance, so the vanilla clouds would hang as
 * loose squares over the Earth drawn in the sky. Past that height they are left out (NaN means "no clouds" to the
 * vanilla renderer); the cloud layer of that Earth takes their place.
 */
@Mixin(DimensionSpecialEffects.class)
public abstract class HighAltitudeCloudsMixin {
    @Inject(method = "getCloudHeight", at = @At("HEAD"), cancellable = true)
    private void spacebreaker$hideCloudsFromAbove(CallbackInfoReturnable<Float> cir) {
        if (RocketClientEvents.cloudsBelowView((DimensionSpecialEffects) (Object) this)) {
            cir.setReturnValue(Float.NaN);
        }
    }
}
