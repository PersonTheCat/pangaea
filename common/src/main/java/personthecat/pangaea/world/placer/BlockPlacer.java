package personthecat.pangaea.world.placer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.BlockPlacerBuilder;
import personthecat.pangaea.serialization.codec.PatternBlockPlacerCodec;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public interface BlockPlacer {
    Codec<BlockPlacer> CODEC = buildCodec();

    boolean placeUnchecked(PangaeaContext ctx, int x, int y, int z);
    MapCodec<? extends BlockPlacer> codec();

    private static Codec<BlockPlacer> buildCodec() {
        final var dispatcher =
            PgRegistries.PLACER_TYPE.codec().dispatch(BlockPlacer::codec, Function.identity());
        return Codec.lazyInitialized(() ->
            PatternBlockPlacerCodec.wrap(BlockPlacerBuilder.wrap(dispatcher)));
    }
}
