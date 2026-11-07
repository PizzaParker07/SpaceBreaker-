package com.pizzaparker07.spacebreaker.compat.mixin.iris;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pizzaparker07.spacebreaker.compat.client.PackTime;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Iris's worldTime uniform in orbit (packs fade day and night lighting with it): see {@link PackTime}. Only applied
 * when Iris is installed.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.WorldTimeUniforms", remap = false)
public abstract class PackWorldTimeMixin {
    @WrapOperation(method = "getWorldDayTime", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getDayTime()J"))
    private static long spacebreaker$orbitTime(ClientLevel level, Operation<Long> original) {
        long time = PackTime.packDayTime(level);
        return time < 0 ? original.call(level) : time;
    }
}
