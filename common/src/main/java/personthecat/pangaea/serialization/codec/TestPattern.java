package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.KeyCompressor;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapLike;
import net.minecraft.util.Unit;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@FunctionalInterface
public interface TestPattern extends Predicate<Dynamic<?>>, Decoder<Unit> {
    TestPattern STRING = d -> d.asString().isSuccess();
    TestPattern NUMBER = d -> d.asNumber().isSuccess();
    TestPattern MAP = d -> d.getMapValues().isSuccess();
    TestPattern LIST = d -> d.asStreamOpt().isSuccess();
    TestPattern STRING_LIST = listOf(STRING);
    TestPattern NUMBER_LIST = listOf(NUMBER);
    TestPattern MAP_LIST = listOf(MAP);
    TestPattern ID = matching("(?:\\w+:)?\\w+");
    TestPattern STATE = matching("(?:\\w+:)?\\w+\\[.*\\]").or(mapContaining("Name"));
    TestPattern ALWAYS = d -> true;

    static TestPattern listOf(TestPattern p) {
        return d -> d.asStreamOpt().result().filter(s -> s.allMatch(p)).isPresent();
    }

    static TestPattern matching(@RegExp String pattern) {
        return d -> d.asString().result().filter(Pattern.compile(pattern).asMatchPredicate()).isPresent();
    }

    static TestPattern mapContaining(String... keys) {
        return forMap(MapPattern.contains(keys));
    }

    static TestPattern forMap(MapPattern pattern) {
        return d -> pattern.decoder().decode(d).isSuccess();
    }

    static TestPattern not(TestPattern pattern) {
        return d -> !pattern.test(d);
    }

    @Override
    default <T> DataResult<Pair<Unit, T>> decode(DynamicOps<T> ops, T input) {
        return this.test(new Dynamic<>(ops, input))
            ? DataResult.success(Pair.of(Unit.INSTANCE, input))
            : DataResult.error(() -> "Pattern mismatch");
    }

    default TestPattern or(TestPattern other) {
        return d -> this.test(d) || other.test(d);
    }

    @FunctionalInterface
    interface MapPattern extends Predicate<MapPattern.MapFunction>, MapDecoder<Unit> {
        KeyCompressor<?> COMPRESSOR = new KeyCompressor<>(JavaOps.INSTANCE, Stream.empty());

        static MapPattern contains(String... keys) {
            return m -> Stream.of(keys).allMatch(k -> m.get(k) != null);
        }

        @Override
        default <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        default <T> KeyCompressor<T> compressor(DynamicOps<T> ops) {
            return (KeyCompressor<T>) COMPRESSOR;
        }

        @Override
        default <T> DataResult<Unit> decode(DynamicOps<T> ops, MapLike<T> map) {
            return this.test(MapFunction.create(ops, map))
                ? DataResult.success(Unit.INSTANCE)
                : DataResult.error(() -> "Pattern mismatch");
        }

        @FunctionalInterface
        interface MapFunction {
            @Nullable Dynamic<?> get(String key);

            default Optional<Dynamic<?>> getOptional(String key) {
                return Optional.ofNullable(this.get(key));
            }

            default boolean keyMatches(String key, TestPattern p) {
                return this.getOptional(key).filter(p).isPresent();
            }

            static <T> MapFunction create(DynamicOps<T> ops, MapLike<T> map) {
                return s -> Optional.ofNullable(map.get(s)).map(t -> new Dynamic<>(ops, t)).orElse(null);
            }
        }
    }
}
