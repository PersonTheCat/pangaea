package personthecat.pangaea.resources.worldpack;

import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;

public final class WorldPackSelectionConfig {
    public static final PackSelectionConfig REQUIRED =
        new PackSelectionConfig(true, Pack.Position.TOP, false);
    public static final PackSelectionConfig OPTIONAL =
        new PackSelectionConfig(false, Pack.Position.TOP, false);

    public static PackSelectionConfig get(boolean required) {
        return required ? REQUIRED : OPTIONAL;
    }

    private WorldPackSelectionConfig() {}
}
