package personthecat.pangaea.config;

import personthecat.catlib.config.Config;
import personthecat.catlib.config.Config.Comment;
import personthecat.catlib.config.Config.NeedsWorldRestart;
import personthecat.catlib.config.ConfigEvaluator;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.world.density.DensityModificationHook;
import personthecat.pangaea.world.road.Road;

public final class Cfg implements Config.Listener {
    private static final Cfg INSTANCE = new Cfg();

    @Comment("Settings to configure road generation")
    Roads roads = new Roads();

    @Comment("Temporary settings until we get presets")
    Temporary temporary = new Temporary();

    @Comment("Settings to configure data functions and codecs")
    Data data = new Data();

    @Override
    public void onConfigUpdated() {
        DensityModificationHook.onConfigUpdated();
    }

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

    public static boolean generateDebugPillars() {
        return INSTANCE.temporary.generateDebugPillars;
    }

    public static boolean enableRoads() {
        return INSTANCE.temporary.enableRoads;
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

    public static boolean enableDensityBuilders() {
        return INSTANCE.data.enableDensityBuilders;
    }

    public static boolean encodeDensityBuilders() {
        return INSTANCE.data.encodeDensityBuilders;
    }

    public static boolean enableStructuralDensity() {
        return INSTANCE.data.enableStructuralDensity;
    }

    public static boolean encodeStructuralDensity() {
        return INSTANCE.data.encodeStructuralDensity;
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
        boolean generateDebugPillars = false;
        boolean enableRoads = true;
        float roadChance = 1.0F / 4000F;
        int maxBranches = 15;
        int minRoadLength = Road.MAX_DISTANCE / 4;
        int maxRoadLength = Road.MAX_DISTANCE;
    }

    static class Data {
        @NeedsWorldRestart
        boolean enableDensityBuilders = true;
        @NeedsWorldRestart
        boolean encodeDensityBuilders = true;
        @NeedsWorldRestart
        boolean enableStructuralDensity = true;
        @NeedsWorldRestart
        boolean encodeStructuralDensity = true;
    }
}
