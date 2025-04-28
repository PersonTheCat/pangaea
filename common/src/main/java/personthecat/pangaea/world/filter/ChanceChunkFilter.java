package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public record ChanceChunkFilter(double chance) implements ChunkFilter {
    public static final MapCodec<ChanceChunkFilter> CODEC =
        Codec.doubleRange(0, 1).fieldOf("chance")
            .xmap(ChanceChunkFilter::new, ChanceChunkFilter::chance);

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        return ctx.rand.nextDouble() <= this.chance;
    }

    @Override
    public MapCodec<ChanceChunkFilter> codec() {
        return CODEC;
    }
}
