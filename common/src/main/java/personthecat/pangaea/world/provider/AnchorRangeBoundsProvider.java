package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.level.GenerationContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record AnchorRangeBoundsProvider(AnchorCutoff lower, AnchorCutoff upper) implements ColumnProvider {
    public static final MapCodec<AnchorRangeBoundsProvider> CODEC = codecOf(
        field(AnchorCutoff.CODEC, "lower", AnchorRangeBoundsProvider::lower),
        field(AnchorCutoff.CODEC, "upper", AnchorRangeBoundsProvider::upper),
        AnchorRangeBoundsProvider::new
    );

    @Override
    public ColumnBounds getColumn(GenerationContext ctx, int x, int z) {
        // We should eventually look into caching this result if the bounds are effectively constant
        return new ColumnBounds(this.lower.getCutoff(ctx), this.upper.getCutoff(ctx));
    }

    @Override
    public MapCodec<AnchorRangeBoundsProvider> codec() {
        return CODEC;
    }

    public record AnchorCutoff(VerticalAnchor min, VerticalAnchor max, double harshness) {
        private static final Codec<AnchorCutoff> CODEC = codecOf(
            field(VerticalAnchor.CODEC, "min", AnchorCutoff::min),
            field(VerticalAnchor.CODEC, "max", AnchorCutoff::max),
            defaulted(Codec.DOUBLE, "harshness", DensityCutoff.DEFAULT_HARSHNESS, AnchorCutoff::harshness),
            AnchorCutoff::new
        ).codec();

        private DensityCutoff getCutoff(GenerationContext ctx) {
            return new DensityCutoff(this.min.resolveY(ctx), this.max.resolveY(ctx), this.harshness);
        }
    }
}
