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

public record AnchoredBoundsProvider(
        VerticalAnchor min, VerticalAnchor max, double harshness) implements ColumnProvider {
    public static final MapCodec<AnchoredBoundsProvider> CODEC = codecOf(
        field(VerticalAnchor.CODEC, "min", AnchoredBoundsProvider::min),
        field(VerticalAnchor.CODEC, "max", AnchoredBoundsProvider::max),
        defaulted(Codec.DOUBLE, "harshness", DensityCutoff.DEFAULT_HARSHNESS, AnchoredBoundsProvider::harshness),
        AnchoredBoundsProvider::new
    );

    @Override
    public ColumnBounds getColumn(GenerationContext ctx, int x, int z) {
        return ColumnBounds.create(this.min.resolveY(ctx), this.max.resolveY(ctx), this.harshness);
    }

    @Override
    public MapCodec<AnchoredBoundsProvider> codec() {
        return CODEC;
    }
}
