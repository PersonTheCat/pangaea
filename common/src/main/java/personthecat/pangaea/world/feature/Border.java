package personthecat.pangaea.world.feature;

import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.map.GenerationContext;

public interface Border {
    Border NONE = new Border() {

        @Override
        public boolean isInRange(GenerationContext ctx, int x, int z) {
            return true;
        }

        @Override
        public boolean isClose(GenerationContext ctx, int x, int z) {
            return true;
        }

        @Override
        public double transformNoise(GenerationContext ctx, int x, int z, double n) {
            return n;
        }
    };

    boolean isInRange(GenerationContext ctx, int x, int z);
    boolean isClose(GenerationContext ctx, int x, int z);
    double transformNoise(GenerationContext ctx, int x, int z, double n);

    default Border and(Border b) {
        final var a = this;
        return new Border() {

            @Override
            public boolean isInRange(GenerationContext ctx, int x, int z) {
                return a.isInRange(ctx, x, z) && b.isInRange(ctx, x, z);
            }

            @Override
            public boolean isClose(GenerationContext ctx, int x, int z) {
                return a.isClose(ctx, x, z) || b.isClose(ctx, x, z);
            }

            @Override
            public double transformNoise(GenerationContext ctx, int x, int z, double n) {
                return Math.min(a.transformNoise(ctx, x, z, n), b.transformNoise(ctx, x, z, n));
            }
        };
    }

    static Border circleAround(int x, int z, double r, double t) {
        final var cutoff = new DensityCutoff(t, r, 0.15);
        return new Border() {

            @Override
            public boolean isInRange(GenerationContext ctx, int x2, int z2) {
                return Utils.distance(x, z, x2, z2) <= r;
            }

            @Override
            public boolean isClose(GenerationContext ctx, int x2, int z2) {
                return Utils.distance(x, z, x2, z2) <= t;
            }

            @Override
            public double transformNoise(GenerationContext ctx, int x2, int z2, double n) {
                return cutoff.transformUpper(n, Utils.distance(x, z, x2, z2));
            }
        };
    }

    static Border forPredicate(GenerationContext ctx, PositionalBiomePredicate predicate, double r, double t) {
        if (predicate.isDisabled()) {
            return NONE;
        }
        final var graph = ctx.noise.graphBiomes(ctx.biomes, predicate, ctx.chunkX, ctx.chunkZ);
        final var cutoff = new DensityCutoff(t, r, 0.15);

        return new Border() {

            @Override
            public boolean isInRange(GenerationContext ctx, int x, int z) {
                return graph.distance(x, z, r) <= r;
            }

            @Override
            public boolean isClose(GenerationContext ctx, int x, int z) {
                return graph.distance(x, z, t) <= t;
            }

            @Override
            public double transformNoise(GenerationContext ctx, int x, int z, double n) {
                return cutoff.transformUpper(n, graph.distance(x, z, r));
            }
        };
    }

}
