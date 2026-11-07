package com.pizzaparker07.spacebreaker.compat.mixin.iris;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pizzaparker07.spacebreaker.compat.client.PackTime;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Iris's sun and moon angle (sunAngle, sunPosition, shadow light) in orbit: see {@link PackTime}. Only applied when
 * Iris is installed.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CelestialUniforms", remap = false)
public abstract class PackSunMixin {
    @WrapOperation(method = "getSkyAngle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private static float spacebreaker$orbitSun(ClientLevel level, float partialTick, Operation<Float> original) {
        long time = PackTime.packDayTime(level);
        return time < 0 ? original.call(level, partialTick) : PackTime.timeOfDay(time);
    }
}
