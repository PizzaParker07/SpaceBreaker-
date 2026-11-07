package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.TreeSet;

/**
 * Writes every Spacebreaker block, item, entity type, fluid and sound event to {@code run-gametest/qa/registry.tsv}
 * for {@code tools/qa/asset_audit.py}, which checks models, textures and translations against it.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistryDumpTests {
    @GameTest(template = "empty")
    public static void registryIsDumpedForTheAssetAudit(GameTestHelper h) {
        TreeSet<String> lines = new TreeSet<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (!e.getKey().location().getNamespace().equals(Spacebreaker.MOD_ID)) {
                continue;
            }
            var block = e.getValue();
            String flags = (block instanceof LiquidBlock ? "liquid " : "")
                    + (block.defaultBlockState().getRenderShape() == RenderShape.INVISIBLE ? "invisible " : "")
                    + (block.defaultBlockState().getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED ? "entity_rendered " : "");
            lines.add("block\t" + e.getKey().location().getPath() + "\t" + flags.trim() + "\t" + block.getDescriptionId());
        }
        for (var e : BuiltInRegistries.ITEM.entrySet()) {
            if (!e.getKey().location().getNamespace().equals(Spacebreaker.MOD_ID)) {
                continue;
            }
            var item = e.getValue();
            String flags = item instanceof BlockItem bi ? "block_item:" + BuiltInRegistries.BLOCK.getKey(bi.getBlock()).getPath() : "";
            lines.add("item\t" + e.getKey().location().getPath() + "\t" + flags + "\t" + item.getDescriptionId());
        }
        dump(lines, "entity", BuiltInRegistries.ENTITY_TYPE, t -> t.getDescriptionId());
        dump(lines, "fluid", BuiltInRegistries.FLUID, f -> f.getFluidType().getDescriptionId());
        dump(lines, "sound", BuiltInRegistries.SOUND_EVENT, s -> "");
        ProgressionReport.write("registry.tsv", String.join("\n", lines) + "\n");
        h.assertTrue(lines.size() > 100, "registry dumped: " + lines.size());
        h.succeed();
    }

    private static <T> void dump(TreeSet<String> lines, String kind, Registry<T> registry, java.util.function.Function<T, String> key) {
        for (var e : registry.entrySet()) {
            if (e.getKey().location().getNamespace().equals(Spacebreaker.MOD_ID)) {
                lines.add(kind + "\t" + e.getKey().location().getPath() + "\t\t" + key.apply(e.getValue()));
            }
        }
    }

    private RegistryDumpTests() {
    }
}
