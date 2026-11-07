package com.pizzaparker07.spacebreaker.content.belt;

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
 * The first time a player sets foot on one of the worlds beyond Mars, a short note in the chat says where they are and
 * what makes the place special: the world's name and one fact worth knowing on arrival. Each player gets it once per
 * world (kept in the player's own data, so it works in multiplayer and survives relogging).
 */
public final class OuterArrivals {
    static final String VISITED = "spacebreaker:outer_worlds_visited";
    /** World -> {English note, Turkish note}. */
    static final Map<String, String[]> NOTES = new LinkedHashMap<>();

    static {
        note("ceres", "The largest body in the asteroid belt. Bright salt spots mark where brine reached the surface; ice lies underneath.",
                "Asteroit kuşağının en büyük cismi. Parlak tuz lekeleri tuzlu suyun yüzeye çıktığı yerleri gösterir; altında buz vardır.");
        note("vesta", "A battered protoplanet. Its south pole is one vast impact basin with a mountain three times the height of Everest.",
                "Hırpalanmış bir ön gezegen. Güney kutbu, ortasında Everest'in üç katı yükseklikte bir dağ olan dev bir çarpma havzası.");
        note("io", "The most volcanic world known. Sulfur lakes and lava stay hot here: watch where you step.",
                "Bilinen en volkanik dünya. Kükürt gölleri ve lav burada sıcak kalır: nereye bastığına dikkat et.");
        note("europa", "A smooth shell of ice over a hidden ocean. The reddish cracks are where the ocean touched the surface.",
                "Gizli bir okyanusun üstünde pürüzsüz bir buz kabuğu. Kızıl çatlaklar okyanusun yüzeye değdiği yerlerdir.");
        note("ganymede", "The largest moon in the solar system: old dark plains crossed by bright, grooved ice.",
                "Güneş Sistemi'nin en büyük uydusu: parlak, oluklu buzla kesilen eski koyu ovalar.");
        note("callisto", "The most cratered surface known, unchanged for four billion years. Bright frost caps every crater rim.",
                "Bilinen en kraterli yüzey, dört milyar yıldır değişmedi. Her krater kenarını parlak kırağı kaplar.");
        note("titan", "A thick orange sky and lakes of liquid methane. The air is dense enough to fly through, and freezing.",
                "Kalın turuncu bir gök ve sıvı metan gölleri. Hava uçacak kadar yoğun ve dondurucu.");
        note("enceladus", "Fountains of ice spray from the cracks at the south pole, feeding Saturn's E ring.",
                "Güney kutbundaki çatlaklardan buz fıskiyeleri fışkırır ve Satürn'ün E halkasını besler.");
        note("miranda", "A patchwork moon with the tallest cliff known. In this gravity a fall from the top lasts minutes.",
                "Bilinen en yüksek uçurumun olduğu yamalı bir uydu. Bu yerçekiminde tepeden düşüş dakikalar sürer.");
        note("ariel", "The youngest face among Uranus' moons: long, flat-floored valleys where icy lava once flowed.",
                "Uranüs uydularının en genç yüzü: bir zamanlar buzlu lavın aktığı uzun, düz tabanlı vadiler.");
        note("titania", "Uranus' largest moon, split by canyons hundreds of kilometres long.",
                "Uranüs'ün en büyük uydusu; yüzlerce kilometre uzunluğunda kanyonlarla yarılmış.");
        note("umbriel", "The darkest of Uranus' large moons, except for the bright ring on the floor of Wunda crater.",
                "Uranüs'ün büyük uydularının en karanlığı; tek istisna Wunda krateri tabanındaki parlak halka.");
        note("oberon", "Old, cratered ice with dark-floored craters and a mountain about 11 km high.",
                "Koyu tabanlı kraterleri ve yaklaşık 11 km yüksekliğinde bir dağı olan eski, kraterli buz.");
        note("triton", "The coldest surface ever measured. Nitrogen geysers on the southern cap throw dark plumes kilometres high.",
                "Ölçülen en soğuk yüzey. Güney başlığındaki azot gayzerleri kilometrelerce yükseğe koyu sütunlar fırlatır.");
        note("proteus", "Too small to pull itself round, and darker than soot. The Pharos basin nearly broke it apart.",
                "Küre olamayacak kadar küçük ve isten koyu. Pharos havzası onu neredeyse parçalıyordu.");
        note("nereid", "On the most stretched orbit of any moon: Neptune shrinks to a small blue disc out here.",
                "Uyduların en basık yörüngesinde: Neptün buradan küçük mavi bir diske dönüşür.");
        note("pluto", "A heart-shaped plain of nitrogen ice, slowly churning, under a thin blue haze.",
                "İnce mavi bir pusun altında, yavaşça çalkalanan kalp biçimli bir azot buzu ovası.");
        note("charon", "Pluto's twin: the two always show each other the same face, so Pluto never moves in this sky.",
                "Plüton'un ikizi: ikisi birbirine hep aynı yüzü gösterir, bu yüzden Plüton bu gökte hiç kıpırdamaz.");
        note("nix", "A tumbling shard of water ice. A strong jump here could almost reach orbit.",
                "Yuvarlanan bir su buzu parçası. Burada güçlü bir zıplayış neredeyse yörüngeye ulaşabilir.");
        note("hydra", "The brightest ground in Pluto's system: almost pure water ice.",
                "Plüton sisteminin en parlak yüzeyi: neredeyse saf su buzu.");
        note("bennu", "A loose rubble pile darker than coal. Nothing here is solid rock: tread lightly.",
                "Kömürden koyu, gevşek bir moloz yığını. Burada hiçbir şey sağlam kaya değil: hafif adım at.");
        note("ryugu", "A spinning top of carbon-rich rubble, with a ridge round its waist.",
                "Belinin çevresinde bir sırt olan, karbonca zengin molozdan bir topaç.");
        note("eros", "A long stony asteroid, the first ever orbited and landed on by a spacecraft.",
                "Bir uzay aracının yörüngesine girip indiği ilk asteroit; uzun ve taşlı.");
        note("itokawa", "A peanut of two rubble lumps; the smooth gravel of the Muses Sea lies in its neck.",
                "İki moloz parçasından bir fıstık; boynunda Muses Denizi'nin düz çakılı uzanır.");
    }

    private OuterArrivals() {
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

    /** True if the player has already been to that outer world. */
    public static boolean visited(ServerPlayer player, String world) {
        return player.getPersistentData().getCompound(VISITED).getBoolean(world);
    }

    static void translations(Lang lang) {
        NOTES.forEach((w, t) -> lang.add("arrival.spacebreaker." + w, t[0], t[1]));
    }
}
