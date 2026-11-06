package com.pizzaparker07.spacebreaker.compat;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.LoadingModList;

/**
 * Optional renderer integrations (Iris shader packs, Sodium). Nothing here needs those mods at compile or run
 * time: mixins that touch rendering only apply when Iris is installed ({@link CompatMixinPlugin}) and Iris is
 * only reached through its public API by reflection. See docs/COMPAT.md.
 */
public final class Compat {
    public static final String IRIS = "iris";
    public static final String OCULUS = "oculus";
    public static final String SODIUM = "sodium";
    public static final String[] LOD_MODS = {"distanthorizons", "voxy"};

    private Compat() {
    }

    public static void init(ModContainer container) {
        if (FMLEnvironment.dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, CompatConfig.SPEC, "spacebreaker-compat-client.toml");
        }
    }

    /** True when a mod is installed; usable before mods are constructed (mixin plugins). */
    public static boolean isInstalled(String modId) {
        LoadingModList list = LoadingModList.get();
        return list != null && list.getModFileById(modId) != null;
    }

    public static boolean shaderModInstalled() {
        return isInstalled(IRIS) || isInstalled(OCULUS);
    }

    public static boolean lodModInstalled() {
        for (String id : LOD_MODS) {
            if (isInstalled(id)) {
                return true;
            }
        }
        return false;
    }
}
