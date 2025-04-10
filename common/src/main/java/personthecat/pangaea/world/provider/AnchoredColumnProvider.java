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

public record AnchoredColumnProvider(
        VerticalAnchor min, VerticalAnchor max, double harshness) implements ColumnProvider {
    public static final MapCodec<AnchoredColumnProvider> CODEC = codecOf(
        field(VerticalAnchor.CODEC, "min", AnchoredColumnProvider::min),
        field(VerticalAnchor.CODEC, "max", AnchoredColumnProvider::max),
        defaulted(Codec.DOUBLE, "harshness", DensityCutoff.DEFAULT_HARSHNESS, AnchoredColumnProvider::harshness),
        AnchoredColumnProvider::new
    );

    @Override
    public ColumnBounds getColumn(GenerationContext ctx, int x, int z) {
        return ColumnBounds.create(this.min.resolveY(ctx), this.max.resolveY(ctx), this.harshness);
    }

    @Override
    public MapCodec<AnchoredColumnProvider> codec() {
        return CODEC;
    }
}
