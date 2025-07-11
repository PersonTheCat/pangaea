package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

public class NeverWeight implements WeightFunction {
    public static final NeverWeight INSTANCE = new NeverWeight();
    public static final MapCodec<NeverWeight> CODEC = MapCodec.unit(INSTANCE);

    private NeverWeight() {}

    @Override
    public double compute(PangaeaContext pg, FunctionContext fn) {
        return NEVER;
    }

    @Override
    public MapCodec<NeverWeight> codec() {
        return CODEC;
    }
}
