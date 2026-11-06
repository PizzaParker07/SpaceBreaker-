package com.pizzaparker07.spacebreaker.testmod.research;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.dungeons.DungeonDef;
import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.dungeons.DungeonsModule;
import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import com.pizzaparker07.spacebreaker.content.research.ResearchSavedData;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.content.research.SampleKitItem;
import com.pizzaparker07.spacebreaker.content.research.TechDiskItem;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlockEntity;
import com.pizzaparker07.spacebreaker.content.research.net.KnownTechPayload;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechChangedEvent;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import com.pizzaparker07.spacebreaker.core.tech.TechRequirement;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {
    private static final String EMPTY = "empty";
    private static final ResourceLocation ROCKETRY = ResearchTree.id("basic_rocketry");

    private static ServerPlayer player(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }

    private static void remove(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().getServer().getPlayerList().remove(p);
    }

    // ------------------------------------------------------------------ tree data

    @GameTest(template = EMPTY)
    public static void techTreeLoadsWithValidPrerequisites(GameTestHelper helper) {
        Map<ResourceLocation, TechNode> all = Tech.all(helper.getLevel().registryAccess());
        helper.assertTrue(all.size() >= ResearchTree.all().size(), "tech registry has " + all.size() + " of " + ResearchTree.all().size());
        for (ResourceLocation id : ResearchTree.all().keySet()) {
            helper.assertTrue(all.containsKey(id), "missing tech json " + id);
        }
        for (Map.Entry<ResourceLocation, TechNode> e : all.entrySet()) {
            helper.assertFalse(e.getValue().category().isBlank(), e.getKey() + " has no branch"); // new branches are allowed
            for (ResourceLocation r : e.getValue().requires()) {
                helper.assertTrue(all.containsKey(r), e.getKey() + " requires unknown " + r);
                helper.assertTrue(all.get(r).tier() <= e.getValue().tier(), e.getKey() + " requires a later-phase tech " + r);
            }
        }
        // no cycles: every node reaches a root
        Map<ResourceLocation, Integer> state = new HashMap<>();
        for (ResourceLocation id : all.keySet()) {
            helper.assertFalse(cycle(id, all, state), "prerequisite cycle through " + id);
        }
        helper.assertTrue(all.get(ROCKETRY).requires().isEmpty(), "basic rocketry is a root");
        helper.succeed();
    }

    private static boolean cycle(ResourceLocation id, Map<ResourceLocation, TechNode> all, Map<ResourceLocation, Integer> state) {
        Integer s = state.get(id);
        if (s != null) {
            return s == 1;
        }
        state.put(id, 1);
        for (ResourceLocation r : all.get(id).requires()) {
            if (cycle(r, all, state)) {
                return true;
            }
        }
        state.put(id, 2);
        return false;
    }

    // ------------------------------------------------------------------ learning, persistence, events, sync

    @GameTest(template = EMPTY)
    public static void learnedTechPersistsAndPostsEvents(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        AtomicInteger events = new AtomicInteger();
        var listener = (java.util.function.Consumer<TechChangedEvent>) e -> {
            if (e.getPlayer() == p) {
                events.incrementAndGet();
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            helper.assertFalse(Tech.knows(p, ROCKETRY), "fresh player knows nothing");
            helper.assertTrue(Tech.learn(p, ROCKETRY), "learn returns true the first time");
            helper.assertFalse(Tech.learn(p, ROCKETRY), "learn returns false the second time");
            helper.assertTrue(events.get() == 1, "one TechChangedEvent, got " + events.get());
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            ServerPlayer copy = helper.makeMockServerPlayerInLevel();
            copy.load(saved);
            helper.assertTrue(Tech.knows(copy, ROCKETRY), "known tech survives save/load");
            helper.assertTrue(Tech.forget(p, ROCKETRY), "forget");
            helper.assertFalse(Tech.knows(p, ROCKETRY), "forgotten");
            remove(helper, copy);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            remove(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void syncPayloadRoundTrips(GameTestHelper helper) {
        Set<ResourceLocation> known = new HashSet<>(List.of(ROCKETRY, ResearchTree.id("solar_power")));
        KnownTechPayload out = new KnownTechPayload(known, List.of(ROCKETRY));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        KnownTechPayload.STREAM_CODEC.encode(buf, out);
        KnownTechPayload in = KnownTechPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(in.known().equals(known), "known set round-trips");
        helper.assertTrue(in.justLearned().equals(List.of(ROCKETRY)), "just-learned list round-trips");
        helper.succeed();
    }

    // ------------------------------------------------------------------ samples, team sharing, gating

    @GameTest(template = EMPTY)
    public static void earthSamplesRevealRootTechnologies(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        var pick = Research.pickRevealed(p, ResearchTree.EARTH, helper.getLevel().random);
        helper.assertTrue(pick.isPresent(), "an Earth sample teaches something to a beginner");
        TechNode node = Tech.get(helper.getLevel().registryAccess(), pick.get()).orElseThrow();
        helper.assertTrue(node.requires().isEmpty() && node.tier() == 1, "beginners get a tier-1 root, got " + pick.get());
        helper.assertTrue(Research.revealableBy(p, ResearchTree.MOON).isEmpty(), "Moon samples need prerequisites first");
        helper.assertTrue(SampleKitItem.kindFor(helper.getLevel()).equals(ResearchTree.EARTH), "overworld samples are Earth samples");
        helper.assertTrue(SampleKitItem.isSampleable(Blocks.STONE.defaultBlockState()), "stone can be sampled");
        helper.assertFalse(SampleKitItem.isSampleable(Blocks.OAK_PLANKS.defaultBlockState()), "planks cannot be sampled");
        remove(helper, p);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void teamSharingTeachesTeamMates(GameTestHelper helper) {
        ServerPlayer a = player(helper);
        ServerPlayer b = player(helper);
        Scoreboard board = helper.getLevel().getScoreboard();
        PlayerTeam team = board.addPlayerTeam("sb_test_team_" + helper.getLevel().random.nextInt(100000));
        board.addPlayerToTeam(a.getScoreboardName(), team);
        // mock players share one profile name, so b is in the team too (teams are keyed by name)
        ResourceLocation tech = ResearchTree.id("solar_power");
        Research.teach(a, tech, true);
        helper.assertTrue(Tech.knows(a, tech), "researcher learns");
        helper.assertTrue(Tech.knows(b, tech), "team mate learns");
        helper.assertTrue(ResearchSavedData.get(helper.getLevel().getServer()).teamTech(team.getName()).contains(tech),
                "team record kept for offline members");
        board.removePlayerTeam(team);
        remove(helper, a);
        remove(helper, b);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void techRequirementGatesUnlocks(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        var access = helper.getLevel().registryAccess();
        TechRequirement req = TechRequirement.forUnlock(access, Spacebreaker.id("rocket_assembler"));
        helper.assertTrue(req.techs().contains(ROCKETRY), "rocket assembler is unlocked by basic rocketry");
        helper.assertFalse(TechRequirement.isUnlocked(p, Spacebreaker.id("rocket_assembler")), "locked before learning");
        helper.assertTrue(TechRequirement.isUnlocked(p, Spacebreaker.id("something_nobody_gates")), "ungated ids are free");
        helper.assertFalse(TechRequirement.of(ROCKETRY).test(p), "strict test fails");
        Tech.learn(p, ROCKETRY);
        helper.assertTrue(TechRequirement.isUnlocked(p, Spacebreaker.id("rocket_assembler")), "unlocked after learning");
        helper.assertTrue(TechRequirement.of(ROCKETRY).test(p), "strict test passes");
        remove(helper, p);
        helper.succeed();
    }

    // ------------------------------------------------------------------ research lab

    @GameTest(template = EMPTY, timeoutTicks = 900)
    public static void labTeachesTechFromDisk(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ResearchModule.RESEARCH_LAB.get());
        ResearchLabBlockEntity lab = (ResearchLabBlockEntity) helper.getBlockEntity(pos);
        lab.setResearcher(p);
        lab.setItem(ResearchLabBlockEntity.SLOT_INPUT, Research.disk(ROCKETRY));
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(energy != null, "lab exposes an energy capability");
        helper.onEachTick(() -> energy.receiveEnergy(ResearchLabBlockEntity.MAX_RECEIVE, false));
        helper.succeedWhen(() -> {
            helper.assertTrue(Tech.knows(p, ROCKETRY), "not learned yet (status " + lab.status() + ", " + lab.progress() + "/" + lab.total() + ")");
            ItemStack out = lab.getItem(ResearchLabBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.getItem() instanceof TechDiskItem && ROCKETRY.equals(TechDiskItem.tech(out)), "disk handed back");
            helper.assertTrue(Boolean.TRUE.equals(out.get(ResearchModule.ANALYSED.get())), "disk marked as read");
            remove(helper, p);
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void labWaitsForPowerAndPrerequisites(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ResearchModule.RESEARCH_LAB.get());
        ResearchLabBlockEntity lab = (ResearchLabBlockEntity) helper.getBlockEntity(pos);
        lab.setResearcher(p);
        lab.setItem(ResearchLabBlockEntity.SLOT_INPUT, Research.disk(ResearchTree.id("staging")));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(lab.status() == ResearchLabBlockEntity.MISSING_PREREQS, "staging needs prerequisites, status " + lab.status());
            lab.setItem(ResearchLabBlockEntity.SLOT_INPUT, Research.disk(ROCKETRY));
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(lab.status() == ResearchLabBlockEntity.NO_POWER, "no power -> paused, status " + lab.status());
            helper.assertFalse(Tech.knows(p, ROCKETRY), "nothing learned without power");
            remove(helper, p);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ dungeons and loot

    @GameTest(template = EMPTY)
    public static void observatoryLootRollsTechDisks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(Dungeons.lootTable("earth", "observatory_archive"));
        helper.assertTrue(table != LootTable.EMPTY, "archive loot table exists");
        Map<ResourceLocation, TechNode> all = Tech.all(level.registryAccess());
        boolean rocketry = true, flightData = true;
        for (int i = 0; i < 20; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            List<ItemStack> items = table.getRandomItems(params);
            boolean found = false, data = false;
            for (ItemStack s : items) {
                data |= s.is(ResearchModule.EXPERIMENT_DATA.get()) && ResearchTree.SUBORBITAL.equals(s.get(ResearchModule.SAMPLE.get()));
                if (s.getItem() instanceof TechDiskItem) {
                    ResourceLocation t = TechDiskItem.tech(s);
                    helper.assertTrue(t != null && all.containsKey(t), "disk holds a real technology: " + t);
                    helper.assertTrue(all.get(t).tier() == 1, "observatory disks are phase 1, got " + t);
                    found |= ROCKETRY.equals(t);
                }
            }
            rocketry &= found;
            flightData &= data;
        }
        helper.assertTrue(rocketry, "every archive has the basic rocketry disk");
        helper.assertTrue(flightData, "every archive has suborbital flight data (vacuum engines need it)");
        LootTable supplies = level.getServer().reloadableRegistries().getLootTable(Dungeons.lootTable("earth", "observatory_supplies"));
        helper.assertTrue(supplies != LootTable.EMPTY, "supplies loot table exists");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "research_structure", timeoutTicks = 200)
    public static void observatoryTemplatePlacesWithLootChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        DungeonDef def = Dungeons.all(access).get(Dungeons.EARTH_OBSERVATORY);
        helper.assertTrue(def != null, "earth observatory dungeon definition loaded");
        Structure structure = access.registryOrThrow(Registries.STRUCTURE).get(def.structure());
        helper.assertTrue(structure != null, "worldgen structure " + def.structure() + " exists");
        StructureTemplate template = level.getStructureManager().get(Spacebreaker.id("dungeons/earth/observatory")).orElse(null);
        helper.assertTrue(template != null, "observatory template exists");
        BlockPos origin = helper.absolutePos(new BlockPos(0, 6, 0)).offset(0, 0, 40);
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 2);
        int archives = 0, supplies = 0;
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(origin, new StructurePlaceSettings(), Blocks.CHEST)) {
            helper.assertTrue(level.getBlockState(info.pos()).getBlock() instanceof ChestBlock, "chest placed at " + info.pos());
            if (level.getBlockEntity(info.pos()) instanceof ChestBlockEntity chest) {
                ResourceKey<LootTable> key = chest.getLootTable();
                if (Dungeons.lootTable("earth", "observatory_archive").equals(key)) {
                    archives++;
                } else if (Dungeons.lootTable("earth", "observatory_supplies").equals(key)) {
                    supplies++;
                }
            }
        }
        helper.assertTrue(archives == 1 && supplies == 1, "one archive and one supply chest, got " + archives + "/" + supplies);
        helper.assertTrue(template.getSize().getX() > 20 && template.getSize().getZ() > 20, "full-size site");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void moonTechDataRollsDisksAndDungeonsResolve(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, Spacebreaker.id("chests/moon/tech_data")));
        Map<ResourceLocation, TechNode> all = Tech.all(level.registryAccess());
        for (int i = 0; i < 20; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
            int disks = 0;
            for (ItemStack s : table.getRandomItems(params)) {
                if (s.getItem() instanceof TechDiskItem) {
                    ResourceLocation t = TechDiskItem.tech(s);
                    helper.assertTrue(t != null && all.containsKey(t) && all.get(t).tier() <= 2, "Moon disk is a phase 1-2 tech: " + t);
                    disks++;
                }
            }
            helper.assertTrue(disks == 1, "Moon tech data always holds one disk, got " + disks);
        }
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<ResourceLocation, DungeonDef> e : Dungeons.all(level.registryAccess()).entrySet()) {
            helper.assertTrue(structures.containsKey(e.getValue().structure()), e.getKey() + " points at a missing structure");
            for (ResourceLocation loot : e.getValue().lootTables()) {
                helper.assertTrue(level.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE).contains(loot),
                        e.getKey() + " lists a missing loot table " + loot);
            }
        }
        helper.assertTrue(Dungeons.all(level.registryAccess()).size() >= 3, "Earth observatory + two Moon dungeons");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void techIconsAndUnlocksAreRealItems(GameTestHelper helper) {
        for (Map.Entry<ResourceLocation, TechNode> e : Tech.all(helper.getLevel().registryAccess()).entrySet()) {
            e.getValue().icon().ifPresent(icon -> helper.assertTrue(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(icon), e.getKey() + " has a missing icon " + icon));
        }
        var recipes = helper.getLevel().getRecipeManager();
        for (Map.Entry<ResourceLocation, TechNode> e : Tech.all(helper.getLevel().registryAccess()).entrySet()) {
            for (ResourceLocation u : e.getValue().unlocks()) {
                boolean real = net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(u)
                        || net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(u)
                        || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(u)
                        || recipes.byKey(u).isPresent();
                helper.assertTrue(real, e.getKey() + " unlocks " + u + ", which is not a registered item, block, entity or recipe");
            }
        }
        for (String starter : new String[]{"crusher", "combustion_generator", "battery_bank", "fluid_tank", "pressurized_tank", "cryogenic_tank", "research_lab"}) {
            helper.assertTrue(TechRequirement.forUnlock(helper.getLevel().registryAccess(), Spacebreaker.id(starter)).isEmpty(),
                    starter + " must stay a starter machine (not gated)");
        }
        helper.assertTrue(TechRequirement.forUnlock(helper.getLevel().registryAccess(), Spacebreaker.id("arc_furnace")).techs()
                .contains(ResearchTree.id("metallurgy")), "arc furnace gated by metallurgy");
        helper.assertTrue(TechRequirement.forUnlock(helper.getLevel().registryAccess(), Spacebreaker.id("fabricator")).techs()
                .contains(ResearchTree.id("precision_manufacturing")), "fabricator gated by precision manufacturing");
        helper.assertTrue(TechRequirement.forUnlock(helper.getLevel().registryAccess(), Spacebreaker.id("kerosene_engine"))
                .techs().contains(ResearchTree.id("liquid_fuel_engines")), "kerosene engine gated by liquid-fuel engines");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void discoveryOutsideDungeonsDoesNothing(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        p.moveTo(helper.absoluteVec(new Vec3(1, 2, 1)));
        helper.assertFalse(DungeonsModule.discover(p), "no dungeon here");
        helper.assertTrue(p.getData(DungeonsModule.DISCOVERED).isEmpty(), "nothing recorded");
        remove(helper, p);
        helper.succeed();
    }

    private ResearchGameTests() {
    }
}
