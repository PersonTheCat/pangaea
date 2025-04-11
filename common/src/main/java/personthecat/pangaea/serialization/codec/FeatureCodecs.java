package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.extras.FeatureExtras;

public final class FeatureCodecs {
    // Schema: { type: typeof Feature<FC> } & FC
    public static final MapCodec<ConfiguredFeature<?, ?>> FLAT_CONFIG =
        CommonRegistries.FEATURE.codec()
            .dispatchMap(ConfiguredFeature::feature, FeatureCodecs::getFlatCodec);

    private FeatureCodecs() {}

    private static <FC extends FeatureConfiguration> MapCodec<ConfiguredFeature<FC, Feature<FC>>> getFlatCodec(
            Feature<FC> f) {
        // Whereas Mojang consumes a Codec<FC> and reads at field of "config",
        // we assume and thus only support MapCodecs at the parent level.
        return CodecUtils.asMapCodec(FeatureExtras.getCodec(f))
            .xmap(fc -> new ConfiguredFeature<>(f, fc), ConfiguredFeature::config);
    }
}
