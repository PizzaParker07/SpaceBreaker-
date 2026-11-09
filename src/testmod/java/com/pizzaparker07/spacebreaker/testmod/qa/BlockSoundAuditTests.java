package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * QA (beta.1 feedback, "cables sound like wool"): no block of the mod sounds like wool unless it is soft. Every
 * registered block's sound is read (with the level, for blocks that choose their sound per position); a block whose
 * step or break sound is wool's must be on the short list of soft blocks below or have a soft-material name. Writes
 * {@code run-gametest/qa/block_sounds.md}.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockSoundAuditTests {
    /** Soft blocks where a wool sound is right. */
    private static final Set<String> SOFT = Set.of("station_bunk", "envelope_fabric");
    private static final Pattern SOFT_NAME = Pattern.compile(".*(wool|fabric|cloth|carpet|felt|foam|cushion|bunk|bed|curtain).*");

    @GameTest(template = "empty")
    public static void metalBlocksDoNotSoundLikeWool(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        List<String> wool = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!Spacebreaker.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            SoundType sound;
            try {
                sound = block.defaultBlockState().getSoundType(level, pos, null);
            } catch (RuntimeException e) {
                sound = block.defaultBlockState().getSoundType();
            }
            boolean woolly = sound == SoundType.WOOL || sound.getStepSound() == SoundType.WOOL.getStepSound()
                    || sound.getBreakSound() == SoundType.WOOL.getBreakSound();
            if (!woolly) {
                continue;
            }
            wool.add(id.getPath());
            if (!SOFT.contains(id.getPath()) && !SOFT_NAME.matcher(id.getPath()).matches()) {
                bad.add(id.getPath());
            }
        }
        ProgressionReport.write("block_sounds.md", "## Block sounds (`qa/BlockSoundAuditTests`)\n\nWool sound: "
                + (wool.isEmpty() ? "none" : String.join(", ", wool)) + "\n\nNot soft but sounds like wool: "
                + (bad.isEmpty() ? "none" : String.join(", ", bad)) + "\n");
        h.assertTrue(bad.isEmpty(), "blocks that are not soft but sound like wool: " + bad);
        h.succeed();
    }

    private BlockSoundAuditTests() {
    }
}
