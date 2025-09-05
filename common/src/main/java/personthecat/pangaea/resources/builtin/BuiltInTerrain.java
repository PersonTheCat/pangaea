package personthecat.pangaea.resources.builtin;

import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.resources.ResourceMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static personthecat.pangaea.resources.builtin.BuiltInWorldPack.key;

public final class BuiltInTerrain {

    public static void bootstrap(ResourceMap resources) {
        resources.addDynamicResource(key(PgRegistries.Keys.INJECTOR, "density/overworld_modifications"), terrain());
    }

    private static Object terrain() {
        final var map = new HashMap<>();
        map.put("dimensions", List.of("overworld"));
        map.put("surface", "overworld/sloped_cheese");
        map.put("upper_cutoff", upperCutoff());
        map.put("lower_cutoff", lowerCutoff());

        if (Cfg.enableDensityCaves()) {
            if (Cfg.enableSurfaceEntrances()) {
                map.put("entrances", entrances());
            }
            final var undergroundCaverns = undergroundCaverns();
            if (!undergroundCaverns.isEmpty()) {
                map.put("underground_caverns", undergroundCaverns);
            }
            final var globalCaverns = globalCaverns();
            if (!globalCaverns.isEmpty()) {
                map.put("global_caverns", globalCaverns);
            }
            if (Cfg.enableSpeleothems()) {
                map.put("underground_filler", List.of("overworld/caves/pillars"));
            }
        }
        return map;
    }

    private static Object upperCutoff() {
        return Map.of(
            "range", List.of(Cfg.upperTerrainCutoff(), 256),
            "harshness", 0.078125
        );
    }

    private static Object lowerCutoff() {
        return Map.of(
            "range", List.of(-64, Cfg.lowerTerrainCutoff()),
            "harshness", 0.1171875
        );
    }

    private static Object entrances() {
        return Map.of(
            "mul", Cfg.surfaceEntranceScale(),
            "times", "overworld/caves/entrances"
        );
    }

    private static List<Object> undergroundCaverns() {
        final var list = new ArrayList<>();
        if (Cfg.enableCheeseCaves()) {
            list.add("pangaea:overworld/caves/cheese");
        }
        if (Cfg.enableEntrancesUnderground()) {
            list.add("overworld/caves/entrances");
        }
        if (Cfg.enableSpaghettiCaves()) {
            list.add("pangaea:overworld/caves/spaghetti");
        }
        return list;
    }

    private static List<Object> globalCaverns() {
        final var list = new ArrayList<>();
        if (Cfg.enableNoodleCaves()) {
            list.add("overworld/caves/noodle");
        }
        return list;
    }
}
