package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.blueprint.BlueprintScreen;
import com.pizzaparker07.spacebreaker.content.blueprint.BlueprintModule;
import com.pizzaparker07.spacebreaker.content.blueprint.Hologram;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/** Hologram Projector: a half-built launch site with its projection, material list, layer mode and the habitat. */
final class BlueprintScenario implements ShowcaseScenario {
    private static final int FLOOR = ShowcaseBuilder.FLOOR;
    private static final double G = FLOOR + 1;
    static final BlockPos LAUNCH = new BlockPos(0, FLOOR + 1, 0);
    static final BlockPos HABITAT = new BlockPos(10, FLOOR + 2, 0);

    @Override
    public void build(ServerLevel level) {
        for (int x = -12; x <= 18; x++) {
            for (int z = -12; z <= 14; z++) {
                for (int y = FLOOR + 1; y <= FLOOR + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(new BlockPos(x, FLOOR, z), MoonModule.LUNAR_REGOLITH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        MultiblockDefinition launch = Multiblocks.get(level.registryAccess(), Spacebreaker.id("launch_site")).orElseThrow();
        // half built: the pad and trench, the lower tower, one wrong block in the pad
        for (MultiblockDefinition.Placement p : launch.placements(LAUNCH, Direction.NORTH)) {
            boolean build = p.layer() <= 2 && !(p.symbol() == 'P' && (p.pos().getX() + p.pos().getZ()) % 3 == 0) && p.symbol() != 'U';
            if (build) {
                level.setBlock(p.pos(), p.spec().display(MultiblockDefinition.rotation(Direction.NORTH)), Block.UPDATE_ALL);
            }
        }
        level.setBlock(LAUNCH.offset(2, 0, 2), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        // the habitat: floor and part of the walls
        MultiblockDefinition habitat = Multiblocks.get(level.registryAccess(), Spacebreaker.id("pressurised_habitat")).orElseThrow();
        for (MultiblockDefinition.Placement p : habitat.placements(HABITAT, Direction.NORTH)) {
            if (p.layer() == 0 || (p.layer() == 1 && p.pos().getX() < HABITAT.getX()) || p.symbol() == 'D') {
                level.setBlock(p.pos(), p.spec().display(MultiblockDefinition.rotation(Direction.NORTH)), Block.UPDATE_ALL);
            }
        }
    }

    private static Consumer<ServerPlayer> projecting(String blueprint, BlockPos anchor, int layer) {
        return p -> {
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            ItemStack projector = new ItemStack(BlueprintModule.HOLOGRAM_PROJECTOR.get());
            Hologram h = Hologram.of(Spacebreaker.id(blueprint)).withAnchor(anchor, Direction.NORTH);
            for (int i = 0; i <= layer; i++) {
                h = h.nextLayer(10);
            }
            projector.set(BlueprintModule.HOLOGRAM.get(), h);
            p.setItemSlot(EquipmentSlot.MAINHAND, projector);
            var inv = p.getInventory();
            inv.setItem(1, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Spacebreaker.id("launch_pad")), 12));
            inv.setItem(2, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Spacebreaker.id("service_tower")), 64));
        };
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new Shot("blueprint_01_launch_site", 6000, 0.5, G + 4.5, 9.5, 180F, 22F, false, fp, 160, projecting("launch_site", LAUNCH, -1), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("blueprint_02_close", 6000, -3.5, G + 3.0, 5.5, 215F, 18F, false, fp, 60, projecting("launch_site", LAUNCH, -1), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("blueprint_03_layer_mode", 6000, 0.5, G + 4.5, 9.5, 180F, 22F, false, fp, 60, projecting("launch_site", LAUNCH, 4), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("blueprint_04_habitat", 6000, 10.5, G + 3.5, 7.5, 180F, 20F, false, fp, 60, projecting("pressurised_habitat", HABITAT, -1), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("blueprint_05_hidden_gui", 6000, -3.5, G + 3.0, 5.5, 215F, 18F, true, fp, 40, projecting("launch_site", LAUNCH, -1), SuitShots.CLEAR_TOASTS));
        s.add(new Shot("blueprint_06_select", 6000, 0.5, G + 4.5, 9.5, 180F, 22F, false, fp, 30, projecting("launch_site", LAUNCH, -1),
                SuitShots.clearToastsThen(() -> Minecraft.getInstance().setScreen(new BlueprintScreen()))));
        // "Build": the projection being filled in from the inventory, block by block, a couple of seconds in
        s.add(new Shot("blueprint_08_building", 6000, 6.5, G + 4.0, 10.5, 150F, 25F, false, fp, 50, p -> {
            projecting("launch_site", LAUNCH, -1).accept(p);
            var def = com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(p.registryAccess(), Spacebreaker.id("launch_site")).orElseThrow();
            BlockPos c = LAUNCH;
            for (var pl : def.placements(c, Direction.NORTH)) {
                p.serverLevel().setBlock(pl.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                if (!pl.spec().isAir()) {
                    p.getInventory().add(new net.minecraft.world.item.ItemStack(pl.spec().item()));
                }
            }
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var stack = com.pizzaparker07.spacebreaker.content.blueprint.BlueprintModule.projectorInHand(p);
            com.pizzaparker07.spacebreaker.content.blueprint.BlueprintBuilder.start(p,
                    com.pizzaparker07.spacebreaker.content.blueprint.HologramProjectorItem.hologram(stack));
        }, SuitShots.CLEAR_TOASTS));
        // the pointer on the first material: its name and how many the player carries against how many it takes
        s.add(new Shot("blueprint_07_material_tip", 6000, 0.5, G + 4.5, 9.5, 180F, 22F, false, fp, 30, projecting("launch_site", LAUNCH, -1),
                SuitShots.clearToastsThen(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new BlueprintScreen());
                    var win = mc.getWindow();
                    int w = win.getGuiScaledWidth(), h = win.getGuiScaledHeight();
                    // same layout as BlueprintScreen: panel 30 px from the top and bottom, list 150 px wide
                    int panelW = Math.min(380, w - 20), left = (w - panelW) / 2;
                    int dy0 = 30 + 16 + 2, previewH = Math.max(60, (h - 30 - 30) / 2 - 20);
                    double gx = left + 150 + 8 + 8, gy = dy0 + 22 + previewH + 11 + 8;
                    // the virtual window has no real pointer: place the game's own cursor (test code only)
                    try {
                        for (String name : new String[]{"xpos", "ypos"}) {
                            var f = net.minecraft.client.MouseHandler.class.getDeclaredField(name);
                            f.setAccessible(true);
                            f.setDouble(mc.mouseHandler, (name.equals("xpos") ? gx : gy) * win.getGuiScale());
                        }
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                })));
        return s;
    }
}
