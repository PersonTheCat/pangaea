package personthecat.pangaea;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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
import personthecat.pangaea.world.density.DensityController;
import personthecat.pangaea.world.density.DensityList;
import personthecat.pangaea.world.density.FastNoiseDensity;
import personthecat.pangaea.world.density.NormalDensity;
import personthecat.pangaea.world.density.TrapezoidDensity;
import personthecat.pangaea.world.density.UniformDensity;
import personthecat.pangaea.world.density.WeightedListDensity;
import personthecat.pangaea.serialization.codec.StructuralDensityCodec;
import personthecat.pangaea.world.feature.DebugWeightFeature;
import personthecat.pangaea.world.feature.RoadFeature;
import personthecat.pangaea.world.feature.TestFeature;
import personthecat.pangaea.world.injector.*;
import personthecat.pangaea.world.placement.SimplePlacementModifier;
import personthecat.pangaea.world.placement.IntervalPlacementModifier;
import personthecat.pangaea.world.placement.SurfaceBiomeFilter;
import personthecat.pangaea.world.placer.*;
import personthecat.pangaea.world.provider.*;
import personthecat.pangaea.world.road.RoadMap;
import personthecat.pangaea.world.ruletest.HeterogeneousListRuleTest;

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
    }

    protected final void commonSetup() {
        CommandRegistrationContext.forMod(MOD).addAllCommands(CommandPg.class).addLibCommands().registerAll();
        GameReadyEvent.COMMON.register(() -> {
            if (VERSION_TRACKER.isUpgraded()) {
                log.info("Upgrade detected. Welcome to Pangaea {}", VERSION);
            }
        });
        DataInjectionHook.setup();
        enableDebugFeatures();
    }

    private static void updateRegistries() {
        CommonRegistries.DENSITY_FUNCTION_TYPE.createRegister(ID)
            .register("controller", DensityController.CODEC)
            .register("structural", StructuralDensityCodec.INSTANCE)
            .register("noise", FastNoiseDensity.CODEC)
            .register("min", DensityList.Min.CODEC)
            .register("max", DensityList.Max.CODEC)
            .register("sum", DensityList.Sum.CODEC)
            .register("normal", NormalDensity.CODEC)
            .register("uniform", UniformDensity.CODEC)
            .register("trapezoid", TrapezoidDensity.CODEC)
            .register("weighted_list", WeightedListDensity.CODEC);
        CommonRegistries.FLOAT_PROVIDER_TYPE.createRegister(ID)
            .register("density", DensityFloatProvider.TYPE);
        CommonRegistries.INT_PROVIDER_TYPE.createRegister(ID)
            .register("density", DensityIntProvider.TYPE);
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
            .register("dynamic", DynamicColumnProvider.CODEC)
            .register("anchor_range", AnchorRangeColumnProvider.CODEC);
        CommonRegistries.FEATURE.createRegister(ID)
            .register("test", TestFeature.INSTANCE);
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
        FeatureModificationEvent.global().register(ctx -> {
            log.info("Clearing features from biome: {}", ctx.getName());
            ctx.removeFeature(feature -> true);
        });
    }

    private static void removeAllCarvers() {
        FeatureModificationEvent.global().register(ctx -> {
            log.info("Clearing carvers from biome: {}", ctx.getName());
            ctx.removeCarver(carver -> true);
        });
    }

    private static void generateDebugPillars() {
        CommonRegistries.FEATURE.deferredRegister(MOD.id("debug_weight"), DebugWeightFeature.INSTANCE);
        CommonRegistries.PLACEMENT_MODIFIER_TYPE.deferredRegister(MOD.id("interval_placement"), IntervalPlacementModifier.TYPE);
        DynamicRegistries.CONFIGURED_FEATURE.deferredRegister(MOD.id("configured_debug_weight"), CONFIGURED_DEBUG_WEIGHT);
        DynamicRegistries.PLACED_FEATURE.deferredRegister(MOD.id("placed_debug_weight"), PLACED_DEBUG_WEIGHT);
        FeatureModificationEvent.global().register(ctx -> ctx.addFeature(Decoration.TOP_LAYER_MODIFICATION, PLACED_DEBUG_WEIGHT));
    }

    private static void enableRoads() {
        CommonRegistries.FEATURE.deferredRegister(MOD.id("road"), RoadFeature.INSTANCE);
        DynamicRegistries.CONFIGURED_FEATURE.deferredRegister(MOD.id("configured_road"), CONFIGURED_ROAD);
        DynamicRegistries.PLACED_FEATURE.deferredRegister(MOD.id("placed_road"), PLACED_ROAD);

        FeatureModificationEvent.global().register(ctx -> ctx.addFeature(Decoration.RAW_GENERATION, PLACED_ROAD));
    }

    protected final void shutdown(final MinecraftServer server) {
        RoadMap.clearAll(server);
    }
}
