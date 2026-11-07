package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.Tether;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/**
 * The suit at work, on a planet surface or on a station hull in orbit: every tier by day and by night
 * (helmet lamps, their beams and light pool), the safety tether, and the Maneuvering Unit firing.
 */
final class SuitEvaScenario implements ShowcaseScenario {
    private final ResourceKey<Level> dimension;
    private final String prefix;
    private final java.util.function.Supplier<BlockState> floor;
    private double ground = Double.NaN;

    SuitEvaScenario(ResourceKey<Level> dimension, String prefix, java.util.function.Supplier<BlockState> floor) {
        this.dimension = dimension;
        this.prefix = prefix;
        this.floor = floor;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    private double ground() {
        if (Double.isNaN(ground)) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension);
            ground = level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen
                    ? Math.floor(gen.terrain().height(0.5, 0.5)) + 1 : 101;
        }
        return ground;
    }

    @Override
    public void build(ServerLevel level) {
        ground = Double.NaN;
        boolean planet = level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator;
        ground = planet ? Math.floor(((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain().height(0.5, 0.5)) + 1 : 101;
        int y = (int) ground;
        for (int x = -10; x <= 10; x++) {
            for (int z = -6; z <= 12; z++) {
                for (int dy = 0; dy < 10; dy++) {
                    level.setBlock(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                // planets: flattened ground; orbit: a station hull plate (iron, with darker seams)
                BlockState s = planet ? floor.get() : ((x + 100) % 4 == 0 || (z + 100) % 4 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.IRON_BLOCK).defaultBlockState();
                for (int dy = 1; dy <= (planet ? 3 : 1); dy++) {
                    level.setBlock(new BlockPos(x, y - dy, z), s, Block.UPDATE_CLIENTS);
                }
            }
        }
        // a suit maintenance station, its front towards the camera side, with the oxygen filler beside it
        level.setBlock(new BlockPos(-7, y, 4), com.pizzaparker07.spacebreaker.content.suit.SuitModule.SUIT_STATION.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlock.FACING, net.minecraft.core.Direction.SOUTH), Block.UPDATE_CLIENTS);
        level.setBlock(new BlockPos(-8, y, 4), com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_FILLER.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlock.FACING, net.minecraft.core.Direction.SOUTH), Block.UPDATE_CLIENTS);
        // a handrail post to clip the tether to
        level.setBlock(new BlockPos(6, y, 3), Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(new BlockPos(6, y + 1, 3), Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int i = 0; i < 4; i++) {
            stand(level, -4.5 + i * 3, y, 0.5, SuitTiersScenario.SETS[i]);
        }
    }

    private static void stand(ServerLevel level, double x, int y, double z, List<? extends java.util.function.Supplier<? extends net.minecraft.world.item.Item>> set) {
        ArmorStand stand = new ArmorStand(level, x, y, z);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", true);
        tag.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(tag);
        stand.setLeftArmPose(new Rotations(-10F, 0F, -8F));
        stand.setRightArmPose(new Rotations(-20F, 0F, 8F));
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            stand.setItemSlot(slots[i], new ItemStack(set.get(i).get()));
        }
        level.addFreshEntity(stand);
    }

    private Consumer<ServerPlayer> at(boolean night, Consumer<ServerPlayer> more) {
        return p -> {
            ServerLevel level = p.serverLevel();
            PlanetTime.setDayFraction(level, night ? 0.75 : 0.2);
            if (!(level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator)) {
                level.setDayTime(night ? 18000 : 6000);
            }
            Tether.release(p, "message.spacebreaker.tether.released");
            LifeSupport.data(p).thrusting = false;
            p.setGameMode(GameType.CREATIVE);
            SuitScenario.dress(p, new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY});
            p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (more != null) {
                more.accept(p);
            }
        };
    }

    /** The player as a suited astronaut standing on the ground (the camera follows in third person). */
    private static void astronaut(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        SuitScenario.dress(p, SuitScenario.fullSuit(false, true));
    }

    private void tethered(ServerPlayer p) {
        astronaut(p);
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SuitModule.SAFETY_TETHER.get()));
        Tether.clip(p, BlockPos.containing(6, ground() + 1, 3));
    }

    private static void thrusting(ServerPlayer p) {
        astronaut(p);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        var data = LifeSupport.data(p);
        data.thrusting = true;
        data.thrustDir = new Vec3(0.6, 0.3, -0.75).normalize();
    }

    @Override
    public List<Shot> shots() {
        double g = ground();
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        CameraType back = CameraType.THIRD_PERSON_BACK;
        CameraType front = CameraType.THIRD_PERSON_FRONT;
        s.add(new Shot(prefix + "_01_tiers_day", 0, 0.0, g + 2.0, 7.0, 180F, 9F, true, fp, 200, at(false, null), null));
        s.add(new Shot(prefix + "_02_close_mk1_thermal", 0, -3.0, g + 1.75, 3.0, 180F, 5F, true, fp, 40, at(false, null), null));
        s.add(new Shot(prefix + "_03_close_heated_radiation", 0, 3.0, g + 1.75, 3.0, 180F, 5F, true, fp, 40, at(false, null), null));
        s.add(new Shot(prefix + "_04_tiers_back", 0, 0.0, g + 2.0, -5.5, 0F, 9F, true, fp, 40, at(false, null), null));
        s.add(new Shot(prefix + "_05_tiers_night", 0, 0.0, g + 2.0, 7.0, 180F, 9F, true, fp, 60, at(true, null), null));
        s.add(new Shot(prefix + "_06_lamps_pool", 0, 0.5, g + 1.62, 9.5, 180F, 35F, true, fp, 60, at(true, SuitEvaScenario::astronaut), null));
        s.add(new Shot(prefix + "_07_lamps_front", 0, 0.5, g + 1.62, 4.5, 0F, 5F, true, front, 40, at(true, SuitEvaScenario::astronaut), null));
        s.add(new Shot(prefix + "_08_tether", 0, -2.5, g + 1.62, 3.5, 0F, 8F, true, front, 60, at(false, this::tethered), null));
        s.add(new Shot(prefix + "_09_mmu", 0, -1.5, g + 4.0, 8.0, 150F, 10F, true, back, 40, at(false, SuitEvaScenario::thrusting), null));
        s.add(new Shot(prefix + "_10_hud", 0, 1.5, g + 1.62, 9.5, 220F, 12F, false, fp, 40, at(false, this::tethered), null));
        // lamp glow cones from the side (they point the way the helmets face, +z), close to the Mk1 stand
        s.add(new Shot(prefix + "_11_lamp_cones", 0, -6.8, g + 1.85, 1.6, -100F, 6F, true, fp, 60, at(true, null), null));
        // first person in the cold beyond the suit's rating (Titan's dense air with a Mk1 suit): rime on the visor
        s.add(new Shot(prefix + "_12_visor_frost", 0, 0.5, g + 1.62, 8.0, 180F, 8F, false, fp, 260, at(false, SuitEvaScenario::astronaut), null));
        // the same view as shot 06 with the lamps on long beam: a narrow pool far ahead
        s.add(new Shot(prefix + "_13_long_beam", 0, 0.5, g + 1.62, 9.5, 180F, 12F, true, fp, 60, at(true, p -> {
            astronaut(p);
            p.getItemBySlot(EquipmentSlot.HEAD).set(SuitModule.LONG_BEAM.get(), true);
        }), null));
        s.add(new Shot(prefix + "_14_wide_beam", 0, 0.5, g + 1.62, 9.5, 180F, 12F, true, fp, 60, at(true, SuitEvaScenario::astronaut), null));
        s.add(new Shot(prefix + "_15_suit_station", 0, -6.0, g + 1.9, 7.0, 160F, 22F, true, fp, 40, at(false, null), null));
        s.add(new Shot(prefix + "_16_sensor", 0, 0.5, g + 1.62, 8.5, 180F, 10F, false, fp, 60, at(false, p -> {
            astronaut(p);
            p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SuitModule.ENVIRONMENT_SENSOR.get()));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,
                    com.pizzaparker07.spacebreaker.content.suit.EnvironmentSensorItem.read(p.serverLevel(), p.blockPosition()));
        }), null));
        // the prospecting kit in the hotbar, the charged drill in hand
        s.add(new Shot(prefix + "_17_tools", 0, 0.5, g + 1.62, 8.5, 180F, 25F, false, fp, 60, at(false, p -> {
            astronaut(p);
            ItemStack drill = new ItemStack(SuitModule.ELECTRIC_DRILL.get());
            com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem.setEnergy(drill, 70000);
            p.getInventory().setItem(0, drill);
            p.getInventory().setItem(1, new ItemStack(SuitModule.ENVIRONMENT_SENSOR.get()));
            int slot = 2;
            for (var tool : com.pizzaparker07.spacebreaker.content.suit.TitaniumTools.all()) {
                p.getInventory().setItem(slot++, new ItemStack(tool.get()));
            }
            p.getInventory().selected = 0;
        }), null));
        return s;
    }
}
