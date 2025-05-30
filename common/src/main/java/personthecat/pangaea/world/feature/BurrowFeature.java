package personthecat.pangaea.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.WarpType;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.density.AutoWrapDensity;
import personthecat.pangaea.world.density.FastNoiseDensity;
import personthecat.pangaea.world.feature.BurrowFeature.Configuration;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.DynamicColumnProvider;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;
import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

public class BurrowFeature extends GiantFeature<Configuration> {
    public static final BurrowFeature INSTANCE = new BurrowFeature();

    private BurrowFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected void place(PangaeaContext ctx, Configuration cfg, ChunkPos pos, Border border) {
        final int aX = ctx.actualX;
        final int aZ = ctx.actualZ;

        for (int x = aX; x < aX + 16; x++) {
            for (int z = aZ; z < aZ + 16; z++) {
                if (border.isInRange(ctx, x, z)) {
                    this.generateColumn(ctx, cfg, border, x, z);
                }
            }
        }
    }

    private void generateColumn(PangaeaContext ctx, Configuration cfg, Border border, int x, int z) {
        final var pos = new MutableFunctionContext(x, 0, z);
        final var rand = ctx.rand;
        ctx.targetPos.set(pos);

        final double stretch = cfg.stretch.sample(rand);
        final double exponent = cfg.exponent.sample(rand);
        final double target = cfg.target.sample(rand);
        final double value = cfg.map.compute(pos) + cfg.shift.sample(rand);

        // compute and adjust radius by distance from horizontal bounds
        double radius = cfg.radius.sample(rand);
        radius = Math.max(0, border.transformNoise(ctx, x, z, radius));

        // avoid calculating offset when there's nothing to spawn
        int cap = (int) cap(stretch, radius, value, exponent, target);
        if (cap <= 0) {
            return;
        }
        final int centerY = (int) cfg.offset.compute(pos);
        final var column = cfg.column.getColumn(ctx, x, z);

        // slowly shrink the radius as we approach vertical bounds
        radius = Math.max(0, column.transformNoise(radius, centerY));
        cap = (int) cap(stretch, radius, value, exponent, target);
        if (cap <= 0) {
            return;
        }
        for (int y = centerY - cap; y <= centerY + cap; y++) {
            cfg.placer.placeUnchecked(ctx, x, y, z);
        }
    }

    private static double cap(double stretch, double radius, double value, double exponent, double target) {
        return stretch * (radius - (Math.pow(value, exponent) * (radius / Math.pow(target, exponent))));
    }

    public static class Configuration extends GiantFeatureConfiguration {
        private static final ColumnProvider DEFAULT_COLUMN = new DynamicColumnProvider(
            VerticalAnchor.absolute(5), VerticalAnchor.absolute(35), DEFAULT_HARSHNESS);
        private static final FastNoise DEFAULT_MAP_NOISE =
            FastNoise.builder().frequency(0.005F).warp(WarpType.BASIC_GRID).warpAmplitude(0.1F).warpFrequency(2.5F).build();
        private static final FastNoise DEFAULT_OFFSET_NOISE =
            FastNoise.builder().frequency(0.01F).range(10, 30).build();
        private static final DensityFunction DEFAULT_MAP =
            FastNoiseDensity.create(DEFAULT_MAP_NOISE, FastNoiseDensity.Mode.SCALED_2D);
        private static final DensityFunction DEFAULT_OFFSET =
            FastNoiseDensity.create(DEFAULT_OFFSET_NOISE, FastNoiseDensity.Mode.SCALED_2D);
        public static final MapCodec<Configuration> CODEC = PangaeaCodec.buildMap(cat -> codecOf(
            cat.defaulted(FloatProvider.codec(1, 24), "radius", ConstantFloat.of(4.5F), c -> c.radius),
            cat.defaulted(FloatProvider.codec(0, 64), "stretch", ConstantFloat.of(1), c -> c.stretch),
            cat.defaulted(FloatProvider.codec(-1, 1), "target", ConstantFloat.of(0.1F), c -> c.target),
            cat.defaulted(FloatProvider.CODEC, "exponent", ConstantFloat.of(4), c -> c.exponent),
            cat.defaulted(FloatProvider.codec(-1, 1), "shift", ConstantFloat.of(0.1F), c -> c.shift),
            cat.field(BlockPlacer.CODEC, "placer", c -> c.placer),
            cat.defaulted(ColumnProvider.CODEC, "column", DEFAULT_COLUMN, c -> c.column),
            cat.defaulted(AutoWrapDensity.HELPER_CODEC, "map", DEFAULT_MAP, c -> c.map),
            cat.defaulted(AutoWrapDensity.HELPER_CODEC, "offset", DEFAULT_OFFSET, c -> c.offset),
            union(PangaeaFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        ));
        public final FloatProvider radius;
        public final FloatProvider stretch;
        public final FloatProvider target;
        public final FloatProvider exponent;
        public final FloatProvider shift;
        public final BlockPlacer placer;
        public final ColumnProvider column;
        public final DensityFunction map;
        public final DensityFunction offset;

        public Configuration(
                FloatProvider radius,
                FloatProvider stretch,
                FloatProvider target,
                FloatProvider exponent,
                FloatProvider shift,
                BlockPlacer placer,
                ColumnProvider column,
                DensityFunction map,
                DensityFunction offset,
                PangaeaFeatureConfiguration parent) {
            super(parent, 0, false);
            this.radius = radius;
            this.stretch = stretch;
            this.target = target;
            this.exponent = exponent;
            this.shift = shift;
            this.placer = placer;
            this.column = column;
            this.map = map;
            this.offset = offset;
        }
    }
}
