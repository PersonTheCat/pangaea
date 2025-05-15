package personthecat.pangaea.world.chain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public interface ChainPathConfig<P extends ChainPath> {
    Codec<ChainPathConfig<?>> CODEC =
        PgRegistries.PATH_TYPE.codec().dispatch(ChainPathConfig::codec, Function.identity());

    default P instance(PangaeaContext ctx) {
        return this.instance(ctx, ctx.rand);
    }

    P instance(PangaeaContext ctx, RandomSource rand);
    MapCodec<? extends ChainPathConfig<P>> codec();
}
