package personthecat.pangaea.config;

import personthecat.catlib.config.Config.Comment;
import personthecat.catlib.config.Config.DecimalRange;
import personthecat.catlib.config.Config.NeedsWorldRestart;
import personthecat.catlib.config.Config.Range;
import personthecat.catlib.config.ConfigEvaluator;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.world.road.Road;
import personthecat.pangaea.world.road.RoadConfig.DestinationStrategy;

import java.util.List;

public final class Cfg {
    private static final Cfg INSTANCE = new Cfg();

    @Comment("Settings to configure road generation")
    Roads roads = new Roads();

    @Comment("Debug settings that may be useful for testing")
    Debug debug = new Debug();

    @Comment("Settings to configure data functions and codecs")
    Data data = new Data();

    @NeedsWorldRestart
    @Comment("Settings to configure the built-in world pack")
    BuiltInPacks builtInPacks = new BuiltInPacks();

    public static void register() {
        ConfigEvaluator.loadAndRegister(Pangaea.MOD, INSTANCE);
    }

    public static boolean debugPregenShape() {
        return INSTANCE.roads.debugPregenShape;
    }

    public static boolean generatePartial() {
        return INSTANCE.roads.generatePartial;
    }

    public static boolean persistRoads() {
        return INSTANCE.roads.persistRoads;
    }

    public static boolean debugBranches() {
        return INSTANCE.roads.debugBranches;
    }

    public static int pregenRadius() {
        return INSTANCE.roads.pregenRadius;
    }

    public static int pregenThreadCount() {
        return INSTANCE.roads.pregenThreadCount;
    }

    public static float pregenSkew() {
        return INSTANCE.roads.pregenSkew;
    }

    public static boolean removeAllFeatures() {
        return INSTANCE.debug.removeAllFeatures;
    }

    public static boolean removeAllCarvers() {
        return INSTANCE.debug.removeAllCarvers;
    }

    public static boolean enableTemporaryDebugFeatures() {
        return INSTANCE.debug.enableTemporaryDebugFeatures;
    }

    public static boolean optimizeBiomeLayouts() {
        return INSTANCE.debug.optimizeBiomeLayouts;
    }

    public static boolean encodeDensityBuilders() {
        return INSTANCE.data.encodeDensityBuilders;
    }

    public static boolean encodeStructuralDensity() {
        return INSTANCE.data.encodeStructuralDensity;
    }

    public static boolean encodeStructuralHeight() {
        return INSTANCE.data.encodeStructuralHeight;
    }

    public static boolean encodeStructuralIntProviders() {
        return INSTANCE.data.encodeStructuralIntProviders;
    }

    public static boolean encodeStructuralFloatProviders() {
        return INSTANCE.data.encodeStructuralFloatProviders;
    }

    public static boolean encodeStructuralBlockPlacers() {
        return INSTANCE.data.encodeStructuralBlockPlacers;
    }

    public static boolean encodeStructuralChunkFilters() {
        return INSTANCE.data.encodeStructuralChunkFilters;
    }

    public static boolean encodePatternRuleTestCodec() {
        return INSTANCE.data.encodePatternRuleTestCodec;
    }

    public static boolean encodePatternBlockPlacers() {
        return INSTANCE.data.encodePatternBlockPlacers;
    }

    public static boolean encodePatternChunkFilters() {
        return INSTANCE.data.encodePatternChunkFilters;
    }

    public static boolean encodePatternIntProvider() {
        return INSTANCE.data.encodePatternIntProvider;
    }

    public static boolean encodePatternFloatProvider() {
        return INSTANCE.data.encodePatternFloatProvider;
    }

    public static boolean encodeVerticalAnchorBuilders() {
        return INSTANCE.data.encodeVerticalAnchorBuilders;
    }

    public static boolean encodePatternHeightProvider() {
        return INSTANCE.data.encodePatternHeightProvider;
    }

    public static boolean encodeDefaultStateAsBlock() {
        return INSTANCE.data.encodeDefaultStateAsBlock;
    }

    public static boolean encodeStatePropertiesAsList() {
        return INSTANCE.data.encodeStatePropertiesAsList;
    }

    public static boolean encodeReadableColors() {
        return INSTANCE.data.encodeReadableColors;
    }

