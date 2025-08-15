package personthecat.pangaea.world.placer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public interface BlockPlacer {
    Codec<BlockPlacer> CODEC =
        PangaeaCodec.forRegistry(PgRegistries.PLACER_TYPE, BlockPlacer::codec);

    boolean place(PangaeaContext ctx, int x, int y, int z, int updates);
    MapCodec<? extends BlockPlacer> codec();

    default boolean place(PangaeaContext ctx, int x, int y, int z) {
        return this.place(ctx, x, y, z, 0);
    }
}
