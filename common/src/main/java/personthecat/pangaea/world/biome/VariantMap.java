package personthecat.pangaea.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate.ParameterPoint;

import java.util.List;
import java.util.Map;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record VariantMap<T>(Map<String, VariantResolver<T>> map) {
    private static final VariantMap<?> EMPTY = new VariantMap<>(Map.of());

    public static <T> Codec<VariantMap<T>> createCodec(Codec<Holder<T>> holderCodec, String holderName) {
        return Codec.unboundedMap(Codec.STRING, VariantResolver.createCodec(holderCodec, holderName).codec())
            .xmap(VariantMap::new, VariantMap::map);
    }

    @SuppressWarnings("unchecked")
    public static <T> VariantMap<T> empty() {
        return (VariantMap<T>) EMPTY;
    }

    public boolean isDefined(String key) {
        return this.map.containsKey(key);
    }

    public VariantResolver<T> get(String key) {
        return this.map.get(key);
    }

    public record VariantResolver<T>(Holder<T> defaultHolder, List<Variant<T>> variants) {

        public static <T> MapCodec<VariantResolver<T>> createCodec(Codec<Holder<T>> holderCodec, String keyName) {
            final var variantCodec = easyList(Variant.createCodec(holderCodec, keyName));
            return codecOf(
                field(holderCodec, "default", VariantResolver::defaultHolder),
                field(variantCodec, "variant", VariantResolver::variants),
                VariantResolver::new
            );
        }

        public Holder<T> getHolder(ParameterPoint point) {
            for (final var variant : this.variants) {
                if (variant.conditions.matchesPoint(point)) {
                    return variant.holder;
                }
            }
            return this.defaultHolder;
        }
    }

    public record Variant<T>(Holder<T> holder, ParameterMap conditions) {

        public static <T> Codec<Variant<T>> createCodec(Codec<Holder<T>> holderCodec, String keyName) {
            final var holderOnly = holderCodec.xmap(key -> new Variant<>(key, ParameterMap.WHEN_WEIRD), Variant::holder);
            final var direct = directCodec(holderCodec, keyName).codec();
            return simpleEither(holderOnly, direct)
                .withEncoder(v -> v.conditions == ParameterMap.WHEN_WEIRD ? holderOnly : direct);
        }

        private static <T> MapCodec<Variant<T>> directCodec(Codec<Holder<T>> holderCodec, String keyName) {
            return codecOf(
                field(holderCodec, keyName, Variant::holder),
                union(ParameterMap.CODEC, Variant::conditions),
                Variant::new
            );
        }
    }
}
