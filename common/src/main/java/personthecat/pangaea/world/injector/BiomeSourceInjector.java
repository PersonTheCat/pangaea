package personthecat.pangaea.world.injector;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.world.biome.CartesianParameterList;
import personthecat.pangaea.extras.MultiNoiseBiomeSourceExtras;
import personthecat.pangaea.world.biome.ParameterMap;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaultedUnion;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record BiomeSourceInjector(DimensionPredicate dimensions, Modifications injections) implements Injector {
    public static final MapCodec<BiomeSourceInjector> CODEC = codecOf(
        field(DimensionPredicate.CODEC, "dimensions", BiomeSourceInjector::dimensions),
        union(Modifications.CODEC, BiomeSourceInjector::injections),
        BiomeSourceInjector::new
    );

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        ctx.registries().registryOrThrow(Registries.LEVEL_STEM).holders().forEach(stem -> {
            if (this.dimensions.test(stem.value()) && this.injections.hasChanges()) {
                this.modifyDimension(stem);
            }
        });
    }

    private void modifyDimension(Holder<LevelStem> stem) {
        if (!(stem.value().generator().getBiomeSource() instanceof MultiNoiseBiomeSource source)) {
            final var key = stem.unwrapKey().orElseThrow();
            LibErrorContext.warn(Pangaea.MOD, InjectionWarningException.incompatibleBiomeSource(key));
            return;
        }
        MultiNoiseBiomeSourceExtras.modifyBiomeParameters(source, parameters -> {
            if (this.injections.removeBiomes != null) {
                parameters.removeIf(pair -> this.injections.removeBiomes.test(pair.getSecond()));
            }
            if (this.injections.addBiomes != null) {
                this.injections.addBiomes.forEach(added -> parameters.addAll(added.generateParameters()));
            }
            if (this.injections.replaceBiomes != null) {
                for (int i = 0; i < parameters.size(); i++) {
                    final var parameter = parameters.get(i);
                    for (final var replaced : this.injections.replaceBiomes) {
                        if (replaced.matchesPair(parameter)) {
                            parameters.set(i, Pair.of(parameter.getFirst(), replaced.replacement));
                        }
                    }
                }
            }
        });
    }

    @Override
    public Phase phase() {
        return Phase.DIMENSION;
    }

    @Override
    public MapCodec<BiomeSourceInjector> codec() {
        return CODEC;
    }

    public record Modifications(
            @Nullable BiomePredicate removeBiomes,
            @Nullable List<AddedBiome> addBiomes,
            @Nullable List<ReplacedBiome> replaceBiomes) {
        public static final MapCodec<Modifications> CODEC = codecOf(
            nullable(BiomePredicate.CODEC, "remove_biomes", Modifications::removeBiomes),
            nullable(AddedBiome.CODEC.codec().listOf(), "add_biomes", Modifications::addBiomes),
            nullable(ReplacedBiome.CODEC.codec().listOf(), "replace_biomes", Modifications::replaceBiomes),
            Modifications::new
        );

        public boolean hasChanges() {
            return this.removeBiomes != null || this.addBiomes != null || this.replaceBiomes != null;
        }
    }

    public record AddedBiome(Holder<Biome> biome, CartesianParameterList climate) {
        public static final MapCodec<AddedBiome> CODEC = codecOf(
            field(Biome.CODEC, "biome", AddedBiome::biome),
            field(CartesianParameterList.CODEC, "climate", AddedBiome::climate),
            AddedBiome::new
        );

        public List<Pair<ParameterPoint, Holder<Biome>>> generateParameters() {
            return this.climate.createPairs(this.biome);
        }
    }

    public record ReplacedBiome(BiomePredicate biomes, Holder<Biome> replacement, ParameterMap conditions) {
        public static final MapCodec<ReplacedBiome> CODEC = codecOf(
            defaulted(BiomePredicate.CODEC, "biomes", BiomePredicate.ALL_BIOMES, ReplacedBiome::biomes),
            field(Biome.CODEC, "replacement", ReplacedBiome::replacement),
            defaultedUnion(ParameterMap.CODEC, () -> ParameterMap.EMPTY, ReplacedBiome::conditions),
            ReplacedBiome::new
        );

        public boolean matchesPair(Pair<ParameterPoint, Holder<Biome>> pair) {
            return this.biomes.test(pair.getSecond()) && this.conditions.matchesPoint(pair.getFirst());
        }
    }
}
