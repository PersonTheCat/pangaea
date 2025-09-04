package personthecat.pangaea.resources.builtin;

import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.resources.ResourceMap;

import java.util.List;
import java.util.Map;

import static personthecat.pangaea.resources.builtin.BuiltInWorldPack.key;

public final class BuiltInChasms {
    private BuiltInChasms() {}

    public static void bootstrap(ResourceMap resources) {
        resources.addDynamicResource(key(PgRegistries.Keys.GIANT_FEATURE, "chasm_feature"), chasmFeature());
    }

    private static Object chasmFeature() {
        return Map.of(
            "type", "pangaea:chain",
            "preset", "chasm",
            "biomes", Cfg.chasmBiomes(),
            "placer", caveAirPlacer(),
            "chunk_filter", Cfg.chasmChance(),
            "height", chasmHeight(),
            "link", chasmLink()
        );
    }

    private static Object caveAirPlacer() {
        return Map.of(
            "target", "#overworld_carver_replaceables",
            "place", "cave_air"
        );
    }

    private static Object chasmHeight() {
        return Map.of(
            "bottom", List.of(8, 60)
        );
    }

    private static Object chasmLink() {
        return Map.of(
            "radius", List.of(Cfg.minChasmRadius(), Cfg.maxChasmRadius()),
            "vertical_scale", Cfg.chasmVerticalScale(),
            "tilt", List.of(Math.toRadians(Cfg.minChasmTiltDegrees()), Math.toRadians(Cfg.maxChasmTiltDegrees())),
            "noise", wallNoise()
        );
    }

    private static Object wallNoise() {
        return Map.of(
            "noise", "perlin",
            "frequency", Cfg.chasmWallFrequency(),
            "range", List.of(0, Cfg.chasmWallDepth())
        );
    }
}
