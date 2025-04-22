package personthecat.pangaea.extras;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import personthecat.pangaea.world.level.PangaeaContext;

public interface WorldGenRegionExtras {
    PangaeaContext pangaea$getPangaeaContext();
    void pangaea$setPangaeaContext(PangaeaContext ctx);

    static PangaeaContext getPangaeaContext(WorldGenLevel level) {
        return get(level).pangaea$getPangaeaContext();
    }

    static void setPangaeaContext(WorldGenLevel level, PangaeaContext ctx) {
        get(level).pangaea$setPangaeaContext(ctx);
    }

    static WorldGenRegionExtras get(WorldGenLevel level) {
        if (level instanceof WorldGenRegionExtras extras) {
            return extras;
        }
        if (!(level instanceof WorldGenRegion)) {
            throw new IllegalArgumentException("Tried to get extras from a non-region level: " + level);
        }
        throw new IllegalStateException("World gen region extras mixin was not applied");
    }
}
