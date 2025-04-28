package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.catlib.util.RandomChunkSelector;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record PredictableChunkFilter(double threshold, int seed) implements ChunkFilter {
    private static final double HASHER_SCALE = 91.0;
    private static final RandomChunkSelector SELECTOR = RandomChunkSelector.DEFAULT;
    public static final MapCodec<PredictableChunkFilter> CODEC = codecOf(
        defaulted(Codec.doubleRange(0, 1), "chance", 0.75, PredictableChunkFilter::probability),
        defaulted(Codec.INT, "seed", 0, PredictableChunkFilter::seed),
        PredictableChunkFilter::fromProbability
    );

    public static PredictableChunkFilter fromProbability(double probability, int seed) {
        return new PredictableChunkFilter(probability * HASHER_SCALE, seed);
    }

    public double probability() {
        return this.threshold / HASHER_SCALE;
    }

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        final int id = this.seed + ctx.featureIndex.get();
        return SELECTOR.testCoordinates(ctx.seed, id, x, z, this.threshold);
    }

    @Override
    public MapCodec<PredictableChunkFilter> codec() {
        return CODEC;
    }
}
