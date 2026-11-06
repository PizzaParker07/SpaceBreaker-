package com.pizzaparker07.spacebreaker.compat.mixin.iris;

import com.pizzaparker07.spacebreaker.compat.client.ShaderSkyPass;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shader packs derive their daylight ambient (sky fill) light from Iris's {@code skyColor} uniform, which is this
 * value. Airless worlds have a black biome sky, so the ground was lit by the pack's warm, atmosphere-filtered sun
 * alone and the grey Moon turned sepia. While a pack is active the uniform gets a dim, cool fill instead (see
 * {@link ShaderSkyPass#packSkyColor}). Our own sky never reads it. Only applied when Iris is installed.
 */
@Mixin(ClientLevel.class)
public abstract class PackAmbientMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void spacebreaker$packAmbient(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 color = ShaderSkyPass.packSkyColor((ClientLevel) (Object) this);
        if (color != null) {
            cir.setReturnValue(color);
        }
    }
}
