package personthecat.pangaea.resources.builtin;

import net.minecraft.core.registries.Registries;
import personthecat.pangaea.resources.ResourceMap;

import java.util.List;
import java.util.Map;

import static personthecat.pangaea.resources.builtin.BuiltInWorldPack.key;

public final class BuiltInDebug {
    private BuiltInDebug() {}

    public static void bootstrap(ResourceMap resources) {
        resources.addDynamicResource(key(Registries.DENSITY_FUNCTION, "density_error"), densityError());
        resources.addDynamicResource(key(Registries.BIOME, "biome_error"), biomeError());
    }

    private static Object densityError() {
        return Map.of(
            "mul", 4,
            "times", Map.of(
                "quarter_negative", Map.of(
                    "add", Map.of(
                        "min", List.of(
                            Map.of("add", 4, "plus", Map.of("never!", true))
                        )
                    ),
                    "plus", Map.of("oops!", true)
                )
            )
        );
    }

    private static Object biomeError() {
        return Map.of(
            "features", List.of(
                List.of(Map.of("nevermore!", true))
            ),
            "effects", Map.of(
                "fog_color", -1
            )
        );
    }
}
