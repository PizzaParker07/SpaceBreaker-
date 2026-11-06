package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.guide.GuideScreen;
import com.pizzaparker07.spacebreaker.client.research.ResearchScreen;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlock;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlockEntity;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/** Research area: labs, items, research tree, field guide and the Earth observatory dungeon. */
final class ResearchScenario implements ShowcaseScenario {
    private static final int F = ShowcaseBuilder.FLOOR;
    /** Observatory template origin: the site sits on a low mound so its cellar is underground. */
    private static final int MOUND = 4;
    private static final BlockPos SITE = new BlockPos(12, F - 4 + MOUND, -6);
    private static final BlockPos LAB = new BlockPos(0, F + 1, 0);

    @Override
    public void build(ServerLevel level) {
        for (int x = -10; x <= 10; x++) {
            for (int z = -6; z <= 8; z++) {
                for (int y = F + 1; y <= F + 8; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                boolean floor = x >= -6 && x <= 6 && z >= -3 && z <= 4;
                level.setBlock(new BlockPos(x, F, z), floor ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // back wall with the collection in item frames
        for (int x = -6; x <= 6; x++) {
            for (int y = F + 1; y <= F + 5; y++) {
                level.setBlock(new BlockPos(x, y, -2), (y == F + 1 || y == F + 5 ? Blocks.STONE_BRICKS : Blocks.WHITE_CONCRETE).defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            }
        }
        List<ItemStack> frames = new ArrayList<>();
        for (String t : new String[]{"basic_rocketry", "pressure_suits", "aluminium_smelting", "observational_astronomy",
                "launch_infrastructure", "field_geology"}) {
            frames.add(Research.disk(ResearchTree.id(t)));
        }
        frames.add(Research.sample(ResearchTree.EARTH));
        frames.add(Research.sample(ResearchTree.MOON));
        frames.add(Research.experimentData(ResearchTree.SUBORBITAL));
        frames.add(new ItemStack(ResearchModule.SAMPLE_KIT.get()));
        frames.add(new ItemStack(ResearchModule.GUIDE_BOOK.get()));
        for (int i = 0; i < frames.size(); i++) {
            int row = i < 6 ? 0 : 1;
            int col = row == 0 ? i : i - 6;
            int x = row == 0 ? -5 + col * 2 : -4 + col * 2;
            ItemFrame frame = new ItemFrame(level, new BlockPos(x, F + 4 - row, -1), Direction.SOUTH);
            frame.setItem(frames.get(i), false);
            level.addFreshEntity(frame);
        }
        // three labs on the bench line, solar trackers at the ends
        for (int x = -2; x <= 2; x += 2) {
            level.setBlock(LAB.offset(x, 0, 0), ResearchModule.RESEARCH_LAB.get().defaultBlockState()
                    .setValue(ResearchLabBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        }
        level.setBlock(LAB.offset(-5, 0, 1), MachinesModule.SOLAR_PANEL.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(LAB.offset(5, 0, 1), MachinesModule.SOLAR_PANEL.get().defaultBlockState(), Block.UPDATE_ALL);
        ServerPlayer player = level.getServer().getPlayerList().getPlayers().get(0);
        startLab(level, player, LAB, "field_geology");
        startLab(level, player, LAB.offset(2, 0, 0), "observational_astronomy");

        // a low grassy mound for the site (superflat ground is too thin for the cellar)
        for (int x = SITE.getX() - 10; x < SITE.getX() + 34 + 10; x++) {
            for (int z = SITE.getZ() - 10; z < SITE.getZ() + 25 + 10; z++) {
                int dx = Math.max(0, Math.max(SITE.getX() - x, x - (SITE.getX() + 33)));
                int dz = Math.max(0, Math.max(SITE.getZ() - z, z - (SITE.getZ() + 24)));
                int h = MOUND - (int) Math.ceil(Math.hypot(dx, dz) / 2.5);
                for (int y = 1; y <= h; y++) {
                    level.setBlock(new BlockPos(x, F + y, z), (y == h ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                if (h > 0) {
                    level.setBlock(new BlockPos(x, F, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        // the observatory dungeon, weathered like world generation does it
        StructureTemplate template = level.getStructureManager().get(Spacebreaker.id("dungeons/earth/observatory")).orElseThrow();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        StructureProcessorList processors = level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                .get(ResourceKey.create(Registries.PROCESSOR_LIST, Spacebreaker.id("observatory_weathering")));
        if (processors != null) {
            processors.list().forEach(settings::addProcessor);
        }
        template.placeInWorld(level, SITE, SITE, settings, level.random, Block.UPDATE_CLIENTS);
    }

    private static void startLab(ServerLevel level, ServerPlayer player, BlockPos pos, String tech) {
        if (level.getBlockEntity(pos) instanceof ResearchLabBlockEntity lab) {
            lab.setResearcher(player);
            lab.setItem(ResearchLabBlockEntity.SLOT_INPUT, Research.disk(ResearchTree.id(tech)));
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            for (int i = 0; i < 120 && energy != null; i++) {
                energy.receiveEnergy(ResearchLabBlockEntity.MAX_RECEIVE, false);
            }
        }
    }

    private static void learnSome(ServerPlayer player) {
        for (String t : new String[]{"basic_rocketry", "pressure_suits", "aluminium_smelting", "solar_power", "metal_pressing",
                "electronics", "observational_astronomy", "oxygen_storage", "thermal_regulation", "fuel_refining",
                "liquid_fuel_engines", "launch_infrastructure"}) {
            Tech.learn(player, ResearchTree.id(t));
        }
    }

    private static void hands(ServerPlayer player) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        var inv = player.getInventory();
        inv.setItem(0, Research.disk(ResearchTree.id("basic_rocketry")));
        inv.setItem(1, Research.disk(ResearchTree.id("pressure_suits")));
        inv.setItem(2, Research.disk(ResearchTree.id("aluminium_smelting")));
        inv.setItem(3, Research.disk(ResearchTree.id("observational_astronomy")));
        inv.setItem(4, Research.sample(ResearchTree.EARTH));
        inv.setItem(5, Research.sample(ResearchTree.MOON));
        inv.setItem(6, Research.experimentData(ResearchTree.ORBIT));
        inv.setItem(7, new ItemStack(ResearchModule.SAMPLE_KIT.get()));
        inv.setItem(8, new ItemStack(ResearchModule.GUIDE_BOOK.get()));
        inv.selected = 7;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(7));
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges();
    }

    private static void emptyHand(ServerPlayer player) {
        player.getInventory().selected = 4;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(4));
    }

    private static void openLab(ServerPlayer player) {
        emptyHand(player);
        learnSome(player);
        if (player.level().getBlockEntity(LAB) instanceof ResearchLabBlockEntity lab) {
            player.openMenu(lab, buf -> buf.writeBlockPos(LAB));
        }
    }

    private static Runnable tree(String focus) {
        return () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getToasts().clear();
            ResearchScreen screen = new ResearchScreen(null);
            mc.setScreen(screen);
            screen.focus(ResearchTree.id(focus));
        };
    }

    private static Runnable guideChapter(String chapter) {
        return () -> {
            Minecraft.getInstance().getToasts().clear();
            GuideScreen screen = new GuideScreen();
            Minecraft.getInstance().setScreen(screen);
            screen.showChapter(chapter);
        };
    }

    private static Runnable guideEntry(String entry, int spread) {
        return () -> {
            Minecraft.getInstance().getToasts().clear();
            GuideScreen screen = new GuideScreen();
            Minecraft.getInstance().setScreen(screen);
            screen.showEntry(Spacebreaker.id(entry), spread);
        };
    }

    private static Runnable guideSearch(String text) {
        return () -> {
            Minecraft.getInstance().getToasts().clear();
            GuideScreen screen = new GuideScreen();
            Minecraft.getInstance().setScreen(screen);
            screen.setSearch(text);
        };
    }

    private static void researchTab() {
        Minecraft mc = Minecraft.getInstance();
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(screen);
        try {
            Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, SbTabs.RESEARCH.holder.get());
        } catch (ReflectiveOperationException e) {
            ShowcaseDriver.log("could not select tab: " + e);
        }
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        double g = F + 1;
        List<Shot> s = new ArrayList<>();
        s.add(new Shot("01_labs", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, true, fp, 80, null, null));
        s.add(new Shot("02_lab_close", 6000, 1.6, g + 1.5, 2.3, 155F, 28F, true, fp, 60, null, null));
        s.add(new Shot("03_collection", 6000, 0.5, g + 3.0, 3.6, 180F, 8F, true, fp, 60, null, null));
        s.add(new Shot("04_lab_gui", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, false, fp, 40, ResearchScenario::openLab, null));
        s.add(new Shot("05_research_tree", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, false, fp, 40, p -> {
            emptyHand(p);
            learnSome(p);
        }, tree("staging")));
        s.add(new Shot("06_research_tree_life", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, false, fp, 40, null, tree("airlocks")));
        s.add(new Shot("07_guide_index", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, false, fp, 40, null, guideChapter("research")));
        s.add(new Shot("08_guide_lab", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, false, fp, 40, null, guideEntry("research_lab", 0)));
        s.add(new Shot("09_guide_orbits", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, false, fp, 40, null, guideEntry("orbits", 0)));
        s.add(new Shot("10_guide_search", 6000, 0.5, g + 1.9, 4.2, 180F, 22F, false, fp, 40, null, guideSearch("moon")));
        double m = g + MOUND;
        s.add(new Shot("11_observatory_wide", 7000, 29.5, m + 11, 31.0, 180F, 22F, true, fp, 200, null, null));
        s.add(new Shot("12_observatory_dome", 7000, 24.0, m + 1.6, 8.5, 115F, -10F, true, fp, 80, null, null));
        s.add(new Shot("13_observatory_archive", 7000, 20.5, m - 2.38, 9.5, 61F, 22F, true, fp, 80, null, null));
        s.add(new Shot("14_test_stand", 9500, 31.5, m + 4.5, 20.5, 200F, 14F, true, fp, 80, null, null));
        s.add(new Shot("15_observatory_dusk", 12600, 4.5, m + 5.5, 22.5, 225F, 8F, true, fp, 120, null, null));
        s.add(new Shot("16_hotbar", 6000, 0.5, g + 1.62, 4.5, 180F, 30F, false, fp, 40, ResearchScenario::hands, null));
        s.add(new Shot("17_creative_tab", 6000, 0.5, g + 1.62, 4.5, 180F, 30F, false, fp, 30, ResearchScenario::hands, ResearchScenario::researchTab));
        return s;
    }
}
