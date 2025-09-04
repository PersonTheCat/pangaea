package personthecat.pangaea;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.versioning.Version;
import personthecat.catlib.versioning.VersionTracker;
import personthecat.pangaea.command.CommandPg;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.registry.PgRegistries;
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
import personthecat.pangaea.world.injector.CarverInjector;
import personthecat.pangaea.world.injector.CavernInjector;
import personthecat.pangaea.world.injector.DataInjectionHook;
import personthecat.pangaea.world.injector.DimensionInjector;
import personthecat.pangaea.world.injector.FeatureInjector;
import personthecat.pangaea.world.injector.OreInjector;
import personthecat.pangaea.world.injector.PlacementModifierInjector;
import personthecat.pangaea.world.injector.SurfaceRuleInjector;
import personthecat.pangaea.world.placement.IntervalPlacementModifier;
import personthecat.pangaea.world.placement.RoadDistanceFilter;
import personthecat.pangaea.world.placement.SimplePlacementModifier;
import personthecat.pangaea.world.placement.SpawnDistancePlacementFilter;
import personthecat.pangaea.world.placement.SurfaceBiomeFilter;
import personthecat.pangaea.world.placer.BiomeRestrictedBlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacerList;
import personthecat.pangaea.world.placer.ChanceBlockPlacer;
import personthecat.pangaea.world.placer.ColumnRestrictedBlockPlacer;
import personthecat.pangaea.world.placer.SurfaceBlockPlacer;
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
import personthecat.pangaea.world.provider.DensityOffsetVerticalAnchor;
import personthecat.pangaea.world.provider.DensityVerticalAnchor;
import personthecat.pangaea.world.provider.DynamicColumnProvider;
import personthecat.pangaea.world.provider.ExactColumnProvider;
import personthecat.pangaea.world.provider.MiddleVerticalAnchor;
import personthecat.pangaea.world.provider.SeaLevelVerticalAnchor;
import personthecat.pangaea.world.provider.SurfaceVerticalAnchor;
import personthecat.pangaea.world.provider.VeryBiasedToBottomInt;
import personthecat.pangaea.world.road.AStarRoadGenerator;
import personthecat.pangaea.world.road.RoadMap;
import personthecat.pangaea.world.ruletest.HeterogeneousListRuleTest;
import personthecat.pangaea.world.surface.AllConditionSource;
import personthecat.pangaea.world.surface.ChanceConditionSource;
import personthecat.pangaea.world.surface.CheckerPatternConditionSource;
import personthecat.pangaea.world.surface.DensityConditionSource;
import personthecat.pangaea.world.surface.HeterogeneousBiomeConditionSource;
import personthecat.pangaea.world.surface.IntervalConditionSource;
import personthecat.pangaea.world.surface.NeverConditionSource;
import personthecat.pangaea.world.surface.NullSource;
import personthecat.pangaea.world.surface.RoadDistanceConditionSource;
import personthecat.pangaea.world.surface.SpawnDistanceConditionSource;
import personthecat.pangaea.world.surface.SurfaceBiomeConditionSource;
import personthecat.pangaea.world.surface.WeightConditionSource;
import personthecat.pangaea.world.weight.BiomeFilterWeight;
import personthecat.pangaea.world.weight.ApproximateWeight;
import personthecat.pangaea.world.weight.ConstantWeight;
import personthecat.pangaea.world.weight.CutoffWeight;
import personthecat.pangaea.world.weight.DefaultWeight;
import personthecat.pangaea.world.weight.DensityWeight;
import personthecat.pangaea.world.weight.InterpolatedWeight;
import personthecat.pangaea.world.weight.MultipleWeight;
import personthecat.pangaea.world.weight.NeverWeight;
import personthecat.pangaea.world.weight.RouterWeight;
import personthecat.pangaea.world.weight.SumWeight;
import personthecat.pangaea.world.weight.WeightFunction;
import personthecat.pangaea.world.weight.WeightList;

