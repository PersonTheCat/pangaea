package personthecat.pangaea.world.filter;

import com.mojang.serialization.MapCodec;
import personthecat.fastnoise.FastNoise;
import personthecat.pangaea.serialization.codec.NoiseCodecs;
import personthecat.pangaea.world.level.PangaeaContext;

public record FastNoiseChunkFilter(FastNoise noise) implements ChunkFilter {
    public static final MapCodec<FastNoiseChunkFilter> CODEC =
        NoiseCodecs.NOISE_CODEC.xmap(FastNoiseChunkFilter::new, FastNoiseChunkFilter::noise);

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        return this.noise.getBoolean(x, z);
    }

    @Override
    public MapCodec<FastNoiseChunkFilter> codec() {
        return CODEC;
    }
}
