package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.world.density.AutoWrapDensity;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.density.FastNoiseDensity;
import personthecat.pangaea.world.feature.BlobFeature.Configuration;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.DynamicColumnProvider;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.*;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;
import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

public final class BlobFeature extends GiantFeature<Configuration> {
    public static final BlobFeature INSTANCE = new BlobFeature();

    private BlobFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected void place(PangaeaContext ctx, Configuration cfg, ChunkPos pos, Border border) {
        if (!cfg.chunkFilter.test(ctx, pos.x, pos.z)) {
            return;
        }
        final int aX = ctx.actualX;
        final int aZ = ctx.actualZ;
        final double cY = cfg.height.sample(ctx.rand, ctx);
        final double r = cfg.radius.sample(ctx.rand);
        final double cX = (pos.x << 4) + 8;
        final double cZ = (pos.z << 4) + 8;

        for (int x = aX; x < aX + 16; x++) {
            for (int z = aZ; z < aZ + 16; z++) {
                if (cfg.strictOrigin || border.isInRange(ctx, x, z)) {
                    this.generateColumn(ctx, cfg, border, cX, cY, cZ, r, x, z);
                }
            }
        }
    }

    private void generateColumn(
            PangaeaContext ctx, Configuration cfg, Border border, double cX, double cY, double cZ, double r, int x, int z) {
        final var column = cfg.column.getColumn(ctx, x, z);
        final int min = Math.max(column.min(), (int) (cY - r));
        final int max = Math.min(column.max(), (int) (cY + r));
        final var pos = new MutableFunctionContext(x, 0, z);
        final var cutoff = cfg.cutoff;

        final double r2 = r * r;
        final double dX2 = ((x - cX) * (x - cX)) / r2;
        final double dZ2 = ((z - cZ) * (z - cZ)) / r2;

        for (int y = min; y <= max; y++) {
            final double dY2 = ((y - cY) * (y - cY)) / r2;
            final double sum = dX2 + dZ2 + dY2;

            if (sum > 1.0) {
                continue;
            }
            for (final var f : cfg.generators) {
                double n = f.compute(pos.at(y));
                if (!cfg.strictOrigin) {
                    n = border.transformNoise(ctx, x, z, n);
                }
                n = cutoff.transformUpper(n, sum);
                n = column.transformNoise(n, y);

                if (n > 0) {
                    cfg.placer.placeUnchecked(ctx, x, y, z);
                    break;
                }
            }
        }
    }

    public static class Configuration extends GiantFeatureConfiguration {
        public final BlockPlacer placer;
        public final IntProvider radius;
        public final DensityCutoff cutoff;
        public final HeightProvider height;
        public final ColumnProvider column;
        public final ChunkFilter chunkFilter;
        public final List<DensityFunction> generators;

        private static final HeightProvider DEFAULT_HEIGHT =
            UniformHeight.of(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32));
        private static final ColumnProvider DEFAULT_COLUMN = new DynamicColumnProvider(
            VerticalAnchor.bottom(), VerticalAnchor.top(), DEFAULT_HARSHNESS);
        private static final Codec<DensityFunction> NOISE_CODEC =
            defaultType(AutoWrapDensity.HELPER_CODEC, FastNoiseDensity.CODEC.codec());
        public static final MapCodec<Configuration> CODEC = codecOf(
            field(BlockPlacer.CODEC, "placer", c -> c.placer),
            defaulted(IntProvider.CODEC, "radius", UniformInt.of(24, 48), c -> c.radius),
            defaulted(Codec.doubleRange(0, 1), "cutoff", 0.8, c -> c.cutoff.min()),
            defaulted(Codec.DOUBLE, "harshness", DEFAULT_HARSHNESS, c -> c.cutoff.harshness()),
            defaulted(HeightProvider.CODEC, "height", DEFAULT_HEIGHT, c -> c.height),
            defaulted(ColumnProvider.CODEC, "column", DEFAULT_COLUMN, c -> c.column),
            defaulted(ChunkFilter.CODEC, "chunk_filter", new ChanceChunkFilter(0.02), c -> c.chunkFilter),
            field(easyList(NOISE_CODEC), "generators", c -> c.generators),
            union(GiantFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        );

        protected Configuration(
                BlockPlacer placer,
                IntProvider radius,
                double cutoff,
                double harshness,
                HeightProvider height,
                ColumnProvider column,
                ChunkFilter chunkFilter,
                List<DensityFunction> generators,
                GiantFeatureConfiguration source) {
            super(source);
            this.placer = placer;
            this.radius = radius;
            this.cutoff = new DensityCutoff(cutoff, 1.0, harshness);
            this.height = height;
            this.column = column;
            this.chunkFilter = chunkFilter;
            this.generators = generators;
        }
    }
}
