package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class PangaeaFeatureConfiguration implements FeatureConfiguration {
    public static final MapCodec<PangaeaFeatureConfiguration> CODEC = codecOf(
        union(ConditionConfiguration.CODEC, c -> c.conditions),
        defaulted(Codec.BOOL, "strict_border", true, c -> c.strictBorder),
        PangaeaFeatureConfiguration::new
    );

    public final ConditionConfiguration conditions;
    public final boolean strictBorder;

    public PangaeaFeatureConfiguration(ConditionConfiguration conditions, boolean strictBorder) {
        this.conditions = conditions;
        this.strictBorder = strictBorder;
    }

    protected PangaeaFeatureConfiguration(PangaeaFeatureConfiguration source) {
        this(source.conditions, source.strictBorder);
    }
}
