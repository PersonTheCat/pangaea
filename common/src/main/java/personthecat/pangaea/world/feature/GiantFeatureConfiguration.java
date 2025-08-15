package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class GiantFeatureConfiguration extends PangaeaFeatureConfiguration {
    public static final MapCodec<GiantFeatureConfiguration> CODEC = codecOf(
        union(PangaeaFeatureConfiguration.CODEC, c -> c),
        defaulted(Codec.intRange(1, 32), "chunk_radius", 8, c -> c.chunkRadius),
        defaulted(Codec.BOOL, "strict_origin", true, c -> c.strictOrigin),
        defaulted(GiantFeatureStage.CODEC, "stage", GiantFeatureStage.BEFORE_SURFACE, c -> c.stage),
        GiantFeatureConfiguration::new
    );

    public final int chunkRadius;
    public final boolean strictOrigin;
    public final GiantFeatureStage stage;

    public GiantFeatureConfiguration(
            PangaeaFeatureConfiguration parent, int chunkRadius, boolean strictOrigin, GiantFeatureStage stage) {
        super(parent);
        this.chunkRadius = chunkRadius;
        this.strictOrigin = strictOrigin;
        this.stage = stage;
    }

    protected GiantFeatureConfiguration(GiantFeatureConfiguration source) {
        this(source, source.chunkRadius, source.strictOrigin);
    }

    protected GiantFeatureConfiguration(
            GiantFeatureConfiguration source, boolean strictOrigin) {
        this(source, source.chunkRadius, strictOrigin, source.stage);
    }

    protected GiantFeatureConfiguration(
            GiantFeatureConfiguration source, int chunkRadius, boolean strictOrigin) {
        this(source, chunkRadius, strictOrigin, source.stage);
    }
}
