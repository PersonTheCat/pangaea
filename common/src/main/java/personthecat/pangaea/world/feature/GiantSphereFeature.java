package personthecat.pangaea.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.feature.GiantSphereFeature.Configuration;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public final class GiantSphereFeature extends GiantFeature<Configuration> {
    public static final GiantSphereFeature INSTANCE = new GiantSphereFeature();

    private GiantSphereFeature() {
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
        final double r2 = r * r;
        final double cX = (pos.x << 4) + 8;
        final double cZ = (pos.z << 4) + 8;

        for (int x = aX; x < aX + 16; x++) {
            final double dX2 = ((x - cX) * (x - cX)) / r2;

            for (int z = aZ; z < aZ + 16; z++) {
                if (!cfg.strictOrigin && !border.isInRange(ctx, x, z)) {
                    continue;
                }

                final double dZ2 = ((z - cZ) * (z - cZ)) / r2;
                for (int y = (int) (cY - r); y <= cY + r; y++) {

                    final double dY2 = ((y - cY) * (y - cY)) / r2;
                    if ((dX2 + dZ2 + dY2) <= 1.0) {
                        cfg.placer.placeUnchecked(ctx, x, y, z);
                    }
                }
            }
        }
    }

    public static class Configuration extends GiantFeatureConfiguration {
        private static final HeightProvider DEFAULT_HEIGHT =
            UniformHeight.of(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32));
        public static final MapCodec<Configuration> CODEC = PangaeaCodec.buildMap(cat -> codecOf(
            cat.field(BlockPlacer.CODEC, "placer", c -> c.placer),
            cat.defaulted(IntProvider.CODEC, "radius", UniformInt.of(18, 24), c -> c.radius),
            cat.defaulted(HeightProvider.CODEC, "height", DEFAULT_HEIGHT, c -> c.height),
            cat.defaulted(ChunkFilter.CODEC, "chunk_filter", new ChanceChunkFilter(0.15), c -> c.chunkFilter),
            union(GiantFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        ));
        public final BlockPlacer placer;
        public final IntProvider radius;
        public final HeightProvider height;
        public final ChunkFilter chunkFilter;

        protected Configuration(
                BlockPlacer placer,
                IntProvider radius,
                HeightProvider height,
                ChunkFilter chunkFilter,
                GiantFeatureConfiguration source) {
            super(source);
            this.placer = placer;
            this.radius = radius;
            this.height = height;
            this.chunkFilter = chunkFilter;
        }
    }
}
