package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class MapFeatureConfiguration extends PangaeaFeatureConfiguration {
    public static final MapCodec<MapFeatureConfiguration> CODEC = codecOf(
        union(PangaeaFeatureConfiguration.CODEC, c -> c),
        defaulted(Codec.intRange(1, 32), "chunk_radius", 8, c -> c.chunkRadius),
        defaulted(Codec.BOOL, "strict_origin", true, c -> c.strictOrigin),
        MapFeatureConfiguration::new
    );

    public final int chunkRadius;
    public final boolean strictOrigin;

    public MapFeatureConfiguration(
        PangaeaFeatureConfiguration parent, int chunkRadius, boolean strictOrigin) {
        super(parent);
        this.chunkRadius = chunkRadius;
        this.strictOrigin = strictOrigin;
    }

    protected MapFeatureConfiguration(MapFeatureConfiguration source) {
        this(source, source.chunkRadius, source.strictOrigin);
    }
}
