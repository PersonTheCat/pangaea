package personthecat.pangaea.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class PangaeaFeatureConfiguration implements FeatureConfiguration {
    public static final MapCodec<PangaeaFeatureConfiguration> CODEC = codecOf(
        union(ConditionConfiguration.CODEC, c -> c.conditions),
        PangaeaFeatureConfiguration::new
    );

    public final ConditionConfiguration conditions;

    public PangaeaFeatureConfiguration(ConditionConfiguration conditions) {
        this.conditions = conditions;
    }

    protected PangaeaFeatureConfiguration(PangaeaFeatureConfiguration source) {
        this(source.conditions);
    }
}
