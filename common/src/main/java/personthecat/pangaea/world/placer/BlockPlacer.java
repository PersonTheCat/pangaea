package personthecat.pangaea.world.placer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public interface BlockPlacer {
    Codec<BlockPlacer> CODEC =
        PangaeaCodec.forRegistry(PgRegistries.PLACER_TYPE, BlockPlacer::codec);

    boolean placeUnchecked(PangaeaContext ctx, int x, int y, int z);
    MapCodec<? extends BlockPlacer> codec();
}
