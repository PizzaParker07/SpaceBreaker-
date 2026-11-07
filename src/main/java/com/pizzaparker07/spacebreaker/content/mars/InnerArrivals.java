package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The first time a player sets foot on an inner world (Mars, its moons, Venus, Mercury), a short note in the chat says
 * where they are and the one thing worth knowing on arrival there. Once per player and world, kept in the player's
 * data; the same format as the outer worlds' notes.
 */
public final class InnerArrivals {
    static final String VISITED = "spacebreaker:inner_worlds_visited";
    /** World -> {English note, Turkish note}. */
    static final Map<String, String[]> NOTES = new LinkedHashMap<>();

    static {
        note("mars", "Thin, freezing carbon-dioxide air you cannot breathe. Dust storms are seen coming; solar panels gather dust, and passing dust whirls or a brush wipe it off.",
                "Solunamayan, ince ve dondurucu karbondioksit havası. Toz fırtınaları önceden görülür; güneş panelleri toz tutar, geçen toz hortumları ya da bir fırça onları temizler.");
        note("phobos", "Gravity a two-thousandth of Earth's: a hard push could carry you off. Mars fills a fifth of the sky; the great crater Stickney lies to the east.",
                "Yerçekimi Dünya'nınkinin iki binde biri: sert bir itiş seni uzaya fırlatabilir. Mars göğün beşte birini kaplar; dev Stickney krateri doğudadır.");
        note("deimos", "Even smaller than Phobos: a single jump lasts many seconds. Old relay spires stand on the dusty plains.",
                "Phobos'tan da küçük: tek bir zıplayış saniyelerce sürer. Tozlu ovalarda eski röle kuleleri yükselir.");
        note("venus", "The surface is 464 °C under 92 bar: no suit survives it. Stay in the cloud layer (y 195-245), near 1 bar and room temperature, but acid eats unprotected armour.",
                "Yüzey 92 bar basınç altında 464 °C: hiçbir giysi dayanmaz. Bulut katmanında (y 195-245) kal; orada basınç yaklaşık 1 bar ve sıcaklık oda sıcaklığındadır, ama asit korumasız zırhı aşındırır.");
        note("mercury", "Ground in sunlight is hotter than an oven, but any shadow is bitterly cold: travel in the shade of crater rims and scarps. Ice waits in the dark polar craters.",
                "Güneşteki zemin fırından sıcak, ama her gölge dondurucu: krater kenarlarının ve yamaçların gölgesinde ilerle. Karanlık kutup kraterlerinde buz bekler.");
    }

    private InnerArrivals() {
    }

    private static void note(String world, String en, String tr) {
        NOTES.put(world, new String[]{en, tr});
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                arrive(player, e.getTo().location());
            }
        });
    }

    /** Sends the arrival note if this is the player's first visit to that world; returns true if it was sent. */
    public static boolean arrive(ServerPlayer player, ResourceLocation world) {
        if (!world.getNamespace().equals(Spacebreaker.MOD_ID) || !NOTES.containsKey(world.getPath())) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        CompoundTag visited = data.getCompound(VISITED);
        if (visited.getBoolean(world.getPath())) {
            return false;
        }
        visited.putBoolean(world.getPath(), true);
        data.put(VISITED, visited);
        player.sendSystemMessage(Component.translatable("dimension.spacebreaker." + world.getPath()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal("  "))
                .append(Component.translatable("arrival.spacebreaker." + world.getPath()).withStyle(ChatFormatting.GRAY)));
        return true;
    }

    /** The inner worlds that have an arrival note. */
    public static java.util.Set<String> worlds() {
        return java.util.Collections.unmodifiableSet(NOTES.keySet());
    }

    static void translations(Lang lang) {
        NOTES.forEach((w, t) -> lang.add("arrival.spacebreaker." + w, t[0], t[1]));
    }
}
