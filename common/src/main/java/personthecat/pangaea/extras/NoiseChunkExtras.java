package personthecat.pangaea.extras;

import net.minecraft.world.level.levelgen.NoiseChunk;

public interface NoiseChunkExtras {
    double pangaea$computeDensity();

    static double computeDensity(NoiseChunk noiseChunk) {
        return get(noiseChunk).pangaea$computeDensity();
    }

    static NoiseChunkExtras get(NoiseChunk noiseChunk) {
        if (noiseChunk instanceof NoiseChunkExtras extras) {
            return extras;
        }
        throw new IllegalStateException("NoiseChunk extras mixin not applied: " + noiseChunk);
    }
}
