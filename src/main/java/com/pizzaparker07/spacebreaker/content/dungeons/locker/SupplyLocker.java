package com.pizzaparker07.spacebreaker.content.dungeons.locker;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/** The shared supply locker: registration and a helper for dungeon pieces. */
public final class SupplyLocker {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredBlock<SupplyLockerBlock> BLOCK = MarsModule.KIT.block("supply_locker", SupplyLockerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyLockerBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("supply_locker", () -> BlockEntityType.Builder.of(SupplyLockerBlockEntity::new, BLOCK.get()).build(null));

    private SupplyLocker() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    /**
     * Places a locker in a dungeon piece at piece cell (x, y, z), its doors facing {@code facing} (piece space), filled from
     * {@code loot} when first opened. Returns the world position, or null outside the chunk being built.
     */
    public static @Nullable BlockPos place(WorldGenLevel level, StructurePiece piece, int x, int y, int z, Direction facing,
                                           ResourceKey<LootTable> loot, long seed, BoundingBox box) {
        BlockPos pos = PieceOrientation.place(level, piece, BLOCK.get().defaultBlockState().setValue(SupplyLockerBlock.FACING, facing), x, y, z, box);
        if (pos != null && level.getBlockEntity(pos) instanceof SupplyLockerBlockEntity locker) {
            locker.setLootTable(loot, seed);
        }
        return pos;
    }
}
