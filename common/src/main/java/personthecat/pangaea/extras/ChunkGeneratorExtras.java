package personthecat.pangaea.extras;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public interface ChunkGeneratorExtras {
    int pangaea$getOptimizedHeight(ChunkAccess chunk, RandomState rand, int blockX, int blockZ);

    static int getOptimizedHeight(ChunkGenerator gen, ChunkAccess chunk, RandomState rand, int blockX, int blockZ) {
        return get(gen).pangaea$getOptimizedHeight(chunk, rand, blockX, blockZ);
    }

    static ChunkGeneratorExtras get(ChunkGenerator gen) {
        if (gen instanceof ChunkGeneratorExtras extras) {
            return extras;
        }
        throw new IllegalStateException("ChunkGenerator extras mixin not applied: " + gen);
    }
}
