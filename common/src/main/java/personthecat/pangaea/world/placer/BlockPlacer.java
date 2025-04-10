package personthecat.pangaea.world.placer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.level.GenerationContext;

import java.util.function.Function;

public interface BlockPlacer {
    Codec<BlockPlacer> CODEC =
        PgRegistries.PLACER_TYPE.codec().dispatch(BlockPlacer::codec, Function.identity());

    boolean placeUnchecked(GenerationContext ctx, int x, int y, int z);
    MapCodec<? extends BlockPlacer> codec();
}
