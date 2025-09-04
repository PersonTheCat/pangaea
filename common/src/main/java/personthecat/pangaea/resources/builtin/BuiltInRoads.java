package personthecat.pangaea.resources.builtin;

import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.resources.ResourceMap;

import java.util.List;
import java.util.Map;

import static personthecat.pangaea.resources.builtin.BuiltInWorldPack.key;

public final class BuiltInRoads {

    private BuiltInRoads() {}

    public static void bootstrap(ResourceMap resources) {
        resources.addDynamicResource(key(PgRegistries.Keys.ROAD, "road"), road());
        resources.addDynamicResource(key(PgRegistries.Keys.GIANT_FEATURE, "road_feature"), roadFeature());
        if (Cfg.minTreeDistance() != 0) {
            resources.addDynamicResource(key(PgRegistries.Keys.INJECTOR, "placement/tree_distance"), treeDistance());
        }
    }

    private static Object road() {
        return Map.of(
            "type", "pangaea:astar",
            "chunk_filter", Cfg.roadChance(),
            "branches", List.of(Cfg.minRoadBranches(), Cfg.maxRoadBranches()),
            "length", List.of(Cfg.minRoadLength(), Cfg.maxRoadLength()),
            "destination_strategy", Cfg.destinationStrategy().name()
        );
    }

    private static Object roadFeature() {
        return Map.of(
            "type", "pangaea:road",
            "biomes", List.of("!#is_ocean"),
            "vertex_configs", List.of(mainRoad(), branches())
        );
    }

    private static Object mainRoad() {
        return Map.of(
            "path", List.of(
                Map.of("chance", Cfg.secondaryRoadBlockChance(), "place", Cfg.secondaryRoadBlock()),
                Map.of("surface_or", "grass_block")
            ),
            "bounds", List.of(Cfg.minRoadHeight(), Cfg.maxRoadHeight())
        );
    }

    private static Object branches() {
        return Map.of(
            "path", List.of(
                Map.of("chance", Cfg.secondaryBranchBlockChance(), "place", Cfg.secondaryRoadBlock()),
                Map.of("surface_or", Cfg.branchRoadBlock())
            ),
            "bounds", List.of(Cfg.minRoadHeight(), Cfg.maxRoadHeight())
        );
    }

    private static Object treeDistance() {
        return Map.of(
            "features", List.of("tree"),
            "inject", List.of(treeDistanceInjection())
        );
    }

    private static Object treeDistanceInjection() {
        return Map.of(
            "type", "pangaea:road_distance",
            "min", Cfg.minTreeDistance()
        );
    }
}