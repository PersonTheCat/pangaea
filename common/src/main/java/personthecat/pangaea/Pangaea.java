package personthecat.pangaea;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.versioning.Version;
import personthecat.catlib.versioning.VersionTracker;
import personthecat.pangaea.command.CommandPg;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.appender.CodecAppenders;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.serialization.extras.DefaultBuilderFields;
import personthecat.pangaea.serialization.extras.DefaultCodecFlags;
import personthecat.pangaea.serialization.extras.DefaultCodecPatterns;
import personthecat.pangaea.serialization.extras.DefaultCodecStructures;
import personthecat.pangaea.serialization.preset.ChainFeaturePresets;
import personthecat.pangaea.util.SeedSupport;
import personthecat.pangaea.world.chain.CanyonLink;
import personthecat.pangaea.world.chain.CanyonPath;
import personthecat.pangaea.world.chain.ChasmLink;
import personthecat.pangaea.world.chain.SphereLink;
import personthecat.pangaea.world.chain.TunnelPath;
import personthecat.pangaea.world.density.DensityController;
import personthecat.pangaea.world.density.DensityList;
import personthecat.pangaea.world.density.FastNoiseDensity;
import personthecat.pangaea.world.density.NormalDensity;
import personthecat.pangaea.world.density.TrapezoidDensity;
import personthecat.pangaea.world.density.UniformDensity;
import personthecat.pangaea.world.density.WeightedListDensity;
import personthecat.pangaea.world.feature.BlobFeature;
import personthecat.pangaea.world.feature.BurrowFeature;
import personthecat.pangaea.world.feature.ChainFeature;
import personthecat.pangaea.world.feature.DebugWeightFeature;
import personthecat.pangaea.world.feature.DensityFeature;
import personthecat.pangaea.world.feature.GiantSphereFeature;
import personthecat.pangaea.world.feature.RavineFeature;
import personthecat.pangaea.world.feature.RoadFeature;
import personthecat.pangaea.world.feature.TestFeature;
import personthecat.pangaea.world.feature.TunnelFeature;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.filter.ClusterChunkFilter;
import personthecat.pangaea.world.filter.DensityChunkFilter;
import personthecat.pangaea.world.filter.FastNoiseChunkFilter;
import personthecat.pangaea.world.filter.IntervalChunkFilter;
import personthecat.pangaea.world.filter.PredictableChunkFilter;
import personthecat.pangaea.world.filter.SpawnDistanceChunkFilter;
import personthecat.pangaea.world.filter.UnionChunkFilter;
import personthecat.pangaea.world.injector.BiomeInjector;
import personthecat.pangaea.world.injector.BiomeModifierInjector;
import personthecat.pangaea.world.injector.BiomeSourceInjector;
import personthecat.pangaea.world.injector.CavernInjector;
import personthecat.pangaea.world.injector.DataInjectionHook;
import personthecat.pangaea.world.injector.DimensionInjector;
import personthecat.pangaea.world.injector.FeatureInjector;
import personthecat.pangaea.world.injector.OreInjector;
import personthecat.pangaea.world.placement.IntervalPlacementModifier;
import personthecat.pangaea.world.placement.SimplePlacementModifier;
import personthecat.pangaea.world.placement.SurfaceBiomeFilter;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacerList;
import personthecat.pangaea.world.placer.ChanceBlockPlacer;
import personthecat.pangaea.world.placer.ColumnRestrictedBlockPlacer;
import personthecat.pangaea.world.placer.TargetedBlockPlacer;
import personthecat.pangaea.world.placer.UnconditionalBlockPlacer;
import personthecat.pangaea.world.provider.AnchorRangeColumnProvider;
import personthecat.pangaea.world.provider.BiasedToBottomFloat;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.ConstantColumnProvider;
import personthecat.pangaea.world.provider.DensityFloatProvider;
import personthecat.pangaea.world.provider.DensityHeightProvider;
import personthecat.pangaea.world.provider.DensityIntProvider;
import personthecat.pangaea.world.provider.DensityOffsetHeightProvider;
import personthecat.pangaea.world.provider.DynamicColumnProvider;
import personthecat.pangaea.world.provider.ExactColumnProvider;
import personthecat.pangaea.world.provider.VeryBiasedToBottomInt;
import personthecat.pangaea.world.road.RoadMap;
import personthecat.pangaea.world.ruletest.HeterogeneousListRuleTest;
import personthecat.pangaea.world.weight.BiomeFilterWeight;
import personthecat.pangaea.world.weight.ApproximateWeight;
import personthecat.pangaea.world.weight.ConstantWeight;
import personthecat.pangaea.world.weight.CutoffWeight;
import personthecat.pangaea.world.weight.DensityWeight;
import personthecat.pangaea.world.weight.InterpolatedWeight;
import personthecat.pangaea.world.weight.MultipleWeight;
import personthecat.pangaea.world.weight.NeverWeight;
import personthecat.pangaea.world.weight.RouterWeight;
import personthecat.pangaea.world.weight.SumWeight;
import personthecat.pangaea.world.weight.WeightFunction;
import personthecat.pangaea.world.weight.WeightList;

import java.util.List;

@Log4j2
public abstract class Pangaea {
    public static final String ID = "@MOD_ID@";
    public static final String NAME = "@MOD_NAME@";
    public static final String RAW_VERSION = "@MOD_VERSION@";
    public static final Version VERSION = Version.parse(RAW_VERSION);
    public static final ModDescriptor MOD =
        ModDescriptor.builder().modId(ID).name(NAME).commandPrefix("pg").version(VERSION).build();
    public static final VersionTracker VERSION_TRACKER = VersionTracker.trackModVersion(MOD);

    private static final ConfiguredFeature<?, ?> CONFIGURED_DEBUG_WEIGHT =
        new ConfiguredFeature<>(DebugWeightFeature.INSTANCE, FeatureConfiguration.NONE);
    private static final PlacedFeature PLACED_DEBUG_WEIGHT =
        new PlacedFeature(Holder.direct(CONFIGURED_DEBUG_WEIGHT), List.of(
                new IntervalPlacementModifier(4),
                HeightmapPlacement.onHeightmap(Types.WORLD_SURFACE_WG)
            ));
    private static final ConfiguredFeature<?, ?> CONFIGURED_ROAD =
        new ConfiguredFeature<>(RoadFeature.INSTANCE, FeatureConfiguration.NONE);
    private static final PlacedFeature PLACED_ROAD =
        new PlacedFeature(Holder.direct(CONFIGURED_ROAD), List.of());

    protected final void init() {
        Cfg.register();
        updateRegistries();
        CodecAppenders.bootstrap();
        configureCodecs();
    }

    protected final void commonSetup() {
        CommandRegistrationContext.forMod(MOD).addAllCommands(CommandPg.class).addLibCommands().registerAll();
        GameReadyEvent.COMMON.register(() -> {
            if (VERSION_TRACKER.isUpgraded()) {
                log.info("Upgrade detected. Welcome to Pangaea {}", VERSION);
            }
        });
        SeedSupport.setup();
        enableDebugFeatures();
        DataInjectionHook.setup();
    }

