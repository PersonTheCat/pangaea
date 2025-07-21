package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static personthecat.catlib.serialization.codec.CodecUtils.asParent;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;

public record PatternCodec<A>(List<Pattern<A>> patterns, BooleanSupplier encode) implements Codec<A> {
    public PatternCodec(List<Pattern<A>> patterns) {
        this(patterns, () -> true);
    }

    public Codec<A> wrap(Codec<A> original) {
        return simpleEither(original, this).withEncoder(a ->
            this.encode.getAsBoolean() && this.hasPatternForInput(a) ? this : original);
    }

    public boolean hasPatternForInput(A input) {
        if (this.encode.getAsBoolean()) {
            for (final Pattern<A> p : this.patterns) {
                if (p.filter.test(p.normalizer.apply(input))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        for (final Pattern<A> p : this.patterns) {
            final var result = p.codec.decode(ops, input);
            if (result.isSuccess()) {
                return result;
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        if (!this.encode.getAsBoolean()) {
            return DataResult.error(() -> "Encoding patterns disabled for input: " + input);
        }
        for (final Pattern<A> p : this.patterns) {
            final var normalized = p.normalizer.apply(input);
            if (p.filter.test(normalized)) {
                return p.codec.encode(normalized, ops, prefix);
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    public record Pattern<A>(Codec<A> codec, UnaryOperator<A> normalizer, Predicate<A> filter) {
        @SuppressWarnings("unchecked")
        public static <A, B extends A, T> Function<A, T> get(Function<B, T> getter) {
            return a -> getter.apply((B) a);
        }

        public static <A> Pattern<A> of(Codec<? extends A> codec, Class<? extends A> type) {
            return of(codec, type::isInstance);
        }

        public static <A> Pattern<A> of(Codec<? extends A> codec, Predicate<A> test) {
            return new Pattern<>(asParent(codec), a -> a, test);
        }

        public Pattern<A> normalized(UnaryOperator<A> normalizer) {
            return new Pattern<>(this.codec, normalizer, this.filter);
        }
    }
}