@Log4j2
public abstract class Pangaea {
    public static final String ID = "@MOD_ID@";
    public static final String NAME = "@MOD_NAME@";
    public static final String RAW_VERSION = "@MOD_VERSION@";
    public static final Version VERSION = Version.parse(RAW_VERSION);
    public static final ModDescriptor MOD =
        ModDescriptor.builder().modId(ID).name(NAME).commandPrefix("pg").version(VERSION).build();
    public static final VersionTracker VERSION_TRACKER = VersionTracker.trackModVersion(MOD);

    protected final void init() {
        Cfg.register();
        updateRegistries();
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
            .register("interval", IntervalPlacementModifier.TYPE)
            .register("road_distance", RoadDistanceFilter.TYPE)
            .register("simple", SimplePlacementModifier.TYPE)
            .register("surface_biome", SurfaceBiomeFilter.TYPE)
            .register("spawn_distance", SpawnDistancePlacementFilter.TYPE);
        CommonRegistries.RULE_TEST_TYPE.createRegister(ID)
            .register("heterogeneous_list", HeterogeneousListRuleTest.TYPE);
        PgRegistries.INJECTOR_TYPE.createRegister(ID)
            .register("biome", BiomeInjector.CODEC)
            .register("biome_modifier", BiomeModifierInjector.CODEC)
            .register("biome_source", BiomeSourceInjector.CODEC)
            .register("carver", CarverInjector.CODEC)
            .register("cavern", CavernInjector.CODEC)
            .register("dimension", DimensionInjector.CODEC)
            .register("feature", FeatureInjector.CODEC)
            .register("ore", OreInjector.CODEC)
            .register("placement", PlacementModifierInjector.CODEC)
            .register("surface", SurfaceRuleInjector.CODEC);
        PgRegistries.PLACER_TYPE.createRegister(ID)
            .register("targeted", TargetedBlockPlacer.CODEC)
            .register("column_restricted", ColumnRestrictedBlockPlacer.CODEC)
            .register("chance", ChanceBlockPlacer.CODEC)
            .register("list", BlockPlacerList.CODEC)
            .register("biome_restricted", BiomeRestrictedBlockPlacer.CODEC)
            .register("surface", SurfaceBlockPlacer.CODEC)
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
            .register("interpolated_sd", InterpolatedWeight.SD.codec())
            .register("interpolated_slope", InterpolatedWeight.SLOPE.codec())
            .register("multiple", MultipleWeight.CODEC)
            .register("never", NeverWeight.CODEC)
            .register("sum", SumWeight.CODEC)
            .register("temperature", RouterWeight.TEMPERATURE.codec())
            .register("vegetation", RouterWeight.VEGETATION.codec())
            .register("list", WeightList.CODEC)
            .register("default", DefaultWeight.CODEC);
        PgRegistries.ROAD_TYPE.createRegister(ID)
            .register("astar", AStarRoadGenerator.Configuration.CODEC);
        CommonRegistries.FEATURE.createRegister(ID)
            .register("test", TestFeature.INSTANCE)
            .register("giant_sphere", GiantSphereFeature.INSTANCE)
            .register("blob", BlobFeature.INSTANCE)
            .register("density", DensityFeature.INSTANCE)
            .register("burrow", BurrowFeature.INSTANCE)
            .register("chain", ChainFeature.INSTANCE)
            .register("road", RoadFeature.INSTANCE)
            .register("temporary_tunnel", TunnelFeature.INSTANCE)
            .register("temporary_ravine", RavineFeature.INSTANCE);
        CommonRegistries.MATERIAL_CONDITION.createRegister(ID)
            .register("all", AllConditionSource.CODEC)
            .register("biome", HeterogeneousBiomeConditionSource.CODEC)
            .register("chance", ChanceConditionSource.CODEC)
            .register("checker_pattern", CheckerPatternConditionSource.CODEC)
            .register("density", DensityConditionSource.CODEC)
            .register("interval", IntervalConditionSource.CODEC)
            .register("never", NeverConditionSource.CODEC)
            .register("road_distance", RoadDistanceConditionSource.CODEC)
            .register("spawn_distance", SpawnDistanceConditionSource.CODEC)
            .register("surface_biome", SurfaceBiomeConditionSource.CODEC)
            .register("weight", WeightConditionSource.CODEC);
        CommonRegistries.MATERIAL_RULE.createRegister(ID)
            .register("null", NullSource.CODEC);
    }