    public static boolean encodeFeatureCategories() {
        return INSTANCE.data.encodeFeatureCategories;
    }

    public static boolean encodePatternConditions() {
        return INSTANCE.data.encodePatternConditions;
    }

    public static boolean encodeStructuralConditions() {
        return INSTANCE.data.encodeStructuralConditions;
    }

    public static boolean encodePatternRules() {
        return INSTANCE.data.encodePatternRules;
    }

    public static boolean encodeStructuralRules() {
        return INSTANCE.data.encodeStructuralRules;
    }

    public static boolean enableBuiltInPack() {
        return INSTANCE.builtInPacks.general.enabledBuiltInPack;
    }

    public static boolean enableRoads() {
        return INSTANCE.builtInPacks.roads.enableRoads;
    }

    public static double roadChance() {
        return INSTANCE.builtInPacks.roads.roadChance;
    }

    public static int minRoadBranches() {
        return INSTANCE.builtInPacks.roads.minBranches;
    }

    public static int maxRoadBranches() {
        return INSTANCE.builtInPacks.roads.maxBranches;
    }

    public static int minRoadLength() {
        return INSTANCE.builtInPacks.roads.minLength;
    }

    public static int maxRoadLength() {
        return INSTANCE.builtInPacks.roads.maxLength;
    }

    public static int minRoadHeight() {
        return INSTANCE.builtInPacks.roads.minHeight;
    }

    public static int maxRoadHeight() {
        return INSTANCE.builtInPacks.roads.maxHeight;
    }

    public static DestinationStrategy destinationStrategy() {
        return INSTANCE.builtInPacks.roads.destinationStrategy;
    }

    public static String secondaryRoadBlock() {
        return INSTANCE.builtInPacks.roads.secondaryBlock;
    }

    public static String branchRoadBlock() {
        return INSTANCE.builtInPacks.roads.branchBlock;
    }

    public static double secondaryRoadBlockChance() {
        return INSTANCE.builtInPacks.roads.secondaryBlockChance;
    }

    public static double secondaryBranchBlockChance() {
        return INSTANCE.builtInPacks.roads.secondaryBlockBranchChance;
    }

    public static int minTreeDistance() {
        return INSTANCE.builtInPacks.roads.minTreeDistance;
    }

    public static boolean enableChasms() {
        return INSTANCE.builtInPacks.chasms.enableChasms;
    }

    public static List<String> chasmBiomes() {
        return INSTANCE.builtInPacks.chasms.chasmBiomes;
    }

    public static float chasmChance() {
        return INSTANCE.builtInPacks.chasms.chasmChance;
    }

    public static float minChasmRadius() {
        return INSTANCE.builtInPacks.chasms.minRadius;
    }

    public static float maxChasmRadius() {
        return INSTANCE.builtInPacks.chasms.maxRadius;
    }

    public static float chasmVerticalScale() {
        return INSTANCE.builtInPacks.chasms.verticalScale;
    }

    public static float minChasmTiltDegrees() {
        return INSTANCE.builtInPacks.chasms.minTiltDegrees;
    }

    public static float maxChasmTiltDegrees() {
        return INSTANCE.builtInPacks.chasms.maxTiltDegrees;
    }

    public static float chasmWallFrequency() {
        return INSTANCE.builtInPacks.chasms.wallFrequency;
    }

    public static int chasmWallDepth() {
        return INSTANCE.builtInPacks.chasms.wallDepth;
    }

    public static boolean modifyTerrainShape() {
        return INSTANCE.builtInPacks.terrain.modifyTerrainShape;
    }

    public static boolean enableSurfaceEntrances() {
        return INSTANCE.builtInPacks.terrain.enableSurfaceEntrances;
    }

    public static double surfaceEntranceScale() {
        return INSTANCE.builtInPacks.terrain.surfaceEntranceScale;
    }

    public static int upperTerrainCutoff() {
        return INSTANCE.builtInPacks.terrain.upperCutoff;
    }

    public static int lowerTerrainCutoff() {
        return INSTANCE.builtInPacks.terrain.lowerCutoff;
    }

    public static boolean enableDensityCaves() {
        return INSTANCE.builtInPacks.terrain.enableDensityCaves;
    }

    public static boolean enableCheeseCaves() {
        return INSTANCE.builtInPacks.terrain.enableCheeseCaves;
    }

