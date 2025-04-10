package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.data.AnchorCutoff;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.level.GenerationContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
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

}
