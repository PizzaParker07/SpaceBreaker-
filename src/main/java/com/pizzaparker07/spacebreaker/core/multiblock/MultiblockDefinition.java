package com.pizzaparker07.spacebreaker.core.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A multiblock structure, loaded from {@code data/<ns>/spacebreaker/multiblock/<id>.json}. The same files feed
 * structure validation (machines check whether they are formed), the Hologram Projector and the guide's 3D view.
 *
 * <pre>{@code
 * {
 *   "name": "multiblock.spacebreaker.launch_site",     // language key (defaults to multiblock.<ns>.<path>)
 *   "controller": "C",                                  // the key character of the block that "owns" the structure
 *   "layers": [                                         // bottom layer first; each layer is rows north -> south,
 *     ["  T  ", " PCP "],                               // each row is columns west -> east (authored facing north)
 *     ["  T  ", "     "]
 *   ],
 *   "key": { "P": {"block": "ns:pad"}, "C": {"block": "ns:core"}, "T": {"tag": "ns:towers"} },
 *   "tech": "spacebreaker:launch_infrastructure",        // optional: technology that teaches it
 *   "self_assembling": false                            // optional: true when placing the controller builds
 *                                                       // the rest itself (projectors then show only the controller
 *                                                       // and the space that must be free)
 * }
 * }</pre>
 * A space means "anything", {@code _} means "must be air"; every other character must be in {@code key}.
 * All rows of all layers must have the same length, every layer the same number of rows, and exactly one
 * controller character must appear.
 */
public record MultiblockDefinition(Optional<String> name, String controller, List<List<String>> layers, Map<String, BlockSpec> key,
                                   Optional<ResourceLocation> tech, boolean selfAssembling) {
    public static final Codec<MultiblockDefinition> CODEC = RecordCodecBuilder.<MultiblockDefinition>create(i -> i.group(
            Codec.STRING.optionalFieldOf("name").forGetter(MultiblockDefinition::name),
            Codec.STRING.fieldOf("controller").forGetter(MultiblockDefinition::controller),
            Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(MultiblockDefinition::layers),
            Codec.unboundedMap(Codec.STRING, BlockSpec.CODEC).fieldOf("key").forGetter(MultiblockDefinition::key),
            ResourceLocation.CODEC.optionalFieldOf("tech").forGetter(MultiblockDefinition::tech),
            Codec.BOOL.optionalFieldOf("self_assembling", false).forGetter(MultiblockDefinition::selfAssembling)
    ).apply(i, MultiblockDefinition::new)).validate(MultiblockDefinition::check);

    /** Source-compatible form without the self-assembling flag. */
    public MultiblockDefinition(Optional<String> name, String controller, List<List<String>> layers, Map<String, BlockSpec> key,
                                Optional<ResourceLocation> tech) {
        this(name, controller, layers, key, tech, false);
    }

    /** One position of the structure, already rotated and placed in the world. */
    public record Placement(BlockPos pos, int layer, char symbol, BlockSpec spec) {
    }

    public boolean isController(Placement p) {
        return String.valueOf(p.symbol()).equals(controller);
    }

    private static DataResult<MultiblockDefinition> check(MultiblockDefinition d) {
        if (d.layers.isEmpty() || d.layers.get(0).isEmpty()) {
            return DataResult.error(() -> "multiblock has no layers");
        }
        int rows = d.layers.get(0).size();
        int cols = d.layers.get(0).get(0).length();
        int controllers = 0;
        for (List<String> layer : d.layers) {
            if (layer.size() != rows) {
                return DataResult.error(() -> "every layer needs " + rows + " rows");
            }
            for (String row : layer) {
                if (row.length() != cols) {
                    return DataResult.error(() -> "every row needs " + cols + " columns: '" + row + "'");
                }
                for (char c : row.toCharArray()) {
                    if (c != ' ' && c != '_' && !d.key.containsKey(String.valueOf(c))) {
                        return DataResult.error(() -> "character '" + c + "' is not in the key");
                    }
                    if (String.valueOf(c).equals(d.controller)) {
                        controllers++;
                    }
                }
            }
        }
        if (controllers != 1) {
            int n = controllers;
            return DataResult.error(() -> "the controller '" + d.controller + "' must appear exactly once, found " + n);
        }
        return DataResult.success(d);
    }

    public String nameKey(ResourceLocation id) {
        return name.orElse("multiblock." + id.getNamespace() + "." + id.getPath());
    }

    public int height() {
        return layers.size();
    }

    public BlockSpec controllerSpec() {
        return key.get(controller);
    }

    /** Layer, row and column of the controller character. */
    private int[] controllerCell() {
        for (int y = 0; y < layers.size(); y++) {
            List<String> layer = layers.get(y);
            for (int z = 0; z < layer.size(); z++) {
                int x = layer.get(z).indexOf(controller);
                if (x >= 0) {
                    return new int[]{y, z, x};
                }
            }
        }
        throw new IllegalStateException("no controller");
    }

    public static Rotation rotation(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * Every non-blank position, rotated so that the structure's north side points to {@code facing}, with the
     * controller at {@code controllerPos}.
     */
    public List<Placement> placements(BlockPos controllerPos, Direction facing) {
        int[] c = controllerCell();
        Rotation rot = rotation(facing);
        List<Placement> out = new ArrayList<>();
        for (int y = 0; y < layers.size(); y++) {
            List<String> layer = layers.get(y);
            for (int z = 0; z < layer.size(); z++) {
                String row = layer.get(z);
                for (int x = 0; x < row.length(); x++) {
                    char ch = row.charAt(x);
                    if (ch == ' ') {
                        continue;
                    }
                    BlockSpec spec = ch == '_' ? BlockSpec.AIR : key.get(String.valueOf(ch));
                    BlockPos local = new BlockPos(x - c[2], y - c[0], z - c[1]).rotate(rot);
                    out.add(new Placement(controllerPos.offset(local), y, ch, spec));
                }
            }
        }
        return out;
    }
}