    public static boolean enableEntrancesUnderground() {
        return INSTANCE.builtInPacks.terrain.enableEntrancesUnderground;
    }

    public static boolean enableSpaghettiCaves() {
        return INSTANCE.builtInPacks.terrain.enableSpaghettiCaves;
    }

    public static boolean enableNoodleCaves() {
        return INSTANCE.builtInPacks.terrain.enableNoodleCaves;
    }

    public static boolean enableSpeleothems() {
        return INSTANCE.builtInPacks.terrain.enableSpeleothems;
    }

    static class Roads {
        @Comment("Whether to generate several road regions ahead of time when the world\n" +
                "is initially generated (tbd)")
        boolean pregenRoads = false;

        @Comment("Whether to print the shape of the regions generated ahead of time")
        boolean debugPregenShape = false;

        @Comment("Whether to support persisting generated road region quadrants to\n" +
                "completely avoid redundant generation")
        boolean generatePartial = true;

        @Comment("Whether to persist road data to the disk or to regenerate the data\n" +
                "each time the game loads")
        boolean persistRoads = true;

        @Comment("Whether to display log details indicating the precise coordinates of\n" +
                "each road branch")
        boolean debugBranches = false;

        @Comment("The radius of road regions (2048^2 blocks) to generate ahead of time")
        int pregenRadius = 5;

        @Comment("The number of threads to use for the pregenerator (different alg for\n" +
                "single vs multithreaded)")
        int pregenThreadCount = 1;

        @Comment("Describes the angle of the plus-shaped pattern used by the pregenerator\n" +
                "(1.0 is a full circle, lower values approximate a plus shape)")
        float pregenSkew = 0.25F;
    }

    static class Debug {
        @Comment("Whether to remove all pre-injection terrain features (trees, ores, etc)")
        boolean removeAllFeatures = false;

        @Comment("Whether to remove all pre-injection world carvers (tunnels, ravines, etc)")
        boolean removeAllCarvers = false;

        @Comment("Whether to enable data pack features flagged as temporary / debug")
        boolean enableTemporaryDebugFeatures = false;

        @Comment("Experimental solution to merge like values in the biome layouts, slices,\n" +
                "and dimension layouts (if using DimensionInjector)")
        boolean optimizeBiomeLayouts = true;
    }

    static class Data {
        boolean encodeDensityBuilders = true;
        boolean encodeStructuralDensity = true;
        boolean encodeStructuralHeight = true;
        boolean encodeStructuralIntProviders = true;
        boolean encodeStructuralFloatProviders = true;
        boolean encodeStructuralBlockPlacers = true;
        boolean encodeStructuralChunkFilters = true;
        boolean encodePatternRuleTestCodec = true;
        boolean encodePatternBlockPlacers = true;
        boolean encodePatternChunkFilters = true;
        boolean encodePatternIntProvider = true;
        boolean encodePatternFloatProvider = true;
        boolean encodeVerticalAnchorBuilders = true;
        boolean encodePatternHeightProvider = true;
        boolean encodeDefaultStateAsBlock = true;
        boolean encodeStatePropertiesAsList = true;
        boolean encodeReadableColors = true;
        boolean encodeFeatureCategories = true;
        boolean encodePatternConditions = true;
        boolean encodeStructuralConditions = true;
        boolean encodePatternRules = true;
        boolean encodeStructuralRules = true;
    }

    static class BuiltInPacks {
        @Comment("General settings for the built-in pack")
        General general = new General();

        @Comment("Configure the roads added by the built-in pack")
        Roads roads = new Roads();

        @Comment("Configure the chasm features generated by the built-in pack")
        Chasms chasms = new Chasms();

        @Comment("Configure the terrain modifications added by the built-in pack")
        Terrain terrain = new Terrain();

        static class General {
            @Comment("Whether to completely enable or disable the built-in pack")
            boolean enabledBuiltInPack = true;
        }

        static class Roads {
            @Comment("Whether to configure roads to generate via the built-in pack")
            boolean enableRoads = true;

            @Comment("The chance to attempt generating a main road in any given chunk")
            @DecimalRange(min = 0.0, max = 1.0)
            double roadChance = 1.0 / 400.0;

            @Comment("Min possible number of branches to generate off the main road (attempts only")
            @Range(min = 0, max = 32)
            int minBranches = 0;

