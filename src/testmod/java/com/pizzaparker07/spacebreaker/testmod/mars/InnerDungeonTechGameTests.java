package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsLoot;
import com.pizzaparker07.spacebreaker.content.mercury.IceMinePiece;
import com.pizzaparker07.spacebreaker.content.research.TechDiskItem;
import com.pizzaparker07.spacebreaker.content.venus.dungeon.VenusLoot;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Every inner world has a dungeon that really teaches something new: its loot can give a tech disk for a technology
 * of tier 2 or more whose research samples come from that world (so it is not a Phase-1 technology from Earth or the Moon).
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InnerDungeonTechGameTests {
    private static void teaches(GameTestHelper helper, String world, List<ResourceKey<LootTable>> tables) {
        ServerLevel level = helper.getLevel();
        Map<ResourceLocation, TechNode> all = Tech.all(level.registryAccess());
        ResourceLocation body = Spacebreaker.id(world);
        TreeSet<String> seen = new TreeSet<>();
        boolean own = false;
        for (ResourceKey<LootTable> key : tables) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            helper.assertTrue(table != LootTable.EMPTY, "loot table " + key.location());
            for (int i = 0; i < 60; i++) {
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
                for (ItemStack s : table.getRandomItems(params, 7000L + i)) {
                    if (s.getItem() instanceof TechDiskItem) {
                        ResourceLocation id = TechDiskItem.tech(s);
                        TechNode node = id == null ? null : all.get(id);
                        helper.assertTrue(node != null, "disk for unknown tech " + id + " in " + key.location());
                        seen.add(id.getPath());
                        own |= node.tier() >= 2 && node.samples().contains(body);
                    }
                }
            }
        }
        helper.assertTrue(own, world + " dungeons teach none of its own technologies, only " + seen);
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void marsDungeonsTeachMartianTech(GameTestHelper helper) {
        teaches(helper, "mars", List.of(MarsLoot.STATION_LAB, MarsLoot.STATION_ISRU, MarsLoot.STATION_COMMAND, MarsLoot.RIVERBED_PROBE));
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void venusAerostatTeachesVenusTech(GameTestHelper helper) {
        teaches(helper, "venus", List.of(VenusLoot.AEROSTAT_LAB, VenusLoot.AEROSTAT_STORES));
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void mercuryIceMineTeachesMercuryTech(GameTestHelper helper) {
        teaches(helper, "mercury", List.of(IceMinePiece.MINE_OFFICE, IceMinePiece.MINE_STORES));
        helper.succeed();
    }

    private InnerDungeonTechGameTests() {
    }
}
