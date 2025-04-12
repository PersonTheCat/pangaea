package personthecat.pangaea.world.feature;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.world.road.RoadRegion;
import personthecat.pangaea.world.road.RoadVertex;

@Log4j2
public class RoadFeature extends Feature<NoneFeatureConfiguration> {
    public static final RoadFeature INSTANCE = new RoadFeature();

    private RoadFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        final var sampler = ((ServerChunkCache) ctx.level().getChunkSource()).randomState().sampler();
        final var fCtx = MutableFunctionContext.from(ctx.origin()).at(ctx.chunkGenerator().getSeaLevel());
        final var level = ctx.level().getLevel();
        final int aX = fCtx.blockX() & ~15;
        final int aZ = fCtx.blockZ() & ~15;
        final short rX = RoadRegion.absToRegion(aX);
        final short rZ = RoadRegion.absToRegion(aZ);
        final var region = LevelExtras.getRoadMap(level).getRegion(sampler, rX, rZ);
        for (final var network : region) {
            if (!network.containsPoint(aX, aZ)) {
                continue;
            }
            for (final var road : network.roads) {
                if (!road.containsPoint(aX, aZ)) {
                    continue;
                }
                for (final var vertex : road.vertices()) {
                    final double d = Utils.distance(vertex.x, vertex.z, aX + 8, aZ + 8);
                    if (d < vertex.radius + 16) {
                        this.trace(ctx.level(), vertex, ctx.random(), road.level(), aX, aZ);
                    }
                }
            }
        }
        return true;
    }

    private void trace(WorldGenLevel level, RoadVertex vertex, RandomSource rand, int l, int aX, int aZ) {
        final MutableBlockPos pos = new MutableBlockPos(aX, 0, aZ);
        for (int x = aX; x < aX + 16; x++) {
            final int dX2 = (x - vertex.x) * (x - vertex.x);
            for (int z = aZ; z < aZ + 16; z++) {
                final int dZ2 = (z - vertex.z) * (z - vertex.z);
                if (Math.sqrt(dX2 + dZ2) < vertex.radius) {
                    if (vertex.integrity == 1 || rand.nextFloat() <= vertex.integrity) {
                        final int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                        level.setBlock(pos.set(x, y, z), this.temporaryGetBlock(l), 2);
                        if (level.getBlockState(pos.setY(y + 1)).is(Blocks.CAVE_AIR)) {
                            log.info("Cave air @ {}", pos);
                        }
                    }
                }
            }
        }
    }

    private BlockState temporaryGetBlock(int l) {
        return switch (l) {
          case 0 -> Blocks.GRAVEL.defaultBlockState();
          case 1 -> Blocks.DIRT_PATH.defaultBlockState();
          default -> Blocks.COARSE_DIRT.defaultBlockState();
        };
    }
}
