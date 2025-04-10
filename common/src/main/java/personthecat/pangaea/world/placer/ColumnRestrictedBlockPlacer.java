package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.GenerationContext;
import personthecat.pangaea.world.provider.ColumnProvider;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record ColumnRestrictedBlockPlacer(ColumnProvider column, BlockPlacer place) implements BlockPlacer {
    public static final MapCodec<ColumnRestrictedBlockPlacer> CODEC = codecOf(
        field(ColumnProvider.CODEC, "column", ColumnRestrictedBlockPlacer::column),
        field(BlockPlacer.CODEC, "place", ColumnRestrictedBlockPlacer::place),
        ColumnRestrictedBlockPlacer::new
    );

    @Override
    public boolean placeUnchecked(GenerationContext ctx, int x, int y, int z) {
        if (this.column.isInBounds(ctx, x, y, z)) {
            return this.place.placeUnchecked(ctx, x, y, z);
        }
        return false;
    }

    @Override
    public MapCodec<ColumnRestrictedBlockPlacer> codec() {
        return CODEC;
    }
}

