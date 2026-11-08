package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenFillerBlockEntity;
import net.minecraft.client.CameraType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/**
 * The EVA suit: a row of armour stands in the full Mk1 suit from several angles (one with the gold sun
 * visor down and a Maneuvering Unit), the player in the suit, lamps at night and the helmet HUD.
 */
final class SuitScenario implements ShowcaseScenario {
    static final int FLOOR = ShowcaseBuilder.FLOOR;
    private static final double G = FLOOR + 1;

    @Override
    public void build(ServerLevel level) {
        for (int x = -12; x <= 18; x++) {
            for (int z = -10; z <= 18; z++) {
                for (int y = FLOOR + 1; y <= FLOOR + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                boolean pad = Math.abs(x) <= 7 && z >= -3 && z <= 3;
                level.setBlock(new BlockPos(x, FLOOR, z), pad ? Blocks.SMOOTH_STONE.defaultBlockState()
                        : MoonModule.LUNAR_REGOLITH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int x = -8; x <= 8; x++) {
            for (int y = FLOOR + 1; y <= FLOOR + 5; y++) {
                level.setBlock(new BlockPos(x, y, -5), MoonModule.MARE_BASALT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        buildHabitat(level);
        stand(level, -4.5, 0F, false, false);
        stand(level, -2.0, 35F, false, false);
        stand(level, 0.5, 90F, false, false);
        stand(level, 3.0, 180F, false, true);
        stand(level, 5.5, -20F, true, true);
    }

    /** A small sealed glass habitat east of the stage: distributor inside, airlock door, filler outside. */
    private static void buildHabitat(ServerLevel level) {
        int x0 = 9, x1 = 13, z0 = 5, z1 = 10, y0 = FLOOR, y1 = FLOOR + 4;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    boolean shell = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    boolean frame = (x == x0 || x == x1) && (z == z0 || z == z1) || y == y0 || y == y1;
                    var state = !shell ? Blocks.AIR.defaultBlockState()
                            : frame ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.GLASS.defaultBlockState();
                    level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos door = new BlockPos(11, FLOOR + 1, z1);
        var lower = SuitModule.AIRLOCK_DOOR.get().defaultBlockState().setValue(AirlockDoorBlock.FACING, Direction.NORTH);
        level.setBlock(door, lower, Block.UPDATE_CLIENTS);
        level.setBlock(door.above(), lower.setValue(AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        BlockPos d = new BlockPos(10, FLOOR + 1, 6);
        level.setBlock(d, SuitModule.OXYGEN_DISTRIBUTOR.get().defaultBlockState().setValue(GasMachineBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        if (level.getBlockEntity(d) instanceof OxygenDistributorBlockEntity be) {
            be.setStack(OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        }
        BlockPos f = new BlockPos(8, FLOOR + 1, 12);
        level.setBlock(f, SuitModule.OXYGEN_FILLER.get().defaultBlockState().setValue(GasMachineBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        if (level.getBlockEntity(f) instanceof OxygenFillerBlockEntity be) {
            be.setStack(new ItemStack(SuitModule.OXYGEN_TANK.get()));
            be.getEnergy().receiveEnergy(16000, false);
        }
        BlockPos f2 = new BlockPos(9, FLOOR + 1, 12);
        level.setBlock(f2, SuitModule.OXYGEN_FILLER.get().defaultBlockState().setValue(GasMachineBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        // a second airlock, open, on the side
        BlockPos side = new BlockPos(x1, FLOOR + 1, 8);
        var sideLower = SuitModule.AIRLOCK_DOOR.get().defaultBlockState().setValue(AirlockDoorBlock.FACING, Direction.EAST)
                .setValue(AirlockDoorBlock.OPEN, true);
        level.setBlock(side, sideLower, Block.UPDATE_CLIENTS);
        level.setBlock(side.above(), sideLower.setValue(AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        ArmorStand inside = new ArmorStand(level, 11.5, FLOOR + 1, 8.0);
        inside.setYRot(160F);
        inside.setYBodyRot(160F);
        dress(inside, fullSuit(false, false));
        inside.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); // helmet off inside the pressurised habitat
        level.addFreshEntity(inside);
    }

    static ItemStack[] fullSuit(boolean sunVisor, boolean mmu) {
        ItemStack helmet = new ItemStack(SuitModule.EVA_HELMET.get());
        if (sunVisor) {
            helmet.set(SuitModule.SUN_VISOR.get(), true);
        }
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        if (mmu) {
            SuitTanks.setManeuveringUnit(torso, ManeuveringUnitItem.filled(SuitModule.MANEUVERING_UNIT.get()));
        }
        return new ItemStack[]{helmet, torso, new ItemStack(SuitModule.EVA_LEGGINGS.get()), new ItemStack(SuitModule.EVA_BOOTS.get())};
    }

    static void dress(LivingEntity entity, ItemStack[] suit) {
        entity.setItemSlot(EquipmentSlot.HEAD, suit[0]);
        entity.setItemSlot(EquipmentSlot.CHEST, suit[1]);
        entity.setItemSlot(EquipmentSlot.LEGS, suit[2]);
        entity.setItemSlot(EquipmentSlot.FEET, suit[3]);
    }

    private static void stand(ServerLevel level, double x, float yaw, boolean sunVisor, boolean mmu) {
        ArmorStand stand = new ArmorStand(level, x, G, 0.5);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", true);
        tag.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(tag);
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
        stand.setLeftArmPose(new Rotations(-10F, 0F, -8F));
        stand.setRightArmPose(new Rotations(-10F, 0F, 8F));
        dress(stand, fullSuit(sunVisor, mmu));
        level.addFreshEntity(stand);
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new Shot("suit_01_lineup", 5000, 0.5, G + 2.0, 7.0, 180F, 8F, true, fp, 140, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_02_front_close", 5000, -4.5, G + 1.5, 2.6, 180F, 4F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_03_helmet_macro", 5000, -4.4, G + 1.75, 1.75, 175F, 8F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_04_three_quarter", 5000, -1.4, G + 1.5, 2.9, 190F, 6F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_05_back_plss", 5000, 3.0, G + 1.6, 2.7, 180F, 8F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_06_gold_visor_mmu", 5000, 5.0, G + 1.6, 2.6, 170F, 6F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_07_night_lamps", 17800, 0.5, G + 1.8, 5.0, 180F, 6F, true, fp, 80, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_08_player", 5200, 0.5, G + 1.62, 6.0, 0F, 0F, true, CameraType.THIRD_PERSON_FRONT, 40,
                SuitScenario::suitUp, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_09_player_back", 5200, 0.5, G + 1.62, 6.0, 0F, 0F, true, CameraType.THIRD_PERSON_BACK, 40,
                SuitScenario::suitUp, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_11_habitat", 6000, 11.0, G + 2.6, 15.5, 175F, 14F, true, fp, 80, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_12_machines_close", 6000, 9.2, G + 1.8, 14.6, 170F, 30F, true, fp, 40, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_13_airlock_open", 6000, 16.2, G + 2.0, 8.5, 90F, 10F, true, fp, 60, null, SuitShots.CLEAR_TOASTS));
        s.add(new Shot("suit_14_inventory", 6000, 0.5, G + 1.62, 7.0, 180F, 5F, false, fp, 30, SuitScenario::fillHotbar,
                SuitShots.clearToastsThen(ShowcaseDriver::openCreativeTab)));
        s.add(new Shot("suit_10_hud", 6000, 0.5, G + 1.62, 7.0, 180F, 5F, false, fp, 60, SuitScenario::suitUp, SuitShots.CLEAR_TOASTS));
        // the same view with the sun visor down: a light gold filter over the screen
        s.add(new Shot("suit_10_hud_sun_visor", 6000, 0.5, G + 1.62, 7.0, 180F, 5F, false, fp, 60,
                p -> { suitUp(p); dress(p, fullSuit(true, false)); }, SuitShots.CLEAR_TOASTS));
        return s;
    }

    private static void fillHotbar(ServerPlayer player) {
        var inv = player.getInventory();
        inv.setItem(0, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        inv.setItem(1, OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        inv.setItem(2, new ItemStack(SuitModule.EVA_HELMET.get()));
        inv.setItem(3, new ItemStack(SuitModule.EVA_CHESTPLATE.get()));
        inv.setItem(4, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
        inv.setItem(5, new ItemStack(SuitModule.EVA_BOOTS.get()));
        inv.setItem(6, new ItemStack(SuitModule.OXYGEN_FILLER_ITEM.get()));
        inv.setItem(7, new ItemStack(SuitModule.AIRLOCK_DOOR_ITEM.get()));
        inv.setItem(8, ManeuveringUnitItem.filled(SuitModule.MANEUVERING_UNIT.get()));
        player.inventoryMenu.broadcastChanges();
    }

    private static void suitUp(ServerPlayer player) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        dress(player, fullSuit(false, false));
        player.setItemSlot(EquipmentSlot.MAINHAND, OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
    }
}
