package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.data.AnchorCutoff;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record AnchorRangeColumnProvider(AnchorCutoff lower, AnchorCutoff upper) implements ColumnProvider {
    public static final MapCodec<AnchorRangeColumnProvider> CODEC = codecOf(
        field(AnchorCutoff.CODEC, "lower", AnchorRangeColumnProvider::lower),
        field(AnchorCutoff.CODEC, "upper", AnchorRangeColumnProvider::upper),
        AnchorRangeColumnProvider::new
    );

    @Override
    public ColumnBounds getColumn(PangaeaContext ctx, int x, int z) {
        // We should eventually look into caching this result if the column are effectively constant
        return new ColumnBounds(this.lower.getCutoff(ctx), this.upper.getCutoff(ctx));
    }

    @Override
    public MapCodec<AnchorRangeColumnProvider> codec() {
        return CODEC;
    }

}
