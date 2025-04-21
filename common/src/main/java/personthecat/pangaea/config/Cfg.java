package personthecat.pangaea.config;

import personthecat.catlib.config.Config.Comment;
import personthecat.catlib.config.ConfigEvaluator;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.world.road.Road;

public final class Cfg {
    private static final Cfg INSTANCE = new Cfg();

    @Comment("Settings to configure road generation")
    Roads roads = new Roads();

    @Comment("Temporary settings until we get presets")
    Temporary temporary = new Temporary();

    @Comment("Settings to configure data functions and codecs")
    Data data = new Data();

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
        return INSTANCE.temporary.removeAllFeatures;
    }

    public static boolean removeAllCarvers() {
        return INSTANCE.temporary.removeAllCarvers;
    }

    public static boolean generateDebugPillars() {
        return INSTANCE.temporary.generateDebugPillars;
    }

    public static boolean enableRoads() {
        return INSTANCE.temporary.enableRoads;
    }

    public static boolean enableTemporaryDebugFeatures() {
        return INSTANCE.temporary.enableTemporaryDebugFeatures;
    }

    public static boolean optimizeBiomeLayouts() {
        return INSTANCE.temporary.optimizeBiomeLayouts;
    }

    public static float roadChance() {
        return INSTANCE.temporary.roadChance;
    }

    public static int maxBranches() {
        return INSTANCE.temporary.maxBranches;
    }

    public static int minRoadLength() {
        return INSTANCE.temporary.minRoadLength;
    }

    public static int maxRoadLength() {
        return INSTANCE.temporary.maxRoadLength;
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

    public static boolean encodePatternRuleTestCodec() {
        return INSTANCE.data.encodePatternRuleTestCodec;
    }

    public static boolean encodePatternBlockPlacers() {
        return INSTANCE.data.encodePatternBlockPlacers;
    }

    public static boolean encodeRangeIntProvider() {
        return INSTANCE.data.encodeRangeIntProvider;
    }

    public static boolean encodeRangeFloatProvider() {
        return INSTANCE.data.encodeRangeFloatProvider;
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

    static class Roads {
        boolean pregenRoads = false;
        boolean debugPregenShape = false;
        boolean generatePartial = true;
        boolean persistRoads = true;
        boolean debugBranches = false;
        int pregenRadius = 5;
        int pregenThreadCount = 1;
        float pregenSkew = 0.25F;
    }

    static class Temporary {
        boolean removeAllFeatures = false;
        boolean removeAllCarvers = false;
        boolean generateDebugPillars = false;
        boolean enableRoads = true;
        boolean enableTemporaryDebugFeatures = true;
        boolean optimizeBiomeLayouts = true;
        float roadChance = 1.0F / 4000F;
        int maxBranches = 15;
        int minRoadLength = Road.MAX_DISTANCE / 4;
        int maxRoadLength = Road.MAX_DISTANCE;
    }

    static class Data {
        boolean encodeDensityBuilders = true;
        boolean encodeStructuralDensity = true;
        boolean encodeStructuralHeight = true;
        boolean encodeStructuralIntProviders = true;
        boolean encodeStructuralFloatProviders = true;
        boolean encodeStructuralBlockPlacers = true;
        boolean encodePatternRuleTestCodec = true;
        boolean encodePatternBlockPlacers = true;
        boolean encodeRangeIntProvider = true;
        boolean encodeRangeFloatProvider = true;
        boolean encodePatternHeightProvider = true;
        boolean encodeDefaultStateAsBlock = true;
        boolean encodeStatePropertiesAsList = true;
        boolean encodeReadableColors = true;
        boolean encodeFeatureCategories = true;
    }
}
