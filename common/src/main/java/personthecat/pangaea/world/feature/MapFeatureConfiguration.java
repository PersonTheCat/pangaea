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
        MapFeatureConfiguration::new
    );

    public final int chunkRadius;

    public MapFeatureConfiguration(PangaeaFeatureConfiguration parent, int chunkRadius) {
        super(parent);
        this.chunkRadius = chunkRadius;
    }

    protected MapFeatureConfiguration(MapFeatureConfiguration source) {
        this(source, source.chunkRadius);
    }
}
