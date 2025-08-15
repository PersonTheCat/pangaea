package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;

import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;

public enum GiantFeatureStage {
    BEFORE_SURFACE,
    AFTER_SURFACE;

    public static final Codec<GiantFeatureStage> CODEC = ofEnum(GiantFeatureStage.class);
}
