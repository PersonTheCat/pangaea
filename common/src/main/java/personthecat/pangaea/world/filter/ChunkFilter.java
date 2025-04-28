package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PatternChunkFilterCodec;
import personthecat.pangaea.serialization.codec.StructuralChunkFilterCodec;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public interface ChunkFilter {
    Codec<ChunkFilter> CODEC = buildCodec();

    boolean test(PangaeaContext ctx, int x, int z);
    MapCodec<? extends ChunkFilter> codec();

    private static Codec<ChunkFilter> buildCodec() {
        final var dispatcher =
            PgRegistries.CHUNK_FILTER_TYPE.codec().dispatch(ChunkFilter::codec, Function.identity());
        return Codec.lazyInitialized(() ->
            PatternChunkFilterCodec.wrap(StructuralChunkFilterCodec.wrap(dispatcher)));
    }
}
