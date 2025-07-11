package personthecat.pangaea.world.weight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PatternWeightCodec;
import personthecat.pangaea.serialization.codec.StructuralWeightCodec;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public interface WeightFunction {
    double NEVER = 1e20;
    Codec<WeightFunction> CODEC = buildCodec();

    double compute(PangaeaContext pg, FunctionContext fn);
    MapCodec<? extends WeightFunction> codec();

    private static Codec<WeightFunction> buildCodec() {
        final var dispatcher =
            PgRegistries.WEIGHT_TYPE.codec().dispatch(WeightFunction::codec, Function.identity());
        return Codec.lazyInitialized(() ->
            PatternWeightCodec.wrap(StructuralWeightCodec.wrap(dispatcher)));
    }
}
