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
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.serialization.codec.CaptureCategory;
import personthecat.catlib.serialization.codec.CapturingCodec;
import personthecat.pangaea.serialization.codec.FeatureCodecs;
import personthecat.pangaea.world.feature.ConditionConfiguration;
import personthecat.pangaea.world.placement.SimplePlacementModifier;
import personthecat.pangaea.world.placement.SurfaceBiomeFilter;

import java.util.ArrayList;
import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record FeatureInjector(InjectionMap<Modifications> injections) implements Injector {
    private static final MapCodec<FeatureInjector> DIRECT_CODEC =
        InjectionMap.codecOfEasyList(Modifications.CODEC.codec())
            .fieldOf("inject")
            .xmap(FeatureInjector::new, FeatureInjector::injections);

    public static final MapCodec<FeatureInjector> CODEC =
        CapturingCodec.of(DIRECT_CODEC)
            .capturing(ConditionConfiguration.CATEGORY.captors())
            .capturing(Modifications.CATEGORY.captors());

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        this.injections.forEach((id, mods) -> mods.inject(id, ctx));
    }

    @Override
    public MapCodec<FeatureInjector> codec() {
        return CODEC;
    }

    public record Modifications(
            BiomePredicate biomes,
            Decoration step,
            ConfiguredFeature<?, ?> feature,
            @Nullable InjectionMap<List<PlacementModifier>> placement) {

        private static final CaptureCategory<Modifications> CATEGORY =
            CaptureCategory.get("Modifications");

        private static final Codec<InjectionMap<List<PlacementModifier>>> PLACEMENT_CODEC =
            InjectionMap.codecOfMapOrList(SimplePlacementModifier.DEFAULTED_LIST_CODEC);

        public static final MapCodec<Modifications> CODEC = codecOf(
            CATEGORY.defaulted(BiomePredicate.CODEC, "biomes", BiomePredicate.ALL_BIOMES, Modifications::biomes),
            CATEGORY.defaulted(Decoration.CODEC, "step", Decoration.RAW_GENERATION, Modifications::step),
            union(FeatureCodecs.FLAT_CONFIG, Modifications::feature),
            nullable(PLACEMENT_CODEC, "placement", Modifications::placement),
            Modifications::new
        );

        public void inject(ResourceLocation id, InjectionContext ctx) {
            final var step = this.step;

            final var configuredRegistry = ctx.registries().registryOrThrow(Registries.CONFIGURED_FEATURE);
            final var configuredHolder = Registry.registerForHolder(configuredRegistry, id, this.feature);

            final var features = this.createFeatures(id, ctx, configuredHolder);
            ctx.addAdditions(this.biomes, mods -> features.forEach(f -> mods.addFeature(step, f)));
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
            if (modifiers.stream().anyMatch(Modifications::isBiomeFilter)) return modifiers;
            return ImmutableList.<PlacementModifier>builder().addAll(modifiers).add(SurfaceBiomeFilter.INSTANCE).build();
        }

        private static boolean isBiomeFilter(PlacementModifier m) {
            return m instanceof BiomeFilter || m instanceof SurfaceBiomeFilter;
        }
    }
}
