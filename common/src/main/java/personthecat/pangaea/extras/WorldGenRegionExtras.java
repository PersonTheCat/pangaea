package personthecat.pangaea.extras;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import personthecat.pangaea.world.level.GenerationContext;

public interface WorldGenRegionExtras {
    GenerationContext pangaea$getGenerationContext();
    void pangaea$setGenerationContext(GenerationContext ctx);

    static GenerationContext getGenerationContext(WorldGenLevel level) {
        return get(level).pangaea$getGenerationContext();
    }

    static void setGenerationContext(WorldGenLevel level, GenerationContext ctx) {
        get(level).pangaea$setGenerationContext(ctx);
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
