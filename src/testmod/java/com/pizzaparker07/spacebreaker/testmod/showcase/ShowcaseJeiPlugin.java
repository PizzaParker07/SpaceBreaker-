package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/** Showcase helper (dev only, loaded only with JEI): keeps the JEI runtime so scenario shots can open its screens. */
@JeiPlugin
public final class ShowcaseJeiPlugin implements IModPlugin {
    static volatile IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return Spacebreaker.id("showcase_jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }
}
