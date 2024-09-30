package personthecat.pangaea.world.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.ParameterPoint;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record BiomeLayout(Map<String, VariantResolver> variants, ParameterMatrix<BiomeChoice> biomes) {
    private static final List<Parameter> DEFAULT_TEMPERATURE = List.of(
        Parameter.span(-1.0F, -0.45F),
        Parameter.span(-0.45F, -0.15F),
        Parameter.span(-0.15F, 0.2F),
        Parameter.span(0.2F, 0.55F),
        Parameter.span(0.55F, 1.0F)
    );
    private static final List<Parameter> DEFAULT_HUMIDITY = List.of(
        Parameter.span(-1.0F, -0.35F),
        Parameter.span(-0.35F, -0.1F),
        Parameter.span(-0.1F, 0.1F),
        Parameter.span(0.1F, 0.3F),
        Parameter.span(0.3F, 1.0F)
    );
    private static final Codec<ResourceKey<Biome>> BIOME_KEY_CODEC =
        ResourceKey.codec(Registries.BIOME);
    private static final Codec<Map<String, VariantResolver>> VARIANT_MAP_CODEC =
        Codec.unboundedMap(Codec.STRING, VariantResolver.CODEC);
    private static final MapCodec<ParameterMatrix<BiomeChoice>> BIOME_MATRIX_CODEC =
        ParameterMatrix.codecBuilder(BiomeChoice.CODEC)
            .withKeys("temperature", "humidity", "biomes")
            .withDefaultAxes(DEFAULT_TEMPERATURE, DEFAULT_HUMIDITY)
            .build();

    public static final MapCodec<BiomeLayout> CODEC = codecOf(
        defaulted(VARIANT_MAP_CODEC, "variants", Map.of(), BiomeLayout::variants),
        union(BIOME_MATRIX_CODEC, BiomeLayout::biomes),
        BiomeLayout::new
    ).validate(BiomeLayout::validate);

    private DataResult<BiomeLayout> validate() {
        for (final var row : this.biomes.matrix()) {
            for (final var col : row) {
                if (col instanceof BiomeChoice.VariantName name && ! this.variants.containsKey(name.key)) {
                    return DataResult.error(() -> "No variant for key: " + name.key);
                }
            }
        }
        return DataResult.success(this);
    }

    record VariantResolver(ResourceKey<Biome> defaultBiome, List<Variant> variants) {
        public static final Codec<VariantResolver> CODEC = codecOf(
            field(BIOME_KEY_CODEC, "default", VariantResolver::defaultBiome),
            field(easyList(Variant.CODEC), "variant", VariantResolver::variants),
            VariantResolver::new
        ).codec();

        public ResourceKey<Biome> getBiome(ParameterPoint point) {
            for (final var variant : this.variants) {
                if (variant.conditions.matchesPoint(point)) {
                    return variant.biome;
                }
            }
            return this.defaultBiome;
        }
    }

    record Variant(ResourceKey<Biome> biome, ParameterMap conditions) {
        private static final Codec<Variant> BIOME_ONLY_CODEC =
            BIOME_KEY_CODEC.xmap(key -> new Variant(key, ParameterMap.WHEN_WEIRD), Variant::biome);
        private static final Codec<Variant> DIRECT_CODEC = codecOf(
            field(ResourceKey.codec(Registries.BIOME), "biome", Variant::biome),
            union(ParameterMap.CODEC, Variant::conditions),
            Variant::new
        ).codec();
        public static final Codec<Variant> CODEC =
            simpleEither(BIOME_ONLY_CODEC, DIRECT_CODEC).withEncoder(
                v -> v.conditions == ParameterMap.WHEN_WEIRD ? BIOME_ONLY_CODEC : DIRECT_CODEC);
    }

    public sealed interface BiomeChoice {
        Codec<BiomeChoice> CODEC =
            Codec.either(Literal.CODEC, VariantName.CODEC).xmap(
                either -> either.map(Function.identity(), Function.identity()),
                choice -> choice instanceof Literal l ? Either.left(l) : Either.right((VariantName) choice));

        ResourceKey<Biome> resolve(BiomeLayout layout, ParameterPoint point);

        record Literal(ResourceKey<Biome> biome) implements BiomeChoice {
            public static final Codec<Literal> CODEC = BIOME_KEY_CODEC.xmap(Literal::new, Literal::biome);

            @Override
            public ResourceKey<Biome> resolve(BiomeLayout layout, ParameterPoint point) {
                return this.biome;
            }
        }

        record VariantName(String key) implements BiomeChoice {
            public static final Codec<VariantName> CODEC =
                Codec.STRING.comapFlatMap(VariantName::fromHashString, VariantName::key);

            private static DataResult<VariantName> fromHashString(String s) {
                if (s.startsWith("#")) {
                    return DataResult.success(new VariantName(s.substring(1)));
                }
                return DataResult.error(() -> "Not a hashed variant name: " + s);
            }

            @Override
            public ResourceKey<Biome> resolve(BiomeLayout layout, ParameterPoint point) {
                final var variant = layout.variants.get(this.key);
                Objects.requireNonNull(variant, "Codec was unchecked. Variant not found: " + variant);
                return variant.getBiome(point);
            }
        }
    }
}
