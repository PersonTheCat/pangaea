package personthecat.pangaea.world.chain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public interface ChainLinkConfig<L extends ChainLink> {
    Codec<ChainLinkConfig<?>> CODEC =
        PgRegistries.LINK_TYPE.codec().dispatch(ChainLinkConfig::codec, Function.identity());

    default L instance(PangaeaContext ctx) {
        return this.instance(ctx, ctx.rand);
    }

    L instance(PangaeaContext ctx, RandomSource rand);
    MapCodec<? extends ChainLinkConfig<L>> codec();
}
