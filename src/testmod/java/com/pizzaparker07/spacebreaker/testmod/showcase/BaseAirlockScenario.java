package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlock;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks;
import com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.WarningLightBlock;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/**
 * A small Moon base with an airlock made of two sliding doors: the warning light over the outer door, the oxygen
 * distributor inside, and the helmet HUD going from vacuum in the airlock to pressurised air in the base.
 */
final class BaseAirlockScenario implements ShowcaseScenario {
    /** The base: walls at x = ±5, z = ±4; the airlock runs out of the south wall (z = 4) to the outer door at z = 7. */
    private static final int HALF_X = 5, HALF_Z = 4, OUTER_Z = 7, ROOF = 4;
    private int g = 101;

    /** The flattened site's walking level; shots() runs before build(), so both work it out from the terrain. */
    private static int ground(ServerLevel level) {
        return level != null && level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen
                ? (int) Math.floor(gen.terrain().height(0.5, 0.5)) + 1 : 101;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return MoonModule.MOON;
    }

    @Override
    public void build(ServerLevel level) {
        g = ground(level);
        BlockState ground = MoonModule.LUNAR_REGOLITH.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // a flat site
        for (int x = -16; x <= 16; x++) {
            for (int z = -12; z <= 24; z++) {
                for (int dy = -3; dy < 12; dy++) {
                    level.setBlock(new BlockPos(x, g + dy, z), dy < 0 ? ground : air, Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockState panel = BaseBlocks.ALUMINUM_PANEL.base().get().defaultBlockState();
        BlockState deck = BaseBlocks.TITANIUM_DECK.base().get().defaultBlockState();
        BlockState stripe = BaseBlocks.HAZARD_STRIPE.base().get().defaultBlockState();
        // the main room: aluminium shell, titanium deck, LED ceiling, a pressure-glass window and portholes
        for (int x = -HALF_X; x <= HALF_X; x++) {
            for (int z = -HALF_Z; z <= HALF_Z; z++) {
                for (int dy = -1; dy <= ROOF; dy++) {
                    boolean wall = Math.abs(x) == HALF_X || Math.abs(z) == HALF_Z;
                    BlockState s = dy == -1 ? deck : dy == ROOF ? panel : wall ? panel : air;
                    if (dy == ROOF && Math.abs(x) <= 2 && Math.abs(z) <= 1 && (x + z) % 2 == 0) {
                        s = BaseBlocks.LED_PANEL.get().defaultBlockState();
                    }
                    level.setBlock(new BlockPos(x, g + dy, z), s, Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            if (x != 0) {
                level.setBlock(new BlockPos(x, g + 2, -HALF_Z), BaseBlocks.PRESSURE_GLASS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int z = -2; z <= 2; z += 2) {
            level.setBlock(new BlockPos(HALF_X, g + 2, z), BaseBlocks.PORTHOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(new BlockPos(-HALF_X, g + 2, z), BaseBlocks.PORTHOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // the airlock: 3 wide, 2 deep, 3 high, between the two doors
        for (int x = -2; x <= 2; x++) {
            for (int z = HALF_Z + 1; z <= OUTER_Z; z++) {
                for (int dy = -1; dy <= 3; dy++) {
                    boolean wall = Math.abs(x) == 2 || z == OUTER_Z;
                    BlockState s = dy == -1 ? deck : dy == 3 ? panel : wall ? (z == OUTER_Z && Math.abs(x) == 1 ? stripe : panel) : air;
                    level.setBlock(new BlockPos(x, g + dy, z), s, Block.UPDATE_CLIENTS);
                }
            }
        }
        level.setBlock(new BlockPos(0, g + 2, OUTER_Z), stripe, Block.UPDATE_CLIENTS);
        level.setBlock(new BlockPos(0, g + 2, HALF_Z + 1), BaseBlocks.LED_PANEL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        // the doors (both facing along z, three blocks apart: they make one airlock)
        BlockState door = BaseBlocks.SLIDING_DOOR.get().defaultBlockState().setValue(SlidingDoorBlock.FACING, Direction.SOUTH);
        SlidingDoorBlock.place(level, new BlockPos(0, g, HALF_Z), door);
        SlidingDoorBlock.place(level, new BlockPos(0, g, OUTER_Z), door);
        // the warning light over the outer door, and a landing apron of deck plates in front of it
        level.setBlock(new BlockPos(0, g + 3, OUTER_Z + 1), RocketryModule.WARNING_LIGHT.get().defaultBlockState()
                .setValue(WarningLightBlock.FACING, Direction.SOUTH).setValue(WarningLightBlock.LIT, true)
                .setValue(WarningLightBlock.POWERED, true), Block.UPDATE_ALL);
        for (int x = -2; x <= 2; x++) {
            for (int z = OUTER_Z + 1; z <= OUTER_Z + 3; z++) {
                level.setBlock(new BlockPos(x, g - 1, z), Math.abs(x) == 2 ? stripe : deck, Block.UPDATE_CLIENTS);
            }
        }
        // the oxygen distributor in the corner, with a full large tank
        BlockPos d = new BlockPos(-3, g, -2);
        level.setBlock(d, SuitModule.OXYGEN_DISTRIBUTOR.get().defaultBlockState().setValue(GasMachineBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        if (level.getBlockEntity(d) instanceof OxygenDistributorBlockEntity be) {
            be.setStack(OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        }
        level.setBlock(new BlockPos(-3, g, -3), SuitModule.SUIT_STATION.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Sets both doors directly (no cycle delay), so each shot starts from a known airlock state. */
    private void doors(ServerLevel level, boolean innerOpen, boolean outerOpen) {
        SlidingDoorBlock block = BaseBlocks.SLIDING_DOOR.get();
        BlockPos inner = new BlockPos(0, g, HALF_Z), outer = new BlockPos(0, g, OUTER_Z);
        block.setOpen(level, inner, level.getBlockState(inner), innerOpen);
        block.setOpen(level, outer, level.getBlockState(outer), outerOpen);
    }

    private Consumer<ServerPlayer> at(boolean innerOpen, boolean outerOpen, boolean suited, boolean helmet) {
        return p -> {
            ServerLevel level = p.serverLevel();
            PlanetTime.setDayFraction(level, 0.2);
            doors(level, innerOpen, outerOpen);
            p.setGameMode(suited ? GameType.SURVIVAL : GameType.CREATIVE);
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            SuitScenario.dress(p, suited ? SuitScenario.fullSuit(false, false)
                    : new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY});
            if (suited && !helmet) {
                p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
            p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        };
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        g = ground(net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().getLevel(MoonModule.MOON));
        double y = g;
        return List.of(
                // outside: the base, the outer door under its warning light
                new Shot("base_airlock_01_exterior", 0, 6.5, y + 2.2, 16.5, 150F, 10F, true, fp, 200,
                        at(false, false, false, true), SuitShots.CLEAR_TOASTS),
                // in the airlock with the outer door open: vacuum on the HUD
                new Shot("base_airlock_02_vacuum", 0, 0.5, y + 1.62, 5.6, 0F, 4F, false, fp, 120,
                        at(false, true, true, true), SuitShots.CLEAR_TOASTS),
                // the outer door shut and the inner one open: the base air fills the airlock, the HUD reads pressurised
                new Shot("base_airlock_03_pressurised", 0, 0.5, y + 1.62, 6.4, 180F, 4F, false, fp, 300,
                        at(true, false, true, true), SuitShots.CLEAR_TOASTS),
                // inside the base, helmet off by the distributor and the suit station
                new Shot("base_airlock_04_helmet_off", 0, 2.0, y + 1.62, 2.5, 125F, 14F, false, fp, 200,
                        at(false, false, true, false), SuitShots.CLEAR_TOASTS),
                // suited, looking at the distributor: the small helmet gauge stays clear of block-info tooltips (top centre)
                new Shot("base_airlock_05_hud_info", 0, -1.2, y + 1.62, 0.2, 135F, 30F, false, fp, 120,
                        at(false, false, true, true), SuitShots.CLEAR_TOASTS));
    }
}
