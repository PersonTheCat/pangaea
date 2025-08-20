package personthecat.pangaea.extras;

import net.minecraft.world.level.chunk.ChunkAccess;

public interface ChunkAccessExtras {
    void pangaea$setApproximateHeight(int x, int z, int h);
    int pangaea$getApproximateHeight(int x, int z);
    int pangaea$getInterpolatedHeight(int x, int z);

    static void setApproximateHeight(ChunkAccess chunk, int x, int z, int h) {
        get(chunk).pangaea$setApproximateHeight(x, z, h);
    }

    static int getApproximateHeight(ChunkAccess chunk, int x, int z) {
        return get(chunk).pangaea$getApproximateHeight(x, z);
    }

    static int getInterpolatedHeight(ChunkAccess chunk, int x, int z) {
        return get(chunk).pangaea$getInterpolatedHeight(x, z);
    }

    static ChunkAccessExtras get(ChunkAccess chunk) {
        if (chunk instanceof ChunkAccessExtras extras) {
            return extras;
        }
        throw new IllegalStateException("ChunkAccess extras mixin not applied: " + chunk);
    }
}
