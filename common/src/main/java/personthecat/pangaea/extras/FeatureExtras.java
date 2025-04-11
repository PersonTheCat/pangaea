package personthecat.pangaea.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public interface FeatureExtras<FC extends FeatureConfiguration> {
    Codec<FC> pangaea$getCodec();

    static <FC extends FeatureConfiguration> Codec<FC> getCodec(Feature<FC> feature) {
        return get(feature).pangaea$getCodec();
    }

    @SuppressWarnings("unchecked")
    static <FC extends FeatureConfiguration> FeatureExtras<FC> get(Feature<FC> feature) {
        if (feature instanceof FeatureExtras<?> extras) {
            return (FeatureExtras<FC>) extras;
        }
        throw new IllegalStateException("Feature extras mixin was not applied");
    }
}
