package com.pizzaparker07.spacebreaker.content.fluids;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One Spacebreaker fluid: its FluidType, source/flowing fluids and, for liquids, a world block and bucket.
 * Gases are fluids without a block (they live in pipes, tanks and canisters).
 */
public final class SbFluid {
    /** PROCESS = a liquid that only lives in tanks, pipes and canisters (no world block or bucket). */
    public enum Kind { LIQUID, CRYOGENIC, GAS, PROCESS }

    public final String name;
    public final String en;
    public final String tr;
    public final Kind kind;
    /** ARGB tint applied to the (vanilla water) fluid textures. */
    public final int tint;
    /** Temperature in kelvin. */
    public final int temperature;
    public final List<TagKey<Fluid>> tags;

    public final DeferredHolder<FluidType, FluidType> type;
    public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
    public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
    @Nullable public final DeferredBlock<LiquidBlock> block;
    @Nullable public final DeferredItem<BucketItem> bucket;

    SbFluid(String name, String en, String tr, Kind kind, int tint, int temperature, int density, int viscosity, List<TagKey<Fluid>> tags) {
        this.name = name;
        this.en = en;
        this.tr = tr;
        this.kind = kind;
        this.tint = tint;
        this.temperature = temperature;
        this.tags = tags;
        boolean gas = kind == Kind.GAS;
        this.type = FluidsModule.FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type.spacebreaker." + name)
                .density(density).viscosity(viscosity).temperature(temperature)
                .canSwim(!gas).canDrown(!gas).canExtinguish(kind == Kind.CRYOGENIC).supportsBoating(false)
                .sound(SoundActions.BUCKET_FILL, kind == Kind.LIQUID ? net.minecraft.sounds.SoundEvents.BUCKET_FILL : net.minecraft.sounds.SoundEvents.BUCKET_FILL_POWDER_SNOW)
                .sound(SoundActions.BUCKET_EMPTY, kind == Kind.LIQUID ? net.minecraft.sounds.SoundEvents.BUCKET_EMPTY : net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_POWDER_SNOW)));
        this.source = FluidsModule.FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
        this.flowing = FluidsModule.FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
        if (gas || kind == Kind.PROCESS) {
            this.block = null;
            this.bucket = null;
        } else {
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of().mapColor(kind == Kind.LIQUID ? MapColor.COLOR_BLACK : MapColor.ICE)
                    .replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid();
            this.block = kind == Kind.CRYOGENIC
                    ? FluidsModule.BLOCKS.registerBlock(name, p -> new CryogenicLiquidBlock(source.get(), p), props.randomTicks())
                    : FluidsModule.BLOCKS.registerBlock(name, p -> new LiquidBlock(source.get(), p), props);
            this.bucket = FluidsModule.ITEMS.registerItem(name + "_bucket", p -> new BucketItem(source.get(), p),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
        }
    }

    private BaseFlowingFluid.Properties properties() {
        BaseFlowingFluid.Properties p = new BaseFlowingFluid.Properties(type, source, flowing);
        if (block != null) {
            p.block(block).bucket(bucket);
        }
        if (kind == Kind.LIQUID) {
            p.slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(20);
        } else {
            p.slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5);
        }
        return p;
    }

    public boolean isGas() {
        return kind == Kind.GAS;
    }

    public boolean isCryogenic() {
        return kind == Kind.CRYOGENIC;
    }

    public Fluid get() {
        return source.get();
    }

    public FluidStack stack(int amount) {
        return new FluidStack(source.get(), amount);
    }
}
