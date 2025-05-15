package personthecat.pangaea.world.chain;

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
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaultTry;
import static personthecat.pangaea.serialization.codec.PgCodecs.floatRangeFix;

public class SphereLink extends ChainLink {
    private final double chance;
    private final double radius;
    private final double floorLevel;
    private final double verticalScale;

    protected SphereLink(RandomSource rand, Config config) {
        super(config);
        this.chance = config.chance.sample(rand);
        this.radius = config.radius.sample(rand);
        this.floorLevel = config.floorLevel.sample(rand);
        this.verticalScale = config.verticalScale.sample(rand);
    }

    @Override
    public void place(PangaeaContext ctx, RandomSource rand, ChainPath path, int idx, int end) {
        if (rand.nextFloat() > this.chance) {
            return;
        }
        final double rXz = this.radius * path.radiusFactor();
        final double rY = rXz * this.verticalScale;
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
        final double floorLevel = this.floorLevel;

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

                    if (dY > floorLevel && dX * dX + dY * dY + dZ * dZ < 1.0) {
                        placer.placeUnchecked(ctx, aX, aY, aZ);
                    }
                }
            }
        }
    }

    @Override
    public double radius() {
        return this.radius;
    }

    public record Config(
        FloatProvider chance,
        FloatProvider radius,
        FloatProvider floorLevel,
        FloatProvider verticalScale,
        BlockPlacer placer
    ) implements ChainLinkConfig<SphereLink> {
        private static final FloatProvider DEFAULT_CHANCE = ConstantFloat.of(0.75F);
        private static final FloatProvider DEFAULT_RADIUS = UniformFloat.of(2, 3);
        private static final FloatProvider DEFAULT_FLOOR = UniformFloat.of(-1, -0.4F);
        private static final FloatProvider DEFAULT_SCALE = ConstantFloat.of(1);
        private static final Receiver<BlockPlacer> DEFAULT_PLACER = receive("placer");

        public static final MapCodec<Config> CODEC = codecOf(
            defaulted(floatRangeFix(0, 1), "chance", DEFAULT_CHANCE, Config::chance),
            defaulted(floatRangeFix(0, 32), "radius", DEFAULT_RADIUS, Config::radius),
            defaulted(floatRangeFix(-1, 1), "floor_level", DEFAULT_FLOOR, Config::floorLevel),
            defaulted(floatRangeFix(0, 32), "vertical_scale", DEFAULT_SCALE, Config::verticalScale),
            defaultTry(BlockPlacer.CODEC, "placer", DEFAULT_PLACER, Config::placer),
            Config::new
        );

        @Override
        public SphereLink instance(PangaeaContext ctx, RandomSource rand) {
            return new SphereLink(rand, this);
        }

        @Override
        public MapCodec<Config> codec() {
            return CODEC;
        }
    }
}
