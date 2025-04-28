package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public record ProbabilityChunkFilter(double chance) implements ChunkFilter {
    public static final MapCodec<ProbabilityChunkFilter> CODEC =
        Codec.doubleRange(0, 1).fieldOf("chance")
            .xmap(ProbabilityChunkFilter::new, ProbabilityChunkFilter::chance);

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        return ctx.rand.nextDouble() <= this.chance;
    }

    @Override
    public MapCodec<ProbabilityChunkFilter> codec() {
        return CODEC;
    }
}
