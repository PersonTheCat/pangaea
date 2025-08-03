package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

public enum ApproximateWeight implements WeightFunction {
    CONTINENTALNESS {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getApproximateContinentalness(fn.blockX(), fn.blockZ());
        }
    },
    DEPTH {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getApproximateDepth(fn.blockX(), fn.blockZ());
        }
    },
    PV {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getApproximatePv(fn.blockX(), fn.blockZ());
        }
    },
    WEIRDNESS {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getApproximateWeirdness(fn.blockX(), fn.blockZ());
        }
    };

    private final MapCodec<ApproximateWeight> codec = MapCodec.unit(this);

    @Override
    public MapCodec<ApproximateWeight> codec() {
        return this.codec;
    }
}
