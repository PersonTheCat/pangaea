package personthecat.pangaea.world.level;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import personthecat.pangaea.world.map.GenerationContext;

public interface WorldGenRegionExtras {
    GenerationContext pangaea$getGenerationContext();
    void pangaea$setGenerationContext(GenerationContext ctx);
    int pangaea$getGeneratingFeatureIndex();
    void pangaea$setGeneratingFeatureIndex(int idx);

    default void pangaea$incrementGeneratingFeatureIndex() {
        this.pangaea$setGeneratingFeatureIndex(this.pangaea$getGeneratingFeatureIndex() + 1);
    }

    static GenerationContext getGenerationContext(WorldGenLevel level) {
        return get(level).pangaea$getGenerationContext();
    }

    static void setGenerationContext(WorldGenLevel level, GenerationContext ctx) {
        get(level).pangaea$setGenerationContext(ctx);
    }

    static int getGeneratingFeatureIndex(WorldGenLevel level) {
        return get(level).pangaea$getGeneratingFeatureIndex();
    }

    static void incrementGeneratingFeatureIndex(WorldGenLevel level) {
        get(level).pangaea$incrementGeneratingFeatureIndex();
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
