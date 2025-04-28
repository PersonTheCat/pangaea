package personthecat.pangaea.world.filter;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.List;

public record UnionChunkFilter(List<ChunkFilter> filters) implements ChunkFilter {
    public static final MapCodec<UnionChunkFilter> CODEC =
        ChunkFilter.CODEC.listOf().fieldOf("filters")
            .xmap(UnionChunkFilter::new, UnionChunkFilter::filters);

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        for (final var filter : this.filters) {
            if (!filter.test(ctx, x, z)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public MapCodec<UnionChunkFilter> codec() {
        return CODEC;
    }
}
