package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PatternHeightCodecs;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public interface ColumnProvider {
    Codec<ColumnProvider> CODEC = buildCodec();

    ColumnBounds getColumn(PangaeaContext ctx, int x, int z);

    default boolean isInBounds(PangaeaContext ctx, int x, int y, int z) {
        return this.getColumn(ctx, x, z).isInRange(y);
    }

    MapCodec<? extends ColumnProvider> codec();

    private static Codec<ColumnProvider> buildCodec() {
        final var dispatcher =
            PgRegistries.COLUMN_TYPE.codec().dispatch(ColumnProvider::codec, Function.identity());
        return PatternHeightCodecs.wrapColumn(dispatcher);
    }
}