            @Comment("Max possible number of branches to generate off the main road (attempts only)")
            @Range(min = 0, max = 32)
            int maxBranches = 15;

            @Comment("Min length in blocks of any generated main road")
            @Range(min = 0, max = Road.MAX_LENGTH)
            int minLength = 400;

            @Comment("Max length in blocks of any generated main road")
            @Range(min = 0, max = Road.MAX_LENGTH)
            int maxLength = 800;

            @Comment("Min height for roads to generate. Lower will produce land bridges")
            @Range(min = 0, max = 128)
            int minHeight = 62;

            @Comment("Max height for roads to generate. Higher will dig tunnels")
            @Range(min = 0, max = 128)
            int maxHeight = 90;

            @Comment("Whether to generate between 2 points or best terrain away from 1")
            DestinationStrategy destinationStrategy = DestinationStrategy.DEFAULT;

            @Comment("The secondary block to get placed in all road levels")
            String secondaryBlock = "gravel";

            @Comment("The primary block to place in road branches")
            String branchBlock = "coarse_dirt";

            @Comment("How likely to place the secondary block in the main road")
            @DecimalRange(min = 0.0, max = 1.0)
            double secondaryBlockChance = 0.75;

            @Comment("How likely to place the secondary block in road branches")
            @DecimalRange(min = 0.0, max = 1.0)
            double secondaryBlockBranchChance = 0.5;

            @Comment("The minimum distance from roads that tree features may spawn")
            @Range(min = 0, max = 16)
            int minTreeDistance = 5;
        }

        static class Chasms {
            @Comment("Whether to enable underground chasms (similar to diagonal ravines)")
            boolean enableChasms = true;

            @Comment("ID list of any biomes that support the chasm feature")
            List<String> chasmBiomes = List.of();

            @Comment("The chance to start generating a chasm in any given chunk")
            @DecimalRange(min = 0.0, max = 1.0)
            float chasmChance = 0.01F;

            @Comment("Min horizontal radius (before tilt) of any chasm segment")
            @DecimalRange(min = 0.0, max = 8.0)
            float minRadius = 1.5F;

            @Comment("Max horizontal radius (before tilt) of any chasm segment")
            @DecimalRange(min = 0.0, max = 8.0)
            float maxRadius = 2.5F;

            @Comment("Vertical multiple of horizontal radius (before tilt) of each chasm segment")
            @DecimalRange(min = 0.0, max = 10.0)
            float verticalScale = 3.0F;

            @Comment("Min tilt amount in degrees of any chasm segment")
            @DecimalRange(min = 0.0, max = 360.0)
            float minTiltDegrees = 22.5F;

            @Comment("Max tilt amount in degrees of any chasm segment")
            @DecimalRange(min = 0.0, max = 360.0)
            float maxTiltDegrees = 67.5F;

            @Comment("Wall noise frequency (higher values are more erratic, lower is smoother)")
            float wallFrequency = 0.1F;

            @Comment("Max depth wall-wave depth of any chasm segment")
            @Range(min = 0, max = 10)
            int wallDepth = 2;
        }

        static class Terrain {
            @Comment("Whether to modify density values for the overworld dimension")
            boolean modifyTerrainShape = true;

            @Comment("Whether to allow some noise caves to carve through the surface")
            boolean enableSurfaceEntrances = true;

            @Comment("Scale used to adjust the size of surface cave entrances")
            double surfaceEntranceScale = 5.0;

            @Comment("Overall cutoff where any terrain must fade to air")
            int upperCutoff = 240;

            @Comment("Overall cutoff where any terrain must fade to stone")
            int lowerCutoff = -40;

            @Comment("Whether to allow any density caves to generate")
            boolean enableDensityCaves = true;

            @Comment("Whether to enable Mojang's 'cheese' caves")
            boolean enableCheeseCaves = true;

            @Comment("Whether to enable entrance features deeper underground")
            boolean enableEntrancesUnderground = true;

            @Comment("Whether to enable Mojang's 'spaghetti' caves")
            boolean enableSpaghettiCaves = true;

            @Comment("Whether to enable Mojang's 'noodle' caves")
            boolean enableNoodleCaves = true;

            @Comment("Whether to fill underground caves with speleothem-like structures")
            boolean enableSpeleothems = true;
        }
    }
}
