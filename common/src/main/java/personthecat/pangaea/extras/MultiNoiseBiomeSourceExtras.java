package personthecat.pangaea.extras;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import personthecat.pangaea.world.biome.ParameterListModifierListener;

public interface MultiNoiseBiomeSourceExtras {
    void pangaea$modifyBiomeParameters(ParameterListModifierListener listener);

    static void modifyBiomeParameters(BiomeSource source, ParameterListModifierListener listener) {
        get(source).pangaea$modifyBiomeParameters(listener);
    }

    static MultiNoiseBiomeSourceExtras get(BiomeSource source) {
        if (source instanceof MultiNoiseBiomeSourceExtras extras) {
            return extras;
        }
        if (!(source instanceof MultiNoiseBiomeSource)) {
            throw new IllegalArgumentException("Tried to get extras from non-noise biome source: " + source);
        }
        throw new IllegalStateException("MultiNoiseBiomeSource extras mixin was not applied");
    }
}
