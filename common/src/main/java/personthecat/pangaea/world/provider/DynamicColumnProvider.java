package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record DynamicColumnProvider(
        VerticalAnchor min, VerticalAnchor max, double harshness) implements ColumnProvider {
    public static final MapCodec<DynamicColumnProvider> CODEC = codecOf(
        field(VerticalAnchor.CODEC, "min", DynamicColumnProvider::min),
        field(VerticalAnchor.CODEC, "max", DynamicColumnProvider::max),
        defaulted(Codec.DOUBLE, "harshness", DensityCutoff.DEFAULT_HARSHNESS, DynamicColumnProvider::harshness),
        DynamicColumnProvider::new
    );

    @Override
    public ColumnBounds getColumn(PangaeaContext ctx, int x, int z) {
        return ColumnBounds.create(this.min.resolveY(ctx), this.max.resolveY(ctx), this.harshness);
    }

    @Override
    public MapCodec<DynamicColumnProvider> codec() {
        return CODEC;
    }
}
