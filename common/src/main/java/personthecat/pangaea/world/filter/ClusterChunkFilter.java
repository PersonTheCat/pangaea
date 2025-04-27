package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.catlib.util.RandomChunkSelector;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record ClusterChunkFilter(double threshold, int seed) implements ChunkFilter {
    private static final double HASHER_SCALE = 91.0;
    private static final RandomChunkSelector SELECTOR = RandomChunkSelector.DEFAULT;
    public static final MapCodec<ClusterChunkFilter> CODEC = codecOf(
        defaulted(Codec.DOUBLE, "probability", 0.75, ClusterChunkFilter::probability),
        defaulted(Codec.INT, "seed", 0, ClusterChunkFilter::seed),
        ClusterChunkFilter::fromProbability
    );

    public static ClusterChunkFilter fromProbability(double probability, int seed) {
        return new ClusterChunkFilter(probability * HASHER_SCALE, seed);
    }

    public double probability() {
        return this.threshold / HASHER_SCALE;
    }

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        final int id = ctx.featureIndex.get() + this.seed;
        return ctx.rand.nextDouble() <= SELECTOR.getProbability(ctx.seed, id, x, z, this.threshold);
    }

    @Override
    public MapCodec<ClusterChunkFilter> codec() {
        return CODEC;
    }
}
