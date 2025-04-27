package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public record IntervalChunkFilter(int interval) implements ChunkFilter {
    public static final MapCodec<IntervalChunkFilter> CODEC =
        Codec.INT.optionalFieldOf("interval", 5)
            .xmap(IntervalChunkFilter::new, IntervalChunkFilter::interval);

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        return x % this.interval == 0 && z % this.interval == 0;
    }

    @Override
    public MapCodec<IntervalChunkFilter> codec() {
        return CODEC;
    }
}
