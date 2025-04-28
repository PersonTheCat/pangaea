package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.filter.UnionChunkFilter;

import java.util.function.Function;

public class PatternChunkFilterCodec implements Codec<ChunkFilter> {
    public static final Codec<ChunkFilter> INSTANCE = new PatternChunkFilterCodec();

    private PatternChunkFilterCodec() {}

    public static Codec<ChunkFilter> wrap(Codec<ChunkFilter> codec) {
        return Codec.either(INSTANCE, codec).xmap(
            e -> e.map(Function.identity(), Function.identity()),
            f -> Cfg.encodePatternChunkFilters() && Pattern.hasMatcherForFilter(f)
                ? Either.left(f) : Either.right(f)
        );
    }

    @Override
    public <T> DataResult<Pair<ChunkFilter, T>> decode(DynamicOps<T> ops, T input) {
        for (final Pattern.Matcher m : Pattern.MATCHERS) {
            final var result = m.codec.decode(ops, input);
            if (result.isSuccess()) {
                return result;
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    @Override
    public <T> DataResult<T> encode(ChunkFilter input, DynamicOps<T> ops, T prefix) {
        if (Cfg.encodePatternRuleTestCodec()) {
            for (final Pattern.Matcher m : Pattern.MATCHERS) {
                if (m.type.isInstance(input)) {
                    return m.codec.encode(input, ops, prefix);
                }
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    private static class Pattern {
        private static final Codec<ChunkFilter> NUMBER =
            Codec.doubleRange(0, 1).xmap(ChanceChunkFilter::new, f -> ((ChanceChunkFilter) f).chance());
        private static final Codec<ChunkFilter> LIST =
            ChunkFilter.CODEC.listOf().xmap(UnionChunkFilter::new, f -> ((UnionChunkFilter) f).filters());

        private static final Matcher[] MATCHERS = {
            new Matcher(NUMBER, ChanceChunkFilter.class),
            new Matcher(LIST, UnionChunkFilter.class),
        };

        private record Matcher(Codec<ChunkFilter> codec, Class<? extends ChunkFilter> type) {}

        private static boolean hasMatcherForFilter(ChunkFilter filter) {
            for (final var matcher : MATCHERS) {
                if (matcher.type.isInstance(filter)) {
                    return true;
                }
            }
            return false;
        }
    }
}
