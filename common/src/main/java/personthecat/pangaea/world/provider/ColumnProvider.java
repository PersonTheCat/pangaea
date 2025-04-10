package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.level.GenerationContext;

import java.util.function.Function;

public interface ColumnProvider {
    Codec<ColumnProvider> CODEC =
        PgRegistries.BOUNDS_TYPE.codec().dispatch(ColumnProvider::codec, Function.identity());

    ColumnBounds getColumn(GenerationContext ctx, int x, int z);

    default boolean isInBounds(GenerationContext ctx, int x, int y, int z) {
        return this.getColumn(ctx, x, z).isInRange(y);
    }

    MapCodec<? extends ColumnProvider> codec();
}
