package personthecat.pangaea.world.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate.ParameterPoint;

import java.util.Objects;
import java.util.function.Function;

public sealed interface Choice<T> {

    static <T> Codec<Choice<T>> createCodec(Codec<Holder<T>> holderCodec) {
        return Codec.either(Literal.createCodec(holderCodec), VariantName.<T>createCodec()).xmap(
            either -> either.map(Function.identity(), Function.identity()),
            choice -> choice instanceof Literal<T> l ? Either.left(l) : Either.right((VariantName<T>) choice));
    }

    Holder<T> resolve(VariantMap<T> map, ParameterPoint point);

    default Getter<T> getter(VariantMap<T> map) {
        return new Getter<>(this, map);
    }

    record Literal<T>(Holder<T> holder) implements Choice<T> {

        public static <T> Codec<Literal<T>> createCodec(Codec<Holder<T>> holderCodec) {
            return holderCodec.xmap(Literal::new, Literal::holder);
        }

        @Override
        public Holder<T> resolve(VariantMap<T> map, ParameterPoint point) {
            return this.holder;
        }
    }

    record VariantName<T>(String key) implements Choice<T> {

        public static <T> Codec<VariantName<T>> createCodec() {
            return Codec.STRING.comapFlatMap(VariantName::fromHashString, VariantName::hashedKey);
        }

        private static <T> DataResult<VariantName<T>> fromHashString(String s) {
            if (s.startsWith("#")) {
                return DataResult.success(new VariantName<>(s.substring(1)));
            }
            return DataResult.error(() -> "Not a hashed variant name: " + s);
        }

        public String hashedKey() {
            return "#" + this.key;
        }

        @Override
        public Holder<T> resolve(VariantMap<T> map, ParameterPoint point) {
            final var variant = map.get(this.key);
            Objects.requireNonNull(variant, "Codec was unchecked. Variant not found: " + variant);
            return variant.getHolder(point);
        }
    }

    record Getter<T>(Choice<T> choice, VariantMap<T> variants) {

        public T get(ParameterPoint point) {
            return this.getHolder(point).value();
        }

        public Holder<T> getHolder(ParameterPoint point) {
            return this.choice.resolve(this.variants, point);
        }
    }
}