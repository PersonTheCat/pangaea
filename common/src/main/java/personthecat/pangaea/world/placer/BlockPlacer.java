package personthecat.pangaea.world.placer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.level.GenerationContext;

import java.util.function.Function;

public interface BlockPlacer {
    Codec<BlockPlacer> CODEC =
        PgRegistries.PLACER_TYPE.codec().dispatch(BlockPlacer::codec, Function.identity());

    boolean place(WorldGenLevel level, WorldgenRandom rand, BlockPos pos);
    boolean placeUnchecked(GenerationContext ctx, int x, int y, int z);
    MapCodec<? extends BlockPlacer> codec();
}
