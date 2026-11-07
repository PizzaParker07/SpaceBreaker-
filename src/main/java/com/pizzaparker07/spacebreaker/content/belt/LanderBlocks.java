package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.content.belt.block.SpentEngineBlock;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Wreck parts of the crashed supply landers: gold foil cladding and a burnt-out engine. */
public final class LanderBlocks {
    /** Crinkled gold foil over a hull panel: the thermal blanket of a lander's descent stage. */
    public static final DeferredBlock<Block> GOLD_FOIL_CLADDING = BeltModule.KIT.block("gold_foil_cladding",
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** A burnt-out rocket engine torn from a wreck, pointing its nozzle the way it faces. */
    public static final DeferredBlock<SpentEngineBlock> SPENT_ENGINE = BeltModule.KIT.block("spent_engine", SpentEngineBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion()
                    .requiresCorrectToolForDrops());

    private LanderBlocks() {
    }

    static void load() {
    }

    static void translations(Lang lang) {
        lang.block(GOLD_FOIL_CLADDING, "Gold Foil Cladding", "Altın Folyo Kaplama");
        lang.block(SPENT_ENGINE, "Spent Rocket Engine", "Yanmış Roket Motoru");
        // the crew's last message on the sign in each crashed lander's cabin (four short lines)
        sign(lang, "lander_log_dust", new String[]{"LAST SIGNAL", "Leg gave way", "on touchdown.", "Cargo intact."},
                new String[]{"SON İLETİM", "İnerken bir", "ayak kırıldı.", "Kargo sağlam."});
        sign(lang, "lander_log_ice", new String[]{"LAST SIGNAL", "Skidding on ice", "cabin holds", "air. Waiting."},
                new String[]{"SON İLETİM", "Buzda kayıyoruz", "kabin hava", "tutuyor. Bekle."});
        sign(lang, "lander_log_comet", new String[]{"LAST SIGNAL", "A jet hit us", "on approach.", "Going down."},
                new String[]{"SON İLETİM", "Yaklaşırken", "jet çarptı.", "Düşüyoruz."});
    }

    private static void sign(Lang lang, String key, String[] en, String[] tr) {
        for (int i = 0; i < 4; i++) {
            lang.add("sign.spacebreaker." + key + "." + i, en[i], tr[i]);
        }
    }

    static void loot(SbBlockLoot loot) {
        loot.self(GOLD_FOIL_CLADDING.get());
        loot.self(SPENT_ENGINE.get());
    }

    static void tags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, GOLD_FOIL_CLADDING.get(), SPENT_ENGINE.get());
    }
}
