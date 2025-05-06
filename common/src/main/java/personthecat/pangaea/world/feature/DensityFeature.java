package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.world.density.AutoWrapDensity;
import personthecat.pangaea.world.density.FastNoiseDensity;
import personthecat.pangaea.world.feature.DensityFeature.Configuration;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.DynamicColumnProvider;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;
import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

public final class DensityFeature extends PangaeaFeature<Configuration> {
    public static final DensityFeature INSTANCE = new DensityFeature();

    private DensityFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected boolean place(PangaeaContext ctx, Configuration cfg, BlockPos pos, Border border) {
        final int aX = ctx.actualX;
        final int aZ = ctx.actualZ;

        for (int x = aX; x < aX + 16; x++) {
            for (int z = aZ; z < aZ + 16; z++) {
                if (border.isInRange(ctx, x, z)) {
                    this.generateColumn(ctx, cfg, border, x, z);
                }
            }
        }
        return true;
    }

    private void generateColumn(PangaeaContext ctx, Configuration cfg, Border border, int x, int z) {
        final var column = cfg.column.getColumn(ctx, x, z);
        final var pos = new MutableFunctionContext(x, 0, z);
        final var placer = cfg.placer;
        for (int y = column.min(); y <= column.max(); y++) {
            for (final var f : cfg.generators) {
                double n = f.compute(pos.at(y));
                n = border.transformNoise(ctx, x, z, n);
                n = column.transformNoise(n, y);
                if (n > 0) {
                    placer.placeUnchecked(ctx, x, y, z);
                    break;
                }
            }
        }
    }

    public static class Configuration extends GiantFeatureConfiguration {
        public final BlockPlacer placer;
        public final ColumnProvider column;
        public final List<DensityFunction> generators;

        private static final ColumnProvider DEFAULT_COLUMN = new DynamicColumnProvider(
            VerticalAnchor.aboveBottom(24), VerticalAnchor.absolute(54), DEFAULT_HARSHNESS);
        private static final Codec<DensityFunction> NOISE_CODEC =
            defaultType(AutoWrapDensity.HELPER_CODEC, FastNoiseDensity.CODEC.codec());
        public static final MapCodec<Configuration> CODEC = codecOf(
            field(BlockPlacer.CODEC, "placer", c -> c.placer),
            defaulted(ColumnProvider.CODEC, "column", DEFAULT_COLUMN, c -> c.column),
            field(easyList(NOISE_CODEC), "generators", c -> c.generators),
            union(GiantFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        );

        protected Configuration(
                BlockPlacer placer,
                ColumnProvider column,
                List<DensityFunction> generators,
                GiantFeatureConfiguration source) {
            super(source, false);
            this.placer = placer;
            this.column = column;
            this.generators = generators;
        }
    }
}
