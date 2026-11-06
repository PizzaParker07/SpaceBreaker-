package com.pizzaparker07.spacebreaker.testmod;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.block.Footprint;
import com.pizzaparker07.spacebreaker.content.block.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.blockentity.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.content.item.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.item.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.registry.ModBlocks;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpacebreakerGameTests {
    private static final String EMPTY = "empty";

    // ------------------------------------------------------------------ regolith boot prints

    @GameTest(template = EMPTY)
    public static void regolithRecordsBootPrints(GameTestHelper helper) {
        BlockPos even = new BlockPos(2, 1, 2);   // x+z even -> left foot
        BlockPos odd = new BlockPos(3, 1, 2);    // x+z odd  -> right foot
        helper.setBlock(even, ModBlocks.LUNAR_REGOLITH.get());
        helper.setBlock(odd, ModBlocks.LUNAR_REGOLITH.get());
        Zombie walker = helper.spawn(EntityType.ZOMBIE, 2, 2, 2);
        walker.setYRot(-90F); // facing east

        for (BlockPos rel : new BlockPos[]{even, odd}) {
            BlockPos abs = helper.absolutePos(rel);
            BlockState state = helper.getLevel().getBlockState(abs);
            state.getBlock().stepOn(helper.getLevel(), abs, state, walker);
        }
        BlockState left = helper.getLevel().getBlockState(helper.absolutePos(even));
        BlockState right = helper.getLevel().getBlockState(helper.absolutePos(odd));
        boolean absEven = ((helper.absolutePos(even).getX() + helper.absolutePos(even).getZ()) & 1) == 0;
        helper.assertTrue(left.getValue(LunarRegolithBlock.PRINT) == (absEven ? Footprint.LEFT : Footprint.RIGHT), "first print has the wrong foot");
        helper.assertTrue(right.getValue(LunarRegolithBlock.PRINT) != left.getValue(LunarRegolithBlock.PRINT), "consecutive prints must alternate feet");
        helper.assertTrue(left.getValue(LunarRegolithBlock.FACING) == Direction.EAST, "print must point the way the walker faces");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void itemsDoNotLeavePrints(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.LUNAR_REGOLITH.get());
        ItemEntity item = helper.spawnItem(ModItems.RAW_BAUXITE.get(), 2.5F, 2.1F, 2.5F);
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        state.getBlock().stepOn(helper.getLevel(), abs, state, item);
        helper.assertBlockProperty(pos, LunarRegolithBlock.PRINT, Footprint.NONE);
        helper.succeed();
    }

    // ------------------------------------------------------------------ solar tracker physics

    @GameTest(template = EMPTY)
    public static void solarIrradianceModel(GameTestHelper helper) {
        helper.assertTrue(Math.abs(SolarPanelBlockEntity.irradiance(0F) - 1F) < 1e-4, "noon must give full output");
        helper.assertTrue(SolarPanelBlockEntity.irradiance(Mth.HALF_PI + 0.01F) == 0F, "no output after sunset");
        helper.assertTrue(SolarPanelBlockEntity.irradiance(-Mth.PI) == 0F, "no output at midnight");
        float morning = SolarPanelBlockEntity.irradiance((float) Math.toRadians(-45));
        float lowSun = SolarPanelBlockEntity.irradiance((float) Math.toRadians(-80));
        helper.assertTrue(morning > 0.8F && morning < 1F, "tracked morning sun should be strong but below noon: " + morning);
        helper.assertTrue(lowSun < morning, "sun near the horizon must give less than a higher sun");
        helper.assertTrue(SolarPanelBlockEntity.targetTilt((float) Math.toRadians(-80)) == -SolarPanelBlockEntity.MAX_TILT, "tracker must stop at its mechanical limit");
        helper.assertTrue(SolarPanelBlockEntity.targetTilt(Mth.PI) == 0F, "tracker stows flat at night");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "solar_noon", timeoutTicks = 100)
    public static void solarPanelChargesAtNoon(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.SOLAR_PANEL.get());
        helper.succeedWhen(() -> {
            SolarPanelBlockEntity be = helper.getBlockEntity(pos);
            helper.assertTrue(be.getEnergy().getEnergyStored() >= SolarPanelBlockEntity.PEAK_OUTPUT * 10, "panel not charging at noon");
        });
    }

    @GameTest(template = EMPTY, batch = "solar_night", timeoutTicks = 60)
    public static void solarPanelIdleAtNight(GameTestHelper helper) {
        helper.getLevel().setDayTime(18000);
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.SOLAR_PANEL.get());
        helper.runAfterDelay(40, () -> {
            SolarPanelBlockEntity be = helper.getBlockEntity(pos);
            helper.assertTrue(be.getEnergy().getEnergyStored() == 0, "panel produced power at midnight");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ equipment

    @GameTest(template = EMPTY)
    public static void oxygenTankStartsFullFromCreative(GameTestHelper helper) {
        ItemStack tank = OxygenTankItem.filled(ModItems.OXYGEN_TANK.get());
        helper.assertTrue(OxygenTankItem.getOxygen(tank) == OxygenTankItem.SMALL_CAPACITY, "filled tank is not full");
        helper.assertTrue(OxygenTankItem.getOxygen(new ItemStack(ModItems.OXYGEN_TANK.get())) == 0, "new tank must be empty");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void helmetSunVisorDefaultsUp(GameTestHelper helper) {
        helper.assertFalse(SpaceSuitItem.isSunVisorDown(new ItemStack(ModItems.EVA_HELMET.get())), "sun visor should start raised");
        helper.succeed();
    }

    private SpacebreakerGameTests() {
    }
}
