package personthecat.pangaea.world.filter;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public interface ChunkFilter {
    Codec<ChunkFilter> CODEC = createCodec();

    boolean test(PangaeaContext ctx, int x, int z);
    MapCodec<? extends ChunkFilter> codec();

    private static Codec<ChunkFilter> createCodec() {
        final var dispatcher =
            PgRegistries.CHUNK_FILTER_TYPE.codec().dispatch(ChunkFilter::codec, Function.identity());
        return Codec.either(Codec.DOUBLE, dispatcher).xmap(
            e -> e.map(ChanceChunkFilter::new, Function.identity()),
            f -> f instanceof ChanceChunkFilter(var chance) ? Either.left(chance) : Either.right(f)
        );
    }
}
