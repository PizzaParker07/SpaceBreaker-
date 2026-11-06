package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.earth.EarthWorldgen;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Worldgen and fluid tests for the Industry area. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryWorldTests {
    @GameTest(template = "empty")
    public static void oreFeaturesAreRegistered(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (EarthWorldgen.Vein v : EarthWorldgen.VEINS) {
            helper.assertTrue(access.registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(v.configured()), "missing configured " + v.configured());
            helper.assertTrue(access.registryOrThrow(Registries.PLACED_FEATURE).containsKey(v.placed()), "missing placed " + v.placed());
            helper.assertTrue(access.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).containsKey(v.modifier()), "missing biome modifier " + v.modifier());
        }
        helper.assertTrue(access.registryOrThrow(Registries.PLACED_FEATURE).containsKey(EarthWorldgen.OIL_PLACED), "missing oil deposit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oreVeinsGenerateInStoneAndDeepslate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        RandomSource random = RandomSource.create(42);
        for (EarthWorldgen.Vein v : EarthWorldgen.VEINS) {
            for (var host : new net.minecraft.world.level.block.Block[]{Blocks.STONE, Blocks.DEEPSLATE}) {
                fill(helper, host);
                ConfiguredFeature<?, ?> feature = registry.getOrThrow(v.configured());
                BlockPos centre = helper.absolutePos(new BlockPos(3, 3, 3));
                for (int i = 0; i < 8; i++) {
                    feature.place(level, level.getChunkSource().getGenerator(), random, centre);
                }
                var expected = host == Blocks.STONE ? v.metal().ore.get() : v.metal().deepslateOre.get();
                int found = count(helper, expected);
                helper.assertTrue(found > 0, "no " + expected + " generated in " + host);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cryogenicLiquidBoilsAway(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, FluidsModule.LIQUID_OXYGEN.block.get());
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).is(FluidsModule.TAG_LOX), "LOX must carry the c:liquid_oxygen tag");
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.AIR, pos));
    }

    @GameTest(template = "empty")
    public static void fluidTagsAndBuckets(GameTestHelper helper) {
        helper.assertTrue(FluidsModule.RP1.get().is(FluidsModule.TAG_RP1), "RP-1 tag");
        helper.assertTrue(FluidsModule.LIQUID_HYDROGEN.get().is(FluidsModule.TAG_LH2), "LH2 tag");
        helper.assertTrue(FluidsModule.LIQUID_METHANE.get().is(FluidsModule.TAG_LCH4), "LCH4 tag");
        helper.assertTrue(FluidsModule.OXYGEN.get().is(FluidsModule.TAG_OXYGEN), "O2 tag");
        helper.assertTrue(FluidsModule.HYDROGEN.get().is(FluidsModule.TAG_HYDROGEN), "H2 tag");
        helper.assertTrue(FluidsModule.OXYGEN.block == null, "gases have no world block");
        helper.assertTrue(FluidsModule.OXYGEN.type.get().isLighterThanAir(), "gases are lighter than air");
        helper.assertTrue(FluidsModule.CRUDE_OIL.bucket.get().content == FluidsModule.CRUDE_OIL.get(), "crude oil bucket");
        helper.succeed();
    }

    static void fill(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                for (int z = 0; z < 7; z++) {
                    helper.setBlock(x, y, z, block);
                }
            }
        }
    }

    static int count(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        int n = 0;
        for (int x = -4; x < 11; x++) {
            for (int y = -4; y < 11; y++) {
                for (int z = -4; z < 11; z++) {
                    if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z))).is(block)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private IndustryWorldTests() {
    }
}
