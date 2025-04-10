package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.level.GenerationContext;

public record ConstantBoundsProvider(ColumnBounds bounds) implements ColumnProvider {
    public static final MapCodec<ConstantBoundsProvider> CODEC =
        ColumnBounds.OBJECT_CODEC.xmap(ConstantBoundsProvider::new, ConstantBoundsProvider::bounds);

    @Override
    public ColumnBounds getColumn(GenerationContext ctx, int x, int z) {
        return this.bounds();
    }

    @Override
    public MapCodec<? extends ColumnProvider> codec() {
        return CODEC;
    }
}
