package com.pizzaparker07.spacebreaker.testmod.qa;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Small helpers: encode registry values with their codec and walk the JSON for ids. */
final class QaScan {
    private QaScan() {
    }

    @SuppressWarnings("unchecked")
    static JsonElement encode(Codec<?> codec, Object value, DynamicOps<JsonElement> ops) {
        try {
            return ((Codec<Object>) codec).encodeStart(ops, value).result().orElse(JsonNull.INSTANCE);
        } catch (RuntimeException e) {
            return JsonNull.INSTANCE;
        }
    }

    /** Every string value in the tree (object keys excluded). */
    static void strings(JsonElement e, Consumer<String> out) {
        if (e == null || e.isJsonNull()) {
            return;
        }
        if (e.isJsonPrimitive()) {
            if (e.getAsJsonPrimitive().isString()) {
                out.accept(e.getAsString());
            }
        } else if (e.isJsonArray()) {
            for (JsonElement c : e.getAsJsonArray()) {
                strings(c, out);
            }
        } else if (e.isJsonObject()) {
            for (var en : e.getAsJsonObject().entrySet()) {
                strings(en.getValue(), out);
            }
        }
    }

    /** Strings that look like resource ids ({@code ns:path}, optional leading {@code #} kept). */
    static List<String> ids(JsonElement e) {
        List<String> out = new ArrayList<>();
        strings(e, s -> {
            String core = s.startsWith("#") ? s.substring(1) : s;
            if (core.indexOf(':') > 0 && ResourceLocation.tryParse(core) != null) {
                out.add(s);
            }
        });
        return out;
    }

    static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }
}