    private static void updateRegistries() {
        CommonRegistries.DENSITY_FUNCTION_TYPE.createRegister(ID)
            .register("controller", DensityController.CODEC)
            .register("noise", FastNoiseDensity.CODEC)
            .register("min", DensityList.Min.CODEC)
            .register("max", DensityList.Max.CODEC)
            .register("sum", DensityList.Sum.CODEC)
            .register("normal", NormalDensity.CODEC)
            .register("uniform", UniformDensity.CODEC)
            .register("trapezoid", TrapezoidDensity.CODEC)
            .register("weighted_list", WeightedListDensity.CODEC);
        CommonRegistries.FLOAT_PROVIDER_TYPE.createRegister(ID)
            .register("density", DensityFloatProvider.TYPE)
            .register("biased_to_bottom", BiasedToBottomFloat.TYPE);
        CommonRegistries.INT_PROVIDER_TYPE.createRegister(ID)
            .register("density", DensityIntProvider.TYPE)
            .register("very_biased_to_bottom", VeryBiasedToBottomInt.TYPE);
        CommonRegistries.HEIGHT_PROVIDER_TYPE.createRegister(ID)
            .register("density", DensityHeightProvider.TYPE)
            .register("density_offset", DensityOffsetHeightProvider.TYPE);
        CommonRegistries.PLACEMENT_MODIFIER_TYPE.createRegister(ID)
            .register("simple", SimplePlacementModifier.TYPE)
            .register("surface_biome", SurfaceBiomeFilter.TYPE);
        CommonRegistries.RULE_TEST_TYPE.createRegister(ID)
            .register("heterogeneous_list", HeterogeneousListRuleTest.TYPE);
        PgRegistries.INJECTOR_TYPE.createRegister(ID)
            .register("ore", OreInjector.CODEC)
            .register("cavern", CavernInjector.CODEC)
            .register("biome", BiomeInjector.CODEC)
            .register("biome_modifier", BiomeModifierInjector.CODEC)
            .register("biome_source", BiomeSourceInjector.CODEC)
            .register("dimension", DimensionInjector.CODEC)
            .register("feature", FeatureInjector.CODEC);
        PgRegistries.PLACER_TYPE.createRegister(ID)
            .register("targeted", TargetedBlockPlacer.CODEC)
            .register("column_restricted", ColumnRestrictedBlockPlacer.CODEC)
            .register("chance", ChanceBlockPlacer.CODEC)
            .register("list", BlockPlacerList.CODEC)
            .register("unconditional", UnconditionalBlockPlacer.CODEC);
        PgRegistries.COLUMN_TYPE.createRegister(ID)
            .register("constant", ConstantColumnProvider.CODEC)
            .register("exact", ExactColumnProvider.CODEC)
            .register("dynamic", DynamicColumnProvider.CODEC)
            .register("anchor_range", AnchorRangeColumnProvider.CODEC);
        PgRegistries.CHUNK_FILTER_TYPE.createRegister(ID)
            .register("chance", ChanceChunkFilter.CODEC)
            .register("cluster", ClusterChunkFilter.CODEC)
            .register("predictable", PredictableChunkFilter.CODEC)
            .register("interval", IntervalChunkFilter.CODEC)
            .register("noise", FastNoiseChunkFilter.CODEC)
            .register("spawn_distance", SpawnDistanceChunkFilter.CODEC)
            .register("density", DensityChunkFilter.CODEC)
            .register("union", UnionChunkFilter.CODEC);
        PgRegistries.LINK_TYPE.createRegister(ID)
            .register("sphere", SphereLink.Config.CODEC)
            .register("canyon", CanyonLink.Config.CODEC)
            .register("chasm", ChasmLink.Config.CODEC);
        PgRegistries.PATH_TYPE.createRegister(ID)
            .register("tunnel", TunnelPath.Config.CODEC)
            .register("canyon", CanyonPath.Config.CODEC);
        PgRegistries.WEIGHT_TYPE.createRegister(ID)
            .register("approximate_continentalness", ApproximateWeight.CONTINENTALNESS.codec())
            .register("approximate_depth", ApproximateWeight.DEPTH.codec())
            .register("approximate_pv", ApproximateWeight.PV.codec())
            .register("approximate_weirdness", ApproximateWeight.WEIRDNESS.codec())
            .register("biome", BiomeFilterWeight.CODEC)
            .register("constant", ConstantWeight.CODEC)
            .register("continents", RouterWeight.CONTINENTS.codec())
            .register("cutoff", CutoffWeight.CODEC)
            .register("density", DensityWeight.CODEC)
            .register("erosion", RouterWeight.EROSION.codec())
            .register("final_density", RouterWeight.FINAL_DENSITY.codec())
            .register("initial_density", RouterWeight.INITIAL_DENSITY.codec())
            .register("interpolated_continentalness", InterpolatedWeight.CONTINENTALNESS.codec())
            .register("interpolated_depth", InterpolatedWeight.DEPTH.codec())
            .register("interpolated_pv", InterpolatedWeight.PV.codec())
            .register("interpolated_weirdness", InterpolatedWeight.WEIRDNESS.codec())
            .register("multiple", MultipleWeight.CODEC)
            .register("never", NeverWeight.CODEC)
            .register("sum", SumWeight.CODEC)
            .register("temperature", RouterWeight.TEMPERATURE.codec())
            .register("vegetation", RouterWeight.VEGETATION.codec())
            .register("list", WeightList.CODEC);
        CommonRegistries.FEATURE.createRegister(ID)
            .register("test", TestFeature.INSTANCE)
            .register("giant_sphere", GiantSphereFeature.INSTANCE)
            .register("blob", BlobFeature.INSTANCE)
            .register("density", DensityFeature.INSTANCE)
            .register("burrow", BurrowFeature.INSTANCE)
            .register("chain", ChainFeature.INSTANCE)
            .register("temporary_tunnel", TunnelFeature.INSTANCE)
            .register("temporary_ravine", RavineFeature.INSTANCE);
    }

