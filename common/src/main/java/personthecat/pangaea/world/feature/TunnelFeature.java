package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import personthecat.pangaea.world.feature.TunnelFeature.Configuration;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class TunnelFeature extends GiantFeature<Configuration> {
    public static final TunnelFeature INSTANCE = new TunnelFeature();
    private static final float PI = (float) Math.PI;
    private static final float HALF_PI = PI / 2.0F;
    private static final float TAU = PI * 2.0F;

    private TunnelFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected void place(PangaeaContext ctx, Configuration cfg, ChunkPos pos, Border border) {
        final var rand = ctx.rand;
        if (rand.nextFloat() > cfg.chance) {
            return;
        }
        final int rangeBlocks = (this.getRangeChunks() * 2 - 1) << 4;
        final int count = rand.nextInt(rand.nextInt(rand.nextInt(this.getMaxCaves()) + 1) + 1);

        for (int i = 0; i < count; i++) {
            final double x = pos.getBlockX(rand.nextInt(16));
            final double y = cfg.height.sample(rand, ctx);
            final double z = pos.getBlockZ(rand.nextInt(16));
            final double radiusFactorXz = cfg.radiusFactorXz.sample(rand);
            final double radiusFactorY = cfg.radiusFactorY.sample(rand);
            final double floorCurve = cfg.floorCurve.sample(rand);
            int branches = 1;
            float pitch;
            if (rand.nextInt(4) == 0) {
                double verticalScale = cfg.verticalScale.sample(rand);
                pitch = 1.0F + rand.nextFloat() * 6.0F;
                this.createRoom(ctx, cfg, x, y, z, pitch, verticalScale, floorCurve);
                branches += rand.nextInt(4);
            }
            for (int j = 0; j < branches; j++) {
                float yaw = rand.nextFloat() * TAU;
                pitch = (rand.nextFloat() - 0.5F) / 4.0F;
                float thickness = this.getThickness(rand);
                int end = rangeBlocks - rand.nextInt(rangeBlocks / 4);
                this.createTunnel(ctx, cfg, rand.nextLong(), x, y, z, radiusFactorXz, radiusFactorY, thickness, yaw, pitch, 0, end, this.getVerticalScale(), floorCurve);
            }
        }
    }

    protected void createRoom(PangaeaContext ctx, Configuration cfg, double x, double y, double z, float pitch, double verticalScale, double floorCurve) {
        double rXz = 1.5 + (double) (Mth.sin(HALF_PI) * pitch);
        double rY = rXz * verticalScale;
        this.carveEllipsoid(ctx, cfg, x + 1.0, y, z, rXz, rY, floorCurve);
    }

    protected void createTunnel(PangaeaContext ctx, Configuration cfg, long localSeed, double x, double y, double z, double rFXz, double rFY, float thickness, float yaw, float pitch, int idx, int end, double verticalScale, double floorCurve) {
        final var localRand = RandomSource.create(localSeed);
        final int branchIndex = localRand.nextInt(end / 2) + end / 4;
        final boolean noiseCorrection = localRand.nextInt(6) == 0;
        float dYaw = 0.0F;
        float dPitch = 0.0F;

        for (int i = idx; i < end; i++) {
            final double rXz = 1.5 + Mth.sin(PI * (float) i / (float) end) * thickness;
            final double rY = rXz * verticalScale;
            final float cosPitch = Mth.cos(pitch);
            x += Mth.cos(yaw) * cosPitch;
            y += Mth.sin(pitch);
            z += Mth.sin(yaw) * cosPitch;
            pitch *= noiseCorrection ? 0.92F : 0.7F;
            pitch += dPitch * 0.1F;
            yaw += dYaw * 0.1F;
            dPitch *= 0.9F;
            dYaw *= 0.75F;
            dPitch += (localRand.nextFloat() - localRand.nextFloat()) * localRand.nextFloat() * 2.0F;
            dYaw += (localRand.nextFloat() - localRand.nextFloat()) * localRand.nextFloat() * 4.0F;

            if (i == branchIndex && thickness > 1.0F) {
                this.createTunnel(ctx, cfg, localRand.nextLong(), x, y, z, rFXz, rFY, localRand.nextFloat() * 0.5F + 0.5F, yaw - HALF_PI, pitch / 3.0F, i, end, 1.0, floorCurve);
                this.createTunnel(ctx, cfg, localRand.nextLong(), x, y, z, rFXz, rFY, localRand.nextFloat() * 0.5F + 0.5F, yaw + HALF_PI, pitch / 3.0F, i, end, 1.0, floorCurve);
                return;
            }
            if (localRand.nextInt(4) != 0) {
                if (!canReach(ctx, x, z, i, end, thickness)) {
                    return;
                }
                this.carveEllipsoid(ctx, cfg, x, y, z, rXz * rFXz, rY * rFY, floorCurve);
            }
        }
    }

    protected void carveEllipsoid(PangaeaContext ctx, Configuration cfg, double x, double y, double z, double rXz, double rY, double floorCurve) {
        final double centerX = ctx.centerX;
        final double centerZ = ctx.centerZ;
        final double range = rXz * 2.0 + 16.0; // likely chunk buffer
        if (!(Math.abs(x - centerX) > range) && !(Math.abs(z - centerZ) > range)) {
            final int chunkMinX = ctx.actualX;
            final int chunkMinZ = ctx.actualZ;
            final int chunkMaxY = ctx.minY + ctx.getGenDepth() - 1 - (ctx.chunk.isUpgrading() ? 0 : 7);
            final int minX = Math.max(0, Mth.floor(x - rXz) - chunkMinX - 1);
            final int maxX = Math.min(15, Mth.floor(x + rXz) - chunkMinX);
            final int minY = Math.max(ctx.minY + 1, Mth.floor(y - rY) - 1);
            final int maxY = Math.min(chunkMaxY, Mth.floor(y + rY) + 1);
            final int minZ = Math.max(0, Mth.floor(z - rXz) - chunkMinZ - 1);
            final int maxZ = Math.min(15, Mth.floor(z + rXz) - chunkMinZ);

            for (int relX = minX; relX <= maxX; relX++) {
                final int aX = chunkMinX + relX;
                final double dX = ((double) aX + 0.5 - x) / rXz;

                for (int relZ = minZ; relZ <= maxZ; relZ++) {
                    final int aZ = chunkMinZ + relZ;
                    final double dZ = ((double) aZ + 0.5 - z) / rXz;

                    if (dX * dX + dZ * dZ < 1.0) {
                        for (int aY = maxY; aY > minY; aY--) {
                            final double dY = ((double) aY - 0.5 - y) / rY;

                            if (dY > floorCurve && dX * dX + dY * dY + dZ * dZ < 1.0) {
                                cfg.placer.placeUnchecked(ctx, aX, aY, aZ);
                            }
                        }
                    }
                }
            }
        }
    }

    protected int getRangeChunks() {
        return 4;
    }

    protected int getMaxCaves() {
        return 15;
    }

    protected double getVerticalScale() {
        return 1.0;
    }

    protected float getThickness(RandomSource rand) {
        float f = rand.nextFloat() * 2.0F + rand.nextFloat();
        if (rand.nextInt(10) == 0) {
            f *= rand.nextFloat() * rand.nextFloat() * 3.0F + 1.0F;
        }
        return f;
    }

    protected static boolean canReach(PangaeaContext ctx, double x, double z, int idx, int end, float thickness) {
        final double dX = x - ctx.centerX;
        final double dZ = z - ctx.centerZ;
        final double progress = end - idx;
        final double radius = thickness + 2.0F + 16.0F; // likely chunk buffer
        return dX * dX + dZ * dZ - progress * progress <= radius * radius;
    }

    public static class Configuration extends GiantFeatureConfiguration {
        public static final MapCodec<Configuration> CODEC = codecOf(
            field(Codec.FLOAT, "chance", c -> c.chance),
            field(HeightProvider.CODEC, "height", c -> c.height),
            field(FloatProvider.CODEC, "radius_factor_xz", c -> c.radiusFactorXz),
            field(FloatProvider.CODEC, "radius_factor_y", c -> c.radiusFactorY),
            field(FloatProvider.CODEC, "floor_curve", c -> c.floorCurve),
            field(FloatProvider.CODEC, "vertical_scale", c -> c.verticalScale),
            field(BlockPlacer.CODEC, "placer", c -> c.placer),
            union(GiantFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        );
        public final float chance;
        public final HeightProvider height;
        public final FloatProvider radiusFactorXz;
        public final FloatProvider radiusFactorY;
        public final FloatProvider floorCurve;
        public final FloatProvider verticalScale;
        public final BlockPlacer placer;

        public Configuration(
                float chance,
                HeightProvider height,
                FloatProvider radiusFactorXz,
                FloatProvider radiusFactorY,
                FloatProvider floorCurve,
                FloatProvider verticalScale,
                BlockPlacer placer,
                GiantFeatureConfiguration parent) {
            super(parent, false);
            this.chance = chance;
            this.height = height;
            this.radiusFactorXz = radiusFactorXz;
            this.radiusFactorY = radiusFactorY;
            this.floorCurve = floorCurve;
            this.verticalScale = verticalScale;
            this.placer = placer;
        }
    }
}
