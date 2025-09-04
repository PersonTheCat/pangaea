package personthecat.pangaea.world.road;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.road.AStarRoadGenerator.Configuration;
import personthecat.pangaea.world.weight.DefaultWeight;
import personthecat.pangaea.world.weight.WeightFunction;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public class AStarRoadGenerator extends RoadGenerator<Configuration> {
    private static final int ANGLE_SMOOTHING_RADIUS = 5;
    private final AStar aStar;

    private AStarRoadGenerator(ServerLevel level, RoadMap map, Configuration cfg) {
        super(level, map, cfg);
        this.aStar = new AStar(this.graph, cfg.weight);
    }

    @Override
    protected boolean isValidForChunk(PangaeaContext ctx, int cX, int cZ) {
        return this.cfg.chunkFilter.test(ctx, cX, cZ);
    }

    @Override
    protected double getWeight(PangaeaContext ctx, int x, int z) {
        return this.cfg.weight.compute(ctx, new MutableFunctionContext(x, 0, z));
    }

    @Override
    protected int getBranchCount(PangaeaContext ctx) {
        return this.cfg.branches.sample(ctx.rand);
    }

    @Override
    protected int getRoadLength(PangaeaContext ctx) {
        return this.cfg.roadLength.sample(ctx.rand);
    }

    @Override
    protected Road trace(PangaeaContext ctx, Point src, Destination dest) {
        this.aStar.reset();
        final List<Point> path = this.aStar.search(ctx, src, dest);
        if (path == null) {
            return null;
        }
        final int l = dest.getRoadLevel();
        final var radii = this.cfg.radii;
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        final int len = path.size();
        final RoadVertex[] vertices = new RoadVertex[len * 2 - 1];
        for (int i = len - 1; i >= 0; i--) {
            final Point p = path.get(i);
            final float theta;
            final float xAngle;
            if (i > 0 && i < len - 1) {
                // Check ahead by 2, if possible. Lazy way to get the angle over a large distance
                final Point prev = i < len - 2 ? path.get(i + 2) : path.get(i + 1);
                final Point next = i > 1 ? path.get(i - 2) : path.getFirst(); // i - 1 = 0
                final float a1 = (float) Math.atan2(prev.z - p.z, prev.x - p.x);
                final float a2 = (float) Math.atan2(next.z - p.z, next.x - p.x);
                final float t = a2 - a1;
                theta = t < 0 ? t + TAU : t;
                xAngle = a2;
            } else {
                theta = -1;
                xAngle = -1;
            }
            if (p.x < minX) minX = p.x;
            if (p.z < minZ) minZ = p.z;
            if (p.x > maxX) maxX = p.x;
            if (p.z > maxZ) maxZ = p.z;
            final byte radius;
            if (l >= radii.size()) {
                radius = (byte) radii.getLast().sample(ctx.rand);
            } else {
                radius = (byte) radii.get(l).sample(ctx.rand);
            }
            final RoadVertex v = new RoadVertex(p.x, p.z, radius, theta, xAngle, (short) 0);
            vertices[(path.size() - i - 1) * 2] = v;
            if (i == len - 1) {
                v.addFlag(RoadVertex.START);
            } else if (i == 0) {
                v.addFlag(RoadVertex.END);
            } else {
                v.addFlag(RoadVertex.MIDPOINT);
            }
        }
        interpolate(vertices);
        smoothAngles(vertices);
        return new Road((byte) l, minX, minZ, maxX, maxZ, vertices);
    }

    private static void interpolate(RoadVertex[] vertices) {
        for (int i = 0; i < vertices.length - 1; i += 2) {
            final var a = vertices[i];
            final var b = vertices[i + 2];
            vertices[i + 1] = new RoadVertex(
                (a.x + b.x) / 2,
                (a.z + b.z) / 2,
                (byte) ((a.radius + b.radius) / 2),
                averageAngle(a.theta, b.theta),
                averageAngle(a.xAngle, b.xAngle),
                RoadVertex.MIDPOINT
            );
        }
    }

    private static float averageAngle(float a, float b) {
        final float sin = Mth.sin(a) + Mth.sin(b);
        final float cos = Mth.cos(a) + Mth.cos(b);
        return (float) Mth.atan2(sin / 2, cos / 2);
    }

    private static void smoothAngles(RoadVertex[] vertices) {
        final float[] smoothed = new float[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            final int s = Math.max(0, i - ANGLE_SMOOTHING_RADIUS);
            final int e = Math.min(vertices.length - 1, i + ANGLE_SMOOTHING_RADIUS);
            float sin = 0;
            float cos = 0;
            int count = 0;

            for (int j = s; j <= e; j++) {
                sin += Mth.sin(vertices[j].xAngle);
                cos += Mth.cos(vertices[j].xAngle);
                count++;
            }
            smoothed[i] = (float) Mth.atan2(sin / count, cos / count);
        }
        for (int i = 0; i < smoothed.length; i++) {
            vertices[i].xAngle = smoothed[i];
        }
    }

    public record Configuration(
            ChunkFilter chunkFilter, WeightFunction weight, IntProvider branches,
            IntProvider roadLength, List<IntProvider> radii, DestinationStrategy destinationStrategy) implements RoadConfig {
        private static final List<IntProvider> DEFAULT_RADII =
            List.of(UniformInt.of(3, 4), UniformInt.of(2, 3));
        public static final MapCodec<Configuration> CODEC = codecOf(
            defaulted(ChunkFilter.CODEC, "chunk_filter", ChanceChunkFilter.of(1.0 / 400.0), Configuration::chunkFilter),
            defaulted(WeightFunction.CODEC, "weight", DefaultWeight.INSTANCE, Configuration::weight),
            defaulted(IntProvider.NON_NEGATIVE_CODEC, "branches", UniformInt.of(0, 15), Configuration::branches),
            defaulted(IntProvider.NON_NEGATIVE_CODEC, "length", UniformInt.of(400, 800), Configuration::roadLength),
            defaulted(IntProvider.codec(1, 8).listOf(), "radii", DEFAULT_RADII, Configuration::radii),
            defaulted(DestinationStrategy.CODEC, "destination_strategy", DestinationStrategy.DEFAULT, Configuration::destinationStrategy),
            Configuration::new
        );

        @Override
        public RoadGenerator<? extends RoadConfig> createGenerator(ServerLevel level, RoadMap map) {
            return new AStarRoadGenerator(level, map, this);
        }

        @Override
        public MapCodec<? extends RoadConfig> codec() {
            return CODEC;
        }
    }
}
