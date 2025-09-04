package personthecat.pangaea.resources.builtin;

import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.resources.ResourceMap;

import java.util.ArrayList;
import java.util.Map;

import static personthecat.pangaea.resources.builtin.BuiltInWorldPack.key;

public final class BuiltInGeneral {
    private BuiltInGeneral() {}

    public static void boostrap(ResourceMap resources) {
        if (Cfg.removeTunnels() || Cfg.removeRavines()) {
            resources.addDynamicResource(key(PgRegistries.Keys.INJECTOR, "carver/removals"), removals());
        }
    }

    private static Object removals() {
        final var ids = new ArrayList<>();
        if (Cfg.removeTunnels()) {
            ids.add("cave");
            ids.add("cave_extra_underground");
        }
        if (Cfg.removeRavines()) {
            ids.add("canyon");
        }
        return Map.of("remove", ids);
    }
}
