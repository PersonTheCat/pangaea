package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public interface ChunkFilter {
    Codec<ChunkFilter> CODEC =
        PangaeaCodec.forRegistry(PgRegistries.CHUNK_FILTER_TYPE, ChunkFilter::codec);

    boolean test(PangaeaContext ctx, int x, int z);
    MapCodec<? extends ChunkFilter> codec();
}
