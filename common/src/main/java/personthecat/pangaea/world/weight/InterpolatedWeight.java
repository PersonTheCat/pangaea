package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

public enum InterpolatedWeight implements WeightFunction {
    CONTINENTALNESS {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getContinentalness(fn.blockX(), fn.blockZ());
        }
    },
    DEPTH {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getDepth(fn.blockX(), fn.blockZ());
        }
    },
    PV {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getPv(fn.blockX(), fn.blockZ());
        }
    },
    WEIRDNESS {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.noise.getWeirdness(fn.blockX(), fn.blockZ());
        }
    };

    private final MapCodec<InterpolatedWeight> codec = MapCodec.unit(this);

    @Override
    public MapCodec<InterpolatedWeight> codec() {
        return this.codec;
    }
}
