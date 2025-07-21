package personthecat.pangaea.world.weight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.level.PangaeaContext;

public interface WeightFunction {
    double NEVER = 1e20;
    Codec<WeightFunction> CODEC =
        PangaeaCodec.forRegistry(PgRegistries.WEIGHT_TYPE, WeightFunction::codec);

    double compute(PangaeaContext pg, FunctionContext fn);
    MapCodec<? extends WeightFunction> codec();
}
