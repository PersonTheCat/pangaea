package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

public enum RouterWeight implements WeightFunction {
    TEMPERATURE {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.temperature().compute(fn);
        }
    },
    VEGETATION {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.vegetation().compute(fn);
        }
    },
    CONTINENTS {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.continents().compute(fn);
        }
    },
    EROSION {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.erosion().compute(fn);
        }
    },
    DEPTH {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.depth().compute(fn);
        }
    },
    RIDGES {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.ridges().compute(fn);
        }
    },
    INITIAL_DENSITY {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.initialDensityWithoutJaggedness().compute(fn);
        }
    },
    FINAL_DENSITY {
        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return pg.router.finalDensity().compute(fn);
        }
    };

    private final MapCodec<RouterWeight> codec = MapCodec.unit(this);

    @Override
    public MapCodec<RouterWeight> codec() {
        return this.codec;
    }
}
