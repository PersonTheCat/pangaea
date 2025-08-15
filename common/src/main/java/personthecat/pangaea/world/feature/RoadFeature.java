package personthecat.pangaea.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import personthecat.catlib.data.Range;
import personthecat.pangaea.data.NeighborGraph.NodeResult;
import personthecat.pangaea.data.VertexGraph.VertexNode;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.feature.RoadFeature.Configuration;
import personthecat.pangaea.world.level.BlockUpdates;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacerList;
import personthecat.pangaea.world.placer.ChanceBlockPlacer;
import personthecat.pangaea.world.placer.UnconditionalBlockPlacer;
import personthecat.pangaea.world.road.RoadRegion;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class RoadFeature extends GiantFeature<Configuration> {
    public static final RoadFeature INSTANCE = new RoadFeature();

    private RoadFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected void place(PangaeaContext ctx, Configuration cfg, ChunkPos pos, Border border) {
        final var aX = ctx.actualX;
        final var aZ = ctx.actualZ;
        final var rX = RoadRegion.absToRegion(aX);
        final var rZ = RoadRegion.absToRegion(aZ);
        final var region = LevelExtras.getRoadMap(ctx.level.getLevel()).getRegion(rX, rZ);
        for (final var network : region) {
            if (!network.containsPoint(aX, aZ)) {
                continue;
            }
            final var nodes = network.graph.getAll(aX + 8, aZ + 8, 18);
            this.trace(ctx, cfg, aX, aZ, nodes, false);
            this.trace(ctx, cfg, aX, aZ, nodes, true);
        }
    }

    private void trace(
            PangaeaContext ctx, Configuration cfg, int aX, int aZ, List<NodeResult<VertexNode>> nodes, boolean place) {
        final var configs = cfg.vertexConfigs;
        for (final var n : nodes) {
            final VertexConfig vc;
            if (n.n().level() >= configs.size()) {
                vc = configs.getLast();
            } else {
                vc = configs.get(n.n().level());
            }
            this.placeOrCarve(ctx, vc, aX, aZ, n, place);
        }
    }

    private void placeOrCarve(
            PangaeaContext ctx, VertexConfig vc, int aX, int aZ, NodeResult<VertexNode> n, boolean place) {
        final int r = n.n().radius();
        final int minX = Math.max(aX, n.x() - r);
        final int maxX = Math.min(aX + 15, n.x() + r);
        final int minZ = Math.max(aZ, n.z() - r);
        final int maxZ = Math.min(aZ + 15, n.z() + r);
        final int cY = vc.bounds.clamp(ctx.getHeightChecked(n.x(), n.z()));

        for (int x = minX; x <= maxX; x++) {
            final int dX2 = (x - n.x()) * (x - n.x());
            for (int z = minZ; z <= maxZ; z++) {
                final int dZ2 = (z - n.z()) * (z - n.z());
                if (Math.sqrt(dX2 + dZ2) >= r + 1) {
                    continue;
                }
                if (place) {
                    // place ground / path below cy
                    final int h = cY - r / 2;
                    for (int y = h - r; y <= h + r / 2; y++) {
                        final int dY2 = (y - h) * (y - h);
                        if (Math.sqrt(dX2 + dZ2 + dY2) >= r) {
                            continue;
                        }
                        if (y >= h + (r / 2)) { // is top
                            if (Math.sqrt(dX2 + dZ2 + dY2) > r / 2.0 + 1) {
                                continue;
                            }
                            vc.path.place(ctx, x, y, z, BlockUpdates.HEIGHTMAP);
                        } else {
                            vc.ground.place(ctx, x, y, z, BlockUpdates.HEIGHTMAP);
                        }
                    }
                } else {
                    // carve air above cy
                    final int h = cY + r + 1;
                    for (int y = h - r; y<= h + r; y++) {
                        final int dY2 = (y - h) * (y - h);
                        if (Math.sqrt(dX2 + dZ2 + dY2) >= r + 1) {
                            continue;
                        }
                        vc.air.place(ctx, x, y, z);
                    }
                }
            }
        }
    }

    public static class Configuration extends GiantFeatureConfiguration {
        public static final MapCodec<Configuration> CODEC = PangaeaCodec.buildMap(cat -> codecOf(
            cat.defaulted(VertexConfig.CODEC.codec().listOf(0, 127), "vertex_configs", List.of(VertexConfig.DEFAULTS, VertexConfig.DEFAULT_2), c -> c.vertexConfigs),
            union(GiantFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        ));
        public final List<VertexConfig> vertexConfigs;

        public Configuration(
                List<VertexConfig> vertexConfigs,
                GiantFeatureConfiguration parent) {
            super(parent, 0, false);
            this.vertexConfigs = vertexConfigs;
        }
    }

    public record VertexConfig(BlockPlacer path, BlockPlacer ground, BlockPlacer air, Range bounds) {
        private static final VertexConfig DEFAULTS = new VertexConfig(
            new BlockPlacerList(List.of(
                new ChanceBlockPlacer(0.75, new UnconditionalBlockPlacer(Blocks.GRAVEL.defaultBlockState())),
                new UnconditionalBlockPlacer(Blocks.STONE.defaultBlockState())
            )),
            new UnconditionalBlockPlacer(Blocks.STONE.defaultBlockState()),
            new UnconditionalBlockPlacer(Blocks.AIR.defaultBlockState()),
            Range.of(62, 90));
        private static final VertexConfig DEFAULT_2 = new VertexConfig(
            new BlockPlacerList(List.of(
                new ChanceBlockPlacer(0.5, new UnconditionalBlockPlacer(Blocks.GRAVEL.defaultBlockState())),
                new UnconditionalBlockPlacer(Blocks.COARSE_DIRT.defaultBlockState())
            )),
            new UnconditionalBlockPlacer(Blocks.GRASS_BLOCK.defaultBlockState()),
            new UnconditionalBlockPlacer(Blocks.AIR.defaultBlockState()),
            Range.of(62, 90));
        public static final MapCodec<VertexConfig> CODEC = PangaeaCodec.buildMap(cat -> codecOf(
            cat.defaulted(BlockPlacer.CODEC, "path", DEFAULTS.path, VertexConfig::path),
            cat.defaulted(BlockPlacer.CODEC, "ground", DEFAULTS.ground, VertexConfig::ground),
            cat.defaulted(BlockPlacer.CODEC, "air", DEFAULTS.air, VertexConfig::air),
            cat.defaulted(Range.CODEC, "bounds", DEFAULTS.bounds, VertexConfig::bounds),
            VertexConfig::new
        ));
    }
}
