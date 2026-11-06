package com.pizzaparker07.spacebreaker.compat.mixin;

import com.pizzaparker07.spacebreaker.compat.SpaceWorlds;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla gives every dimension the Overworld's weather, so when it rains on Earth the client's Moon level also
 * reports rain. Our sky effects hide it, but shader packs read the rain strength directly (wet, darkened ground,
 * rain fog on the Moon). On the client, space worlds without vanilla weather report none. Server-side weather is
 * untouched.
 */
@Mixin(Level.class)
public abstract class ClientWeatherMixin {
    @Unique
    private SpaceWorlds.Kind spacebreaker$kind;

    @Unique
    private boolean spacebreaker$noWeather() {
        Level self = (Level) (Object) this;
        if (!self.isClientSide()) {
            return false;
        }
        if (spacebreaker$kind == null) {
            spacebreaker$kind = SpaceWorlds.classify(self);
        }
        return !spacebreaker$kind.vanillaWeather();
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void spacebreaker$noRain(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (spacebreaker$noWeather()) {
            cir.setReturnValue(0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void spacebreaker$noThunder(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (spacebreaker$noWeather()) {
            cir.setReturnValue(0F);
        }
    }
}
