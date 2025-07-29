package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import personthecat.catlib.serialization.codec.CodecUtils;

public enum AnchorDistribution {
    UNIFORM,
    TRAPEZOID;

    public static final AnchorDistribution DEFAULT = TRAPEZOID;
    public static final Codec<AnchorDistribution> CODE = CodecUtils.ofEnum(AnchorDistribution.class);

    public HeightProvider apply(VerticalAnchor min, VerticalAnchor max) {
        return switch (this) {
            case UNIFORM -> UniformHeight.of(min, max);
            case TRAPEZOID -> TrapezoidHeight.of(min, max);
        };
    }
}
