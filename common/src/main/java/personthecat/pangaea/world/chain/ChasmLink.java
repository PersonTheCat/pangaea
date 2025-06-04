package personthecat.pangaea.world.chain;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import personthecat.catlib.serialization.codec.capture.CaptureCategory;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.NoiseType;
import personthecat.pangaea.serialization.codec.NoiseCodecs;
import personthecat.pangaea.serialization.codec.PangaeaCodec;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.pangaea.serialization.codec.PgCodecs.floatRangeFix;

public class ChasmLink extends ChainLink {
    private final double chance;
    private final double radius;
    private final double verticalScale;
    private final float tilt;
    private final FastNoise noise;

    protected ChasmLink(RandomSource rand, Config config) {
        super(config);
        this.chance = config.chance.sample(rand);
        this.radius = config.radius.sample(rand);
        this.verticalScale = config.verticalScale.sample(rand);
        this.tilt = config.tilt.sample(rand);
        this.noise = config.noise.toBuilder().seed(rand.nextInt()).build();
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination")
    public void place(PangaeaContext ctx, RandomSource rand, ChainPath path, int idx, int end) {
        if (rand.nextFloat() > this.chance) {
            return;
        }
        final double rXz = this.radius * path.radiusFactor();
        final double rY = rXz * this.verticalScale;
        final double r = Math.max(rXz, rY);
        final double range = r * 2.0 + 16.0; // chunk buffer;
        final float x = path.blockX();
        final float y = path.blockY();
        final float z = path.blockZ();
        if (Math.abs(x - ctx.centerX) > range && Math.abs(z - ctx.centerZ) > range) {
            return;
        }
        final int chunkMinX = ctx.actualX;
        final int chunkMinZ = ctx.actualZ;
        final int chunkMaxY = ctx.minY + ctx.getGenDepth() - 1 - (ctx.chunk.isUpgrading() ? 0 : 7);
        final int minX = Math.max(0, Mth.floor(x - r) - chunkMinX - 1);
        final int maxX = Math.min(15, Mth.floor(x + r) - chunkMinX);
        final int minY = Math.max(ctx.minY + 1, Mth.floor(y - r) - 1);
        final int maxY = Math.min(chunkMaxY, Mth.floor(y + r) + 1);
        final int minZ = Math.max(0, Mth.floor(z - r) - chunkMinZ - 1);
        final int maxZ = Math.min(15, Mth.floor(z + r) - chunkMinZ);
        final var placer = ((Config) this.config).placer;

        final var forward = path.direction().normalize();
        final var up = Mth.abs(forward.y) > 0.99 ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        final var side = forward.cross(up).normalize(); // perpendicular side direction

        final var from = new Vector3f(0, 1, 0); // local Y-axis
        final var horizontalRotation = new Quaternionf().rotationTo(from, side);
        final var verticalRotation = new Quaternionf().rotateX(this.tilt);
        final var rotation = verticalRotation.mul(horizontalRotation);

        for (int relX = minX; relX <= maxX; relX++) {
            final int aX = chunkMinX + relX;
            final float lX = aX + 0.5F - x;

            for (int relZ = minZ; relZ <= maxZ; relZ++) {
                final int aZ = chunkMinZ + relZ;
                final float lZ = aZ + 0.5F - z;

                for (int aY = maxY; aY > minY; aY--) {
                    final float lY = aY - 0.5F - y;

                    final Vector3f local = new Vector3f(lX, lY, lZ);
                    rotation.transform(local);

                    final double dX = local.x / rXz;
                    final double dY = local.y / rY;
                    final double dZ = local.z / rXz;

                    final double m = Math.pow(Mth.clampedMap(Mth.abs(local.y), rY / 4, rY, 1, 2), 2) + this.noise.getNoiseScaled(local.y);

                    if ((dX * dX + dZ * dZ) * m + dY * dY < 1.0) {
                        placer.placeUnchecked(ctx, aX, aY, aZ);
                    }
                }
            }
        }
    }

    @Override
    public double radius() {
        return Math.max(this.radius, this.radius * this.verticalScale);
    }

    public record Config(
        FloatProvider chance,
        FloatProvider radius,
        FloatProvider verticalScale,
        FloatProvider tilt,
        FastNoise noise,
        BlockPlacer placer
    ) implements ChainLinkConfig<ChasmLink> {
        private static final FloatProvider DEFAULT_CHANCE = ConstantFloat.of(0.75F);
        private static final FloatProvider DEFAULT_RADIUS = UniformFloat.of(1.5F, 2.5F);
        private static final FloatProvider DEFAULT_SCALE = ConstantFloat.of(3.0F);
        private static final FloatProvider DEFAULT_TILT = UniformFloat.of(0.4F, 1.18F); // 22.5 - 67.5 degrees
        private static final FastNoise DEFAULT_NOISE =
            FastNoise.builder().type(NoiseType.PERLIN).frequency(0.1F).range(0, 2).build();

        public static final MapCodec<Config> CODEC =
            PangaeaCodec.build(Config::createCodec)
                .capturing(/* NoiseCodecs.CATEGORY.createPreset(DEFAULT_NOISE) */)
                .mapCodec();

        @Override
        public ChasmLink instance(PangaeaContext ctx, RandomSource rand) {
            return new ChasmLink(rand, this);
        }

        @Override
        public MapCodec<Config> codec() {
            return CODEC;
        }

        private static MapCodec<Config> createCodec(CaptureCategory<Config> cat) {
            return codecOf(
                cat.defaulted(floatRangeFix(0, 1), "chance", DEFAULT_CHANCE, Config::chance),
                cat.defaulted(floatRangeFix(0, 32), "radius", DEFAULT_RADIUS, Config::radius),
                cat.defaulted(floatRangeFix(0, 32), "vertical_scale", DEFAULT_SCALE, Config::verticalScale),
                cat.defaulted(floatRangeFix(-Mth.TWO_PI, Mth.TWO_PI), "tilt", DEFAULT_TILT, Config::tilt),
                cat.defaulted(NoiseCodecs.NOISE_CODEC.codec(), "noise", DEFAULT_NOISE, Config::noise),
                cat.field(BlockPlacer.CODEC, "placer", Config::placer),
                Config::new
            );
        }
    }
}
