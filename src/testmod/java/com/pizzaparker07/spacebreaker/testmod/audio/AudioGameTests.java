package com.pizzaparker07.spacebreaker.testmod.audio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AudioGameTests {
    /** Every registered sound has a sounds.json entry, and every entry points at an .ogg file that ships in the jar. */
    @GameTest(template = "empty")
    public static void everySoundIsDefinedAndHasItsFile(GameTestHelper helper) throws Exception {
        JsonObject sounds;
        try (InputStream in = Spacebreaker.class.getResourceAsStream("/assets/spacebreaker/sounds.json")) {
            helper.assertTrue(in != null, "assets/spacebreaker/sounds.json missing");
            sounds = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (var holder : SbSounds.SOUNDS.getEntries()) {
            String name = holder.getId().getPath();
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(holder.getId()), "sound event not registered: " + name);
            helper.assertTrue(sounds.has(name), "sounds.json has no entry for " + name);
        }
        for (var entry : sounds.entrySet()) {
            for (var file : entry.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String path = file.getAsJsonObject().get("name").getAsString().split(":", 2)[1];
                try (InputStream ogg = Spacebreaker.class.getResourceAsStream("/assets/spacebreaker/sounds/" + path + ".ogg")) {
                    helper.assertTrue(ogg != null, "missing sound file " + path + ".ogg (" + entry.getKey() + ")");
                }
            }
        }
        helper.succeed();
    }

    /** Mission control speaks every line in both languages, and the countdown covers 10 to 1. */
    @GameTest(template = "empty")
    public static void missionControlSpeaksEnglishAndTurkish(GameTestHelper helper) {
        for (SbSounds.Line line : SbSounds.Line.values()) {
            helper.assertTrue(SbSounds.EN.get(line).isBound() && SbSounds.TR.get(line).isBound(), "voice line missing: " + line.id);
        }
        for (int n = 1; n <= 10; n++) {
            helper.assertTrue(SbSounds.Line.countdown(n) != null && SbSounds.Line.countdown(n).countdown, "countdown " + n);
        }
        helper.assertTrue(SbSounds.Line.countdown(0) == null && SbSounds.Line.countdown(11) == null, "only 10..1 are spoken");
        helper.succeed();
    }
}
