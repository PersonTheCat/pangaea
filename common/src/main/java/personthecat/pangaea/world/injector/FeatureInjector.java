package personthecat.pangaea.world.injector;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.IdList;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.serialization.codec.capture.CaptureCategory;
import personthecat.pangaea.serialization.codec.FeatureCodecs;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.feature.ConditionConfiguration;
import personthecat.pangaea.world.placement.SimplePlacementModifier;
import personthecat.pangaea.world.placement.SurfaceBiomeFilter;

import java.util.ArrayList;
import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record FeatureInjector(
        BiomePredicate biomes,
        @Nullable IdList<PlacedFeature> remove,
        @Nullable IdList<Feature<?>> removeFeatures,
        InjectionMap<AddedFeature> inject) implements Injector {

    public static final MapCodec<FeatureInjector> CODEC =
        PangaeaCodec.build(FeatureInjector::createCodec)
            .addCaptures(CaptureCategory.get(AddedFeature.class).captors())
            .mapCodec();

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        if (this.remove != null) {
            ctx.addRemovals(this.biomes, mods -> mods.removeFeature(this.remove));
        }
        if (this.removeFeatures != null) {
            ctx.addRemovals(this.biomes, mods -> mods.removeFeature(holder -> {
               final var feature = CommonRegistries.FEATURE.getHolder(holder.value().feature().value().feature());
               return this.removeFeatures.test(feature);
            }));
        }
        this.inject.forEach((id, mods) -> mods.apply(this.biomes, id, ctx));
    }

    @Override
    public MapCodec<FeatureInjector> codec() {
        return CODEC;
    }

    private static MapCodec<FeatureInjector> createCodec(CaptureCategory<FeatureInjector> cat) {
        final var injectionListCodec = InjectionMap.codecOfEasyList(AddedFeature.CODEC.codec());
        return codecOf(
            defaulted(BiomePredicate.CODEC, "biomes", BiomePredicate.ALL_BIOMES, FeatureInjector::biomes),
            nullable(IdList.codecOf(Registries.PLACED_FEATURE), "remove", FeatureInjector::remove),
            nullable(IdList.codecOf(Registries.FEATURE), "remove_features", FeatureInjector::removeFeatures),
            defaulted(injectionListCodec, "inject", new InjectionMap<>(), FeatureInjector::inject),
            FeatureInjector::new
        );
    }

    public record AddedFeature(
            Decoration step,
            ConfiguredFeature<?, ?> feature,
            @Nullable InjectionMap<List<PlacementModifier>> placement) {

        private static final Codec<InjectionMap<List<PlacementModifier>>> PLACEMENT_CODEC =
            InjectionMap.codecOfMapOrList(SimplePlacementModifier.DEFAULTED_LIST_CODEC);

        public static final MapCodec<AddedFeature> CODEC = PangaeaCodec.buildMap(cat -> codecOf(
            cat.defaulted(Decoration.CODEC, "step", Decoration.RAW_GENERATION, AddedFeature::step),
            union(FeatureCodecs.FLAT_CONFIG, AddedFeature::feature),
            cat.nullable(PLACEMENT_CODEC, "placement", AddedFeature::placement),
            AddedFeature::new
        ));

        private void apply(BiomePredicate biomes, ResourceLocation id, InjectionContext ctx) {
            final var step = this.step;

            final var configuredRegistry = ctx.registries().registryOrThrow(Registries.CONFIGURED_FEATURE);
            final var configuredHolder = Registry.registerForHolder(configuredRegistry, id, this.feature);

            final var features = this.createFeatures(id, ctx, configuredHolder);
            ctx.addAdditions(biomes, mods -> features.forEach(f -> mods.addFeature(step, f)));
        }

        private List<Holder<PlacedFeature>> createFeatures(
                ResourceLocation parentId,
                InjectionContext ctx,
                Holder<ConfiguredFeature<?, ?>> configuredHolder) {
            final var placedRegistry = ctx.registries().registryOrThrow(Registries.PLACED_FEATURE);
            final var list = new ArrayList<Holder<PlacedFeature>>();
            if (this.placement != null && !this.placement.isEmpty()) {
                this.placement.forEach((id, modifiers) -> {
                    final var placed = new PlacedFeature(configuredHolder, this.addBiomeFilter(modifiers));
                    list.add(Registry.registerForHolder(placedRegistry, id, placed));
                });
            } else {
                final var placed = new PlacedFeature(configuredHolder, List.of(SurfaceBiomeFilter.INSTANCE));
                list.add(Registry.registerForHolder(placedRegistry, parentId, placed));
            }
            return list;
        }

        private List<PlacementModifier> addBiomeFilter(List<PlacementModifier> modifiers) {
            if (modifiers.stream().anyMatch(AddedFeature::isBiomeFilter)) return modifiers;
            return ImmutableList.<PlacementModifier>builder().addAll(modifiers).add(SurfaceBiomeFilter.INSTANCE).build();
        }

        private static boolean isBiomeFilter(PlacementModifier m) {
            return m instanceof BiomeFilter || m instanceof SurfaceBiomeFilter;
        }
    }
}
