package personthecat.pangaea.world.injector;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.IdList;
import personthecat.pangaea.mixin.accessor.OreConfigurationAccessor;
import personthecat.pangaea.world.placement.SimplePlacementModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.idList;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;

@Log4j2
public record OreInjector(
        BiomePredicate biomes,
        @Nullable InjectionMap<Modifications> modifications,
        @Nullable InjectionMap<Modifications> injections,
        @Nullable IdList<ConfiguredFeature<?, ?>> removals) implements Injector {

    public static final MapCodec<OreInjector> CODEC = codecOf(
        defaulted(BiomePredicate.CODEC, "biomes", BiomePredicate.ALL_BIOMES, c -> c.biomes),
        nullable(Modifications.MAPPED_CODEC, "modify",  c -> c.modifications),
        nullable(Modifications.FLEXIBLE_CODEC, "inject", c -> c.injections),
        nullable(idList(Registries.CONFIGURED_FEATURE), "remove", c -> c.removals),
        OreInjector::new
    );

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        this.applyModifications(ctx);
        this.applyInjections(ctx);
        this.applyRemovals(ctx);
    }

    private void applyModifications(InjectionContext ctx) {
        if (this.modifications == null) {
            return;
        }
        final var configuredRegistry = ctx.registries().registryOrThrow(Registries.CONFIGURED_FEATURE);
        final var placedRegistry = ctx.registries().registryOrThrow(Registries.PLACED_FEATURE);
        final var removalsByCfId = new ArrayList<ResourceLocation>();
        final var addedPlacements = new ArrayList<Holder<PlacedFeature>>();
        this.modifications.forEach((id, mod) -> {
            final var original = configuredRegistry.getHolder(id).orElse(null);
            if (original == null) {
                log.warn("Unknown feature. Cannot modify: {}", id);
                return;
            }
            mod.updateConfig(id, original);
            if (mod.placement != null) {
                mod.createPlacedFeatures(original).forEach((placedId, placed) ->
                    addedPlacements.add(Registry.registerForHolder(placedRegistry, placedId, placed)));
                removalsByCfId.add(id);
            }
        });
        if (!removalsByCfId.isEmpty()) {
            ctx.addRemovals(this.biomes, mods -> removalsByCfId.forEach(id ->
                mods.removeFeature(placed -> placed.is(id))));
        }
        if (!addedPlacements.isEmpty()) {
            ctx.addAdditions(this.biomes, mods -> addedPlacements.forEach(placed ->
                mods.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placed)));
        }
    }

    private void applyInjections(InjectionContext ctx) {
        if (this.injections == null) {
            return;
        }
        final var configuredRegistry = ctx.registries().registryOrThrow(Registries.CONFIGURED_FEATURE);
        final var placedRegistry = ctx.registries().registryOrThrow(Registries.PLACED_FEATURE);
        final var addedPlacements = new ArrayList<Holder<PlacedFeature>>();
        this.injections.forEach((id, mod) -> {
            final var configured = new ConfiguredFeature<>(Feature.ORE, mod.toOreConfiguration());
            final var holder = Registry.registerForHolder(configuredRegistry, id, configured);
            if (mod.placement != null) {
                mod.createPlacedFeatures(holder).forEach((placedId, placed) ->
                    addedPlacements.add(Registry.registerForHolder(placedRegistry, placedId, placed)));
            }
        });
        ctx.addAdditions(this.biomes, mods -> addedPlacements.forEach(feature ->
            mods.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature)));
    }

    private void applyRemovals(InjectionContext ctx) {
        if (this.removals == null) {
            return;
        }
        if (this.removals.isExplicitAll()) {
            ctx.addRemovals(this.biomes,
                mods -> mods.removeFeature(placed -> placed.value().feature().value().config() instanceof OreConfiguration));
        } else {
            ctx.addRemovals(this.biomes,
                mods -> mods.removeFeature(placed -> this.removals.test(placed.value().feature())));
        }
    }

    @Override
    public MapCodec<OreInjector> codec() {
        return CODEC;
    }

    public record Modifications(
            @Nullable Integer size,
            @Nullable Float discardChanceOnAirExposure,
            @Nullable List<TargetBlockState> targets,
            @Nullable List<TargetBlockState> addTargets,
            @Nullable InjectionMap<List<PlacementModifier>> placement,
            PlacementModifier placementFilter) {
        private static final Codec<InjectionMap<List<PlacementModifier>>> PLACEMENT_CODEC =
            InjectionMap.codecOfMapOrList(SimplePlacementModifier.DEFAULTED_LIST_CODEC);
        public static final MapCodec<Modifications> CODEC = codecOf(
            nullable(Codec.intRange(0, 64), "size", m -> m.size),
            nullable(Codec.floatRange(0, 1), "discard_chance_on_air_exposure", m -> m.discardChanceOnAirExposure),
            nullable(easyList(TargetBlockState.CODEC), "targets", m -> m.targets),
            nullable(easyList(TargetBlockState.CODEC), "add_targets", m -> m.addTargets),
            nullable(PLACEMENT_CODEC, "placement", m -> m.placement),
            defaulted(PlacementModifier.CODEC, "filter", BiomeFilter.biome(), m -> m.placementFilter),
            Modifications::new
        );
        public static final Codec<InjectionMap<Modifications>> MAPPED_CODEC =
            InjectionMap.codecOfMap(CODEC.codec());
        public static final Codec<InjectionMap<Modifications>> FLEXIBLE_CODEC =
            InjectionMap.codecOfMapOrList(CODEC.codec());

        void updateConfig(ResourceLocation id, Holder<ConfiguredFeature<?, ?>> feature) {
            if (!(feature.value().config() instanceof OreConfigurationAccessor config)) {
                log.warn("Not an ore configuration. Cannot modify: {}", id);
                return;
            }
            if (this.size != null) config.setSize(this.size);
            if (this.discardChanceOnAirExposure != null) config.setDiscardChanceOnAirExposure(this.discardChanceOnAirExposure);
            if (this.targets != null) config.setTargetStates(this.targets);
            if (this.addTargets != null) {
                final var targets = ImmutableList.<TargetBlockState>builder()
                    .addAll(((OreConfiguration) config).targetStates)
                    .addAll(this.addTargets);
                config.setTargetStates(targets.build());
            }
        }

        private OreConfiguration toOreConfiguration() {
            return new OreConfiguration(
                this.targets != null ? this.targets : List.of(),
                this.size != null ? this.size : 8,
                this.discardChanceOnAirExposure != null ? this.discardChanceOnAirExposure : 0);
        }

        private Map<ResourceLocation, PlacedFeature> createPlacedFeatures(Holder<ConfiguredFeature<?, ?>> configured) {
            if (this.placement == null) return Map.of();
            return this.placement.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), new PlacedFeature(configured, this.addFilter(entry.getValue()))))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        }

        private List<PlacementModifier> addFilter(List<PlacementModifier> modifiers) {
            if (modifiers.stream().anyMatch(m -> m instanceof PlacementFilter)) return modifiers;
            return ImmutableList.<PlacementModifier>builder().addAll(modifiers).add(this.placementFilter).build();
        }
    }
}