    private static void configureCodecs() {
        PangaeaCodec.get(ChainFeature.Configuration.class)
            .addPreset("ravine", ChainFeaturePresets.RAVINE)
            .addPreset("tunnel", ChainFeaturePresets.TUNNEL)
            .addPreset("chasm", ChainFeaturePresets.CHASM);

        PangaeaCodec.get(BlockPlacer.class)
            .addBuilderFields(DefaultBuilderFields.PLACER)
            .addBuilderCondition(Cfg::encodeStructuralBlockPlacers)
            .addStructures(DefaultCodecStructures.PLACER)
            .addStructureCondition(Cfg::encodeStructuralBlockPlacers)
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
            .addPatternCondition(Cfg::encodePatternFloatProvider);

        PangaeaCodec.get(IntProvider.class)
            .addStructures(DefaultCodecStructures.INT)
            .addStructureCondition(Cfg::encodeStructuralIntProviders)
            .addPatterns(DefaultCodecPatterns.INT)
            .addPatternCondition(Cfg::encodePatternIntProvider);

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

        PangaeaCodec.get(VerticalAnchor.class)
            .addAlternative(DensityOffsetVerticalAnchor.CODEC)
            .addAlternative(DensityVerticalAnchor.CODEC)
            .addAlternative(MiddleVerticalAnchor.CODEC)
            .addAlternative(SeaLevelVerticalAnchor.CODEC)
            .addAlternative(SurfaceVerticalAnchor.CODEC)
            .addFlags(DefaultCodecFlags.ANCHOR)
            .addFlagCondition(Cfg::encodeVerticalAnchorBuilders)
            .addStructures(DefaultCodecStructures.ANCHOR)
            .addStructureCondition(Cfg::encodeStructuralHeight)
            .addPatterns(DefaultCodecPatterns.ANCHOR)
            .addPatternCondition(Cfg::encodePatternHeightProvider);

        PangaeaCodec.get(HeightProvider.class)
            .addStructures(DefaultCodecStructures.HEIGHT)
            .addFlags(DefaultCodecFlags.HEIGHT)
            .addFlagCondition(Cfg::encodeVerticalAnchorBuilders)
            .addStructureCondition(Cfg::encodeStructuralHeight)
            .addPatterns(DefaultCodecPatterns.HEIGHT)
            .addPatternCondition(Cfg::encodePatternHeightProvider);

        PangaeaCodec.get(ColumnProvider.class)
            .addPatterns(DefaultCodecPatterns.COLUMN)
            .addPatternCondition(Cfg::encodePatternHeightProvider);

        PangaeaCodec.get(ConditionSource.class)
            .addStructures(DefaultCodecStructures.CONDITION_SOURCE)
            .addStructureCondition(Cfg::encodeStructuralConditions)
            .addPatterns(DefaultCodecPatterns.CONDITION_SOURCE)
            .addPatternCondition(Cfg::encodePatternConditions);

        PangaeaCodec.get(RuleSource.class)
            .addStructures(DefaultCodecStructures.RULE_SOURCE)
            .addStructureCondition(Cfg::encodeStructuralRules)
            .addPatterns(DefaultCodecPatterns.RULE_SOURCE)
            .addPatternCondition(Cfg::encodePatternRules);
    }

    private static void enableDebugFeatures() {
        if (Cfg.removeAllFeatures()) {
            removeAllFeatures();
        }
        if (Cfg.removeAllCarvers()) {
            removeAllCarvers();
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

    protected final void shutdown(final MinecraftServer server) {
        RoadMap.clearAll(server);
    }
}
