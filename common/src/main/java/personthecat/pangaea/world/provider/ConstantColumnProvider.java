package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.level.GenerationContext;

public record ConstantColumnProvider(ColumnBounds column) implements ColumnProvider {
    public static final MapCodec<ConstantColumnProvider> CODEC =
        ColumnBounds.OBJECT_CODEC.xmap(ConstantColumnProvider::new, ConstantColumnProvider::column);

    @Override
    public ColumnBounds getColumn(GenerationContext ctx, int x, int z) {
        return this.column();
    }

    @Override
    public MapCodec<? extends ColumnProvider> codec() {
        return CODEC;
    }
}
