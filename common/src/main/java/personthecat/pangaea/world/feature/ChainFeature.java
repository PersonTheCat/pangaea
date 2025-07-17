package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.serialization.codec.capture.CaptureCategory;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.chain.ChainLink;
import personthecat.pangaea.world.chain.ChainLinkConfig;
import personthecat.pangaea.world.chain.ChainPath;
import personthecat.pangaea.world.chain.ChainPathConfig;
import personthecat.pangaea.world.feature.ChainFeature.Configuration;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.provider.VeryBiasedToBottomInt;

import static personthecat.catlib.serialization.codec.capture.CapturingCodec.capture;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;
import static personthecat.pangaea.serialization.codec.PgCodecs.floatRangeFix;

public class ChainFeature extends GiantFeature<Configuration> {
    public static final ChainFeature INSTANCE = new ChainFeature();

    private ChainFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected void place(PangaeaContext ctx, Configuration cfg, ChunkPos pos, Border border) {
        final var rand = ctx.rand;
        if (!cfg.chunkFilter.test(ctx, pos.x, pos.z)) {
            return;
        }
        final int count = cfg.count.sample(rand);
        if (count <= 0) {
            return;
        }
        final var path = cfg.path.instance(ctx);
        final var link = cfg.link.instance(ctx);

        for (int i = 0; i < count; i++) {
            final float x = pos.getBlockX(rand.nextInt(16));
            final float y = cfg.height.sample(rand, ctx);
            final float z = pos.getBlockZ(rand.nextInt(16));

            int branches = 1;
            for (int j = 0; j < branches; j++) {
                final var localRand = RandomSource.create(rand.nextLong());
                final var range = cfg.range.sample(localRand);
                path.start(localRand);
                path.reset(ctx, localRand, x, y, z);

                if (j == 0 && rand.nextFloat() <= cfg.systemChance.sample(rand)) {
                    this.generateHub(ctx, cfg, localRand, path);
                    branches += cfg.systemDensity.sample(rand);
                }
                this.traversePath(ctx, cfg, localRand, path, link, border, 0, range);
            }
        }
    }

    protected void generateHub(PangaeaContext ctx, Configuration cfg, RandomSource rand, ChainPath path) {
        final var hub = cfg.hub.instance(ctx, rand);
        // default hub is link with larger scale
        if (cfg.hub == cfg.link) {
            path.scale(path.radiusFactor() + rand.nextFloat() * 2.0F);
        }
        hub.place(ctx, rand, path, 0, 0);
    }

    protected void traversePath(
            PangaeaContext ctx, Configuration cfg, RandomSource rand, ChainPath path, ChainLink link, Border border, int idx, int end) {
        final int branchIndex = cfg.enableBranches ? rand.nextInt(end / 2) + end / 4 : -1;

        for (int i = idx; i < end; i++) {
            path.next(ctx, rand, i, end);

            if (i == branchIndex && path.radiusFactor() > 1.25F) {
                var localRand = RandomSource.create(rand.nextLong());
                var fork = path.fork(ctx, localRand);
                fork.redirect(path.yaw() - Mth.HALF_PI, path.pitch() / 3.0F);
                this.traversePath(ctx, cfg, localRand, fork, link, border, i, end);

                localRand = RandomSource.create(rand.nextLong());
                fork = path.fork(ctx, localRand);
                fork.redirect(path.yaw() + Mth.HALF_PI, path.pitch() / 3.0F);
                this.traversePath(ctx, cfg, localRand, fork, link, border, i, end);
                return;
            }
            if (!canReach(ctx, path, link, idx, end)) {
                return;
            }
            if (border.isInRange(ctx, (int) path.blockX(), (int) path.blockZ())) {
                link.place(ctx, rand, path, idx, end);
            }
        }
    }

    protected static boolean canReach(PangaeaContext ctx, ChainPath path, ChainLink link, int idx, int end) {
        final double dX = path.blockX() - ctx.centerX;
        final double dZ = path.blockZ() - ctx.centerZ;
        final double dRemaining = end - idx;
        final double radius = link.radius() * path.radiusFactor() + 16.0F; // chunk buffer
        return dX * dX + dZ * dZ - dRemaining * dRemaining <= radius * radius;
    }

    public static class Configuration extends GiantFeatureConfiguration {
        private static final ChunkFilter DEFAULT_CHUNK_FILTER = ChanceChunkFilter.of(0.15F);
        private static final FloatProvider DEFAULT_SYSTEM_CHANCE = ConstantFloat.of(0.25F);
        private static final IntProvider DEFAULT_COUNT = VeryBiasedToBottomInt.of(0, 7);
        private static final IntProvider DEFAULT_RANGE = UniformInt.of(84, 112);
        private static final IntProvider DEFAULT_SYSTEM_DENSITY = UniformInt.of(0, 2);
        private static final HeightProvider DEFAULT_HEIGHT =
            UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(180));

        public static final MapCodec<Configuration> CODEC =
            PangaeaCodec.build(Configuration::createCodec)
                .addCaptures(capture("placer", BlockPlacer.CODEC))
                .mapCodec();

        private final ChunkFilter chunkFilter;
        private final FloatProvider systemChance;
        private final IntProvider count;
        private final IntProvider range;
        private final IntProvider systemDensity;
        private final HeightProvider height;
        private final ChainPathConfig<?> path;
        private final ChainLinkConfig<?> link;
        private final ChainLinkConfig<?> hub;
        private final boolean enableBranches;

        public Configuration(
                ChunkFilter chunkFilter,
                FloatProvider systemChance,
                IntProvider count,
                IntProvider range,
                IntProvider systemDensity,
                HeightProvider height,
                ChainPathConfig<?> path,
                ChainLinkConfig<?> link,
                @Nullable ChainLinkConfig<?> hub,
                boolean enableBranches,
                GiantFeatureConfiguration parent) {
            super(parent);
            this.chunkFilter = chunkFilter;
            this.systemChance = systemChance;
            this.count = count;
            this.range = range;
            this.systemDensity = systemDensity;
            this.height = height;
            this.path = path;
            this.link = link;
            this.hub = hub != null ? hub : link;
            this.enableBranches = enableBranches;
        }

        private static MapCodec<Configuration> createCodec(CaptureCategory<Configuration> cat) {
            return codecOf(
                cat.defaulted(cat.configure(ChunkFilter.CODEC), "chunk_filter", DEFAULT_CHUNK_FILTER, c -> c.chunkFilter),
                cat.defaulted(floatRangeFix(0, 1), "system_chance", DEFAULT_SYSTEM_CHANCE, c -> c.systemChance),
                cat.defaulted(IntProvider.codec(0, 128), "count", DEFAULT_COUNT, c -> c.count),
                cat.defaulted(IntProvider.codec(0, 1024), "range", DEFAULT_RANGE, c -> c.range),
                cat.defaulted(IntProvider.codec(0, 128), "system_density", DEFAULT_SYSTEM_DENSITY, c -> c.systemDensity),
                cat.defaulted(HeightProvider.CODEC, "height", DEFAULT_HEIGHT, c -> c.height),
                cat.field(cat.configure(ChainPathConfig.CODEC), "path", c -> c.path),
                cat.field(cat.configure(ChainLinkConfig.CODEC), "link", c -> c.link),
                cat.nullable(cat.configure(ChainLinkConfig.CODEC), "hub", c -> c.hub),
                cat.defaulted(Codec.BOOL, "enable_branches", true, c -> c.enableBranches),
                union(GiantFeatureConfiguration.CODEC, c -> c),
                Configuration::new
            );
        }
    }
}
