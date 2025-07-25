package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public interface ColumnProvider {
    Codec<ColumnProvider> CODEC =
        PangaeaCodec.forRegistry(PgRegistries.COLUMN_TYPE, ColumnProvider::codec);

    ColumnBounds getColumn(PangaeaContext ctx, int x, int z);

    default boolean isInBounds(PangaeaContext ctx, int x, int y, int z) {
        return this.getColumn(ctx, x, z).isInRange(y);
    }

    MapCodec<? extends ColumnProvider> codec();
}
