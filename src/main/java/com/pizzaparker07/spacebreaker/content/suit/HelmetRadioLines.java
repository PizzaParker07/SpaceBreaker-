package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import net.minecraft.resources.ResourceLocation;

/** Which mission control line a sound belongs to, for the helmet radio caption (client.hud.HelmetRadio). */
public final class HelmetRadioLines {
    private HelmetRadioLines() {
    }

    /** The mission control line a sound event id ({@code spacebreaker:mission_control.<en|tr>.<line>}) belongs to, or null. */
    public static SbSounds.Line lineOf(ResourceLocation id) {
        if (!id.getNamespace().equals(Spacebreaker.MOD_ID)) {
            return null;
        }
        String path = id.getPath();
        String prefix = path.startsWith("mission_control.en.") ? "mission_control.en."
                : path.startsWith("mission_control.tr.") ? "mission_control.tr." : null;
        if (prefix == null) {
            return null;
        }
        String lineId = path.substring(prefix.length());
        for (SbSounds.Line l : SbSounds.Line.values()) {
            if (l.id.equals(lineId)) {
                return l;
            }
        }
        return null;
    }
}
