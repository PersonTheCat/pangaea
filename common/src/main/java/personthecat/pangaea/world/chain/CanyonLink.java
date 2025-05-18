package personthecat.pangaea.world.chain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import personthecat.catlib.serialization.codec.CapturingCodec.Receiver;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;

import static personthecat.catlib.serialization.codec.CapturingCodec.receive;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaultTry;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.pangaea.serialization.codec.PgCodecs.floatRangeFix;

public class CanyonLink extends ChainLink {
    private final float[] mut;
    private final double chance;
    private final double radius;
    private final double verticalScale;

    protected CanyonLink(PangaeaContext ctx, RandomSource rand, Config config) {
        super(config);
        this.chance = config.chance.sample(rand);
        this.radius = config.radius.sample(rand);
        this.verticalScale = config.verticalScale.sample(rand);
        this.mut = this.initMut(ctx, rand, config);
    }

    private float[] initMut(PangaeaContext ctx, RandomSource rand, Config config) {
        final int len = ctx.getGenDepth();
        final float[] mut = new float[len];

        float val = 1.0F;
        for (int i = 0; i < len; i++) {
            if (i == 0 || rand.nextInt(config.widthSmoothness) == 0) {
                val = 1.0F + rand.nextFloat() * rand.nextFloat();
            }
            mut[i] = val * val;
        }
        return mut;
    }

    @Override
    public void place(PangaeaContext ctx, RandomSource rand, ChainPath path, int idx, int end) {
        if (rand.nextFloat() > this.chance) {
            return;
        }
        final double rXz = this.radius * path.radiusFactor();
        final double rY = this.getVerticalRadius(rand, rXz, idx, end);
        final double range = rXz * 2.0 + 16.0; // chunk buffer;
        final float x = path.blockX();
        final float y = path.blockY();
        final float z = path.blockZ();
        if (Math.abs(x - ctx.centerX) > range && Math.abs(z - ctx.centerZ) > range) {
            return;
        }
        final int chunkMinX = ctx.actualX;
        final int chunkMinZ = ctx.actualZ;
        final int chunkMaxY = ctx.minY + ctx.getGenDepth() - 1 - (ctx.chunk.isUpgrading() ? 0 : 7);
        final int minX = Math.max(0, Mth.floor(x - rXz) - chunkMinX - 1);
        final int maxX = Math.min(15, Mth.floor(x + rXz) - chunkMinX);
        final int minY = Math.max(ctx.minY + 1, Mth.floor(y - rY) - 1);
        final int maxY = Math.min(chunkMaxY, Mth.floor(y + rY) + 1);
        final int minZ = Math.max(0, Mth.floor(z - rXz) - chunkMinZ - 1);
        final int maxZ = Math.min(15, Mth.floor(z + rXz) - chunkMinZ);
        final var placer = ((Config) this.config).placer;

        for (int relX = minX; relX <= maxX; relX++) {
            final int aX = chunkMinX + relX;
            final double dX = ((double) aX + 0.5 - x) / rXz;

            for (int relZ = minZ; relZ <= maxZ; relZ++) {
                final int aZ = chunkMinZ + relZ;
                final double dZ = ((double) aZ + 0.5 - z) / rXz;

                if (dX * dX + dZ * dZ >= 1.0) {
                    continue;
                }
                for (int aY = maxY; aY > minY; aY--) {
                    final double dY = ((double) aY - 0.5 - y) / rY;
                    final int yO = aY - minY;

                    if ((dX * dX + dZ * dZ) * mut[yO - 1] + dY * dY / 6.0 < 1.0) {
                        placer.placeUnchecked(ctx, aX, aY, aZ);
                    }
                }
            }
        }
    }

    private double getVerticalRadius(RandomSource rand, double rXz, float idx, float end) {
        final float center = 1.0F - Mth.abs(0.5F - idx / end) * 2.0F;
        final var cfg = (Config) this.config;
        final float factor = cfg.verticalRadiusDefaultFactor + cfg.verticalRadiusCenterFactor * center;
        double rY = rXz * this.verticalScale;
        return factor * rY * (double) Mth.randomBetween(rand, 0.75F, 1.0F);
    }

    @Override
    public double radius() {
        return this.radius;
    }

    public record Config(
        FloatProvider chance,
        FloatProvider radius,
        FloatProvider verticalScale,
        int widthSmoothness,
        float verticalRadiusDefaultFactor,
        float verticalRadiusCenterFactor,
        BlockPlacer placer
    ) implements ChainLinkConfig<CanyonLink> {
        private static final FloatProvider DEFAULT_CHANCE = ConstantFloat.of(0.75F);
        private static final FloatProvider DEFAULT_RADIUS = UniformFloat.of(2, 3);
        private static final FloatProvider DEFAULT_SCALE = ConstantFloat.of(3.0F);
        private static final int DEFAULT_SMOOTHNESS = 3;
        private static final float DEFAULT_DEFAULT_FACTOR = 1.0F;
        private static final float DEFAULT_CENTER_FACTOR = 0.0F;
        private static final Receiver<BlockPlacer> DEFAULT_PLACER = receive("placer");

        public static final MapCodec<Config> CODEC = codecOf(
            defaulted(floatRangeFix(0, 1), "chance", DEFAULT_CHANCE, Config::chance),
            defaulted(floatRangeFix(0, 32), "radius", DEFAULT_RADIUS, Config::radius),
            defaulted(floatRangeFix(0, 32), "vertical_scale", DEFAULT_SCALE, Config::verticalScale),
            defaulted(Codec.INT, "width_smoothness", DEFAULT_SMOOTHNESS, Config::widthSmoothness),
            defaulted(Codec.FLOAT, "vertical_radius_default_factor", DEFAULT_DEFAULT_FACTOR, Config::verticalRadiusDefaultFactor),
            defaulted(Codec.FLOAT, "vertical_radius_center_factor", DEFAULT_CENTER_FACTOR, Config::verticalRadiusCenterFactor),
            defaultTry(BlockPlacer.CODEC, "placer", DEFAULT_PLACER, Config::placer),
            Config::new
        );

        @Override
        public CanyonLink instance(PangaeaContext ctx, RandomSource rand) {
            return new CanyonLink(ctx, rand, this);
        }

        @Override
        public MapCodec<Config> codec() {
            return CODEC;
        }
    }
}
