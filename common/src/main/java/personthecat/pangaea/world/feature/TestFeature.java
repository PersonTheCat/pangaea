package personthecat.pangaea.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import personthecat.pangaea.world.feature.TestFeature.Configuration;
import personthecat.pangaea.world.level.GenerationContext;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class TestFeature extends PangaeaFeature<Configuration> {
    public static final TestFeature INSTANCE = new TestFeature();

    private TestFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected boolean place(GenerationContext ctx, Configuration cfg, BlockPos pos, Border border) {
        final var column = cfg.column.getColumn(ctx, ctx.centerX, ctx.centerZ);
        for (int y = column.min(); y < column.max(); y++) {
            cfg.placer.placeUnchecked(ctx, ctx.centerX, y, ctx.centerZ);
        }
        return true;
    }

    public static class Configuration extends PangaeaFeatureConfiguration {
        public final BlockPlacer placer;
        public final ColumnProvider column;

        public static final MapCodec<Configuration> CODEC = codecOf(
            field(BlockPlacer.CODEC, "placer", c -> c.placer),
            field(ColumnProvider.CODEC, "column", c -> c.column),
            union(PangaeaFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        );

        protected Configuration(
                BlockPlacer placer, ColumnProvider column, PangaeaFeatureConfiguration source) {
            super(source);
            this.placer = placer;
            this.column = column;
        }
    }
}