    private static void configureCodecs() {
        PangaeaCodec.get(ChainFeature.Configuration.class)
            .addPreset("ravine", ChainFeaturePresets.RAVINE)
            .addPreset("tunnel", ChainFeaturePresets.TUNNEL)
            .addPreset("chasm", ChainFeaturePresets.CHASM);
        PangaeaCodec.get(BlockPlacer.class)
            .addBuilderFields(DefaultBuilderFields.PLACER)
            .addBuilderCondition(Cfg::encodeStructuralBlockPlacers)
            .addPatterns(DefaultCodecPatterns.PLACER)
            .addPatternCondition(Cfg::encodePatternBlockPlacers);
        PangaeaCodec.get(DensityFunction.class)
            .addFlags(DefaultCodecFlags.DENSITY)
            .addFlagCondition(Cfg::encodeDensityBuilders)
            .addStructures(DefaultCodecStructures.DENSITY)
            .addStructureCondition(Cfg::encodeStructuralDensity);
        PangaeaCodec.get(FloatProvider.class)
            .addStructures(DefaultCodecStructures.FLOAT)
            .addStructureCondition(Cfg::encodeStructuralFloatProviders)
            .addPatterns(DefaultCodecPatterns.FLOAT)
            .addPatternCondition(Cfg::encodeRangeFloatProvider);
        PangaeaCodec.get(IntProvider.class)
            .addStructures(DefaultCodecStructures.INT)
            .addStructureCondition(Cfg::encodeStructuralIntProviders)
            .addPatterns(DefaultCodecPatterns.INT)
            .addPatternCondition(Cfg::encodeRangeIntProvider);
        PangaeaCodec.get(RuleTest.class)
            .addPatterns(DefaultCodecPatterns.RULE_TEST)
            .addPatternCondition(Cfg::encodePatternRuleTestCodec);
        PangaeaCodec.get(ChunkFilter.class)
            .addStructures(DefaultCodecStructures.CHUNK_FILTER)
            .addStructureCondition(Cfg::encodeStructuralChunkFilters)
            .addPatterns(DefaultCodecPatterns.CHUNK_FILTER)
            .addPatternCondition(Cfg::encodePatternChunkFilters);
        PangaeaCodec.get(WeightFunction.class)
            .addStructures(DefaultCodecStructures.WEIGHT)
            .addPatterns(DefaultCodecPatterns.WEIGHT);
        PangaeaCodec.get(HeightProvider.class)
            .addStructures(DefaultCodecStructures.HEIGHT)
            .addStructureCondition(Cfg::encodeStructuralHeight)
            .addPatterns(DefaultCodecPatterns.HEIGHT)
            .addPatternCondition(Cfg::encodePatternHeightProvider);
        PangaeaCodec.get(ColumnProvider.class)
            .addPatterns(DefaultCodecPatterns.COLUMN)
            .addPatternCondition(Cfg::encodePatternHeightProvider);
    }

    private static void enableDebugFeatures() {
        if (Cfg.removeAllFeatures()) {
            removeAllFeatures();
        }
        if (Cfg.removeAllCarvers()) {
            removeAllCarvers();
        }
        if (Cfg.generateDebugPillars()) {
            generateDebugPillars();
        }
        if (Cfg.enableRoads()) {
            enableRoads();
        }
    }

    private static void removeAllFeatures() {
        FeatureModificationEvent.register(ctx -> {
            log.info("Clearing features from biome: {}", ctx.getName());
            ctx.removeFeature(feature -> true);
        });
    }

    private static void removeAllCarvers() {
        FeatureModificationEvent.register(ctx -> {
            log.info("Clearing carvers from biome: {}", ctx.getName());
            ctx.removeCarver(carver -> true);
        });
    }

    private static void generateDebugPillars() {
        CommonRegistries.FEATURE.deferredRegister(MOD.id("debug_weight"), DebugWeightFeature.INSTANCE);
        CommonRegistries.PLACEMENT_MODIFIER_TYPE.deferredRegister(MOD.id("interval_placement"), IntervalPlacementModifier.TYPE);
        DynamicRegistries.CONFIGURED_FEATURE.deferredRegister(MOD.id("configured_debug_weight"), CONFIGURED_DEBUG_WEIGHT);
        DynamicRegistries.PLACED_FEATURE.deferredRegister(MOD.id("placed_debug_weight"), PLACED_DEBUG_WEIGHT);
        FeatureModificationEvent.register(ctx -> ctx.addFeature(Decoration.TOP_LAYER_MODIFICATION, PLACED_DEBUG_WEIGHT));
    }

    private static void enableRoads() {
        CommonRegistries.FEATURE.deferredRegister(MOD.id("road"), RoadFeature.INSTANCE);
        DynamicRegistries.CONFIGURED_FEATURE.deferredRegister(MOD.id("configured_road"), CONFIGURED_ROAD);
        DynamicRegistries.PLACED_FEATURE.deferredRegister(MOD.id("placed_road"), PLACED_ROAD);

        FeatureModificationEvent.register(ctx -> ctx.addFeature(Decoration.RAW_GENERATION, PLACED_ROAD));
    }

    protected final void shutdown(final MinecraftServer server) {
        RoadMap.clearAll(server);
    }
}
