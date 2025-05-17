package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import personthecat.pangaea.world.feature.RavineFeature.Configuration;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.placer.BlockPlacer;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class RavineFeature extends GiantFeature<Configuration> {
    public static final RavineFeature INSTANCE = new RavineFeature();

    private RavineFeature() {
        super(Configuration.CODEC.codec());
    }

    @Override
    protected void place(PangaeaContext ctx, Configuration cfg, ChunkPos pos, Border border) {
        final var rand = ctx.rand;
        final int rangeBlocks = (this.getRangeChunks() * 2 - 1) << 4;
        final double x = pos.getBlockX(rand.nextInt(16));
        final double y = cfg.height.sample(rand, ctx);
        final double z = pos.getBlockZ(rand.nextInt(16));
        final float yaw = rand.nextFloat() * Mth.TWO_PI;
        final float pitch = cfg.pitch.sample(rand);
        final float verticalScale = cfg.verticalScale.sample(rand);
        final float thickness = cfg.thickness.sample(rand);
        final int distance = (int) ((float) rangeBlocks * cfg.distanceFactor.sample(rand));
        this.generateRavine(ctx, cfg, rand.nextLong(), x, y, z, thickness, yaw, pitch, distance, verticalScale);
    }

    private void generateRavine(PangaeaContext ctx, Configuration cfg, long seed, double x, double y, double z, float thickness, float yaw, float pitch, int end, double verticalScale) {
        final var rand = RandomSource.create(seed);
        final float[] mut = this.initMut(ctx, cfg, rand);
        float dYaw = 0.0F;
        float dPitch = 0.0F;

        for (int i = 0; i < end; i++) {
            double rXz = 1.5 + (Mth.sin((float) i * Mth.PI / (float) end) * thickness);
            double rY = rXz * verticalScale;
            rXz *= cfg.horizontalRadiusFactor.sample(rand);
            rY = this.updateVerticalRadius(cfg, rand, rY, end, i);
            float cosPitch = Mth.cos(pitch);
            x += Mth.cos(yaw) * cosPitch;
            y += Mth.sin(pitch);
            z += Mth.sin(yaw) * cosPitch;
            pitch *= 0.7F;
            pitch += dPitch * 0.05F;
            yaw += dYaw * 0.05F;
            dPitch *= 0.8F;
            dYaw *= 0.5F;
            dPitch += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
            dYaw += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;
            if (rand.nextInt(4) != 0) {
                if (!canReach(ctx, x, z, i, end, thickness)) {
                    return;
                }
                this.carveEllipsoid(ctx, cfg, x, y, z, rXz, rY, mut);
            }
        }
    }

    private float[] initMut(PangaeaContext ctx, Configuration cfg, RandomSource rand) {
        final int len = ctx.getGenDepth();
        final float[] mut = new float[len];

        float val = 1.0F;
        for (int i = 0; i < len; i++) {
            if (i == 0 || rand.nextInt(cfg.widthSmoothness) == 0) {
                val = 1.0F + rand.nextFloat() * rand.nextFloat();
            }
            mut[i] = val * val;
        }
        return mut;
    }

    private double updateVerticalRadius(Configuration cfg, RandomSource rand, double rY, float end, float idx) {
        float center = 1.0F - Mth.abs(0.5F - idx / end) * 2.0F;
        float factor = cfg.verticalRadiusDefaultFactor + cfg.verticalRadiusCenterFactor * center;
        return factor * rY * (double) Mth.randomBetween(rand, 0.75F, 1.0F);
    }

    private int getRangeChunks() {
        return 4;
    }

    private void carveEllipsoid(PangaeaContext ctx, Configuration cfg, double x, double y, double z, double rXz, double rY, float[] mut) {
        final double centerX = ctx.centerX;
        final double centerZ = ctx.centerZ;
        final double range = rXz * 2.0 + 16.0; // likely chunk buffer
        if (!(Math.abs(x - centerX) > range) && !(Math.abs(z - centerZ) > range)) {
            final int chunkMinX = ctx.actualX;
            final int chunkMinZ = ctx.actualZ;
            final int chunkMinY = ctx.minY;
            final int chunkMaxY = chunkMinY + ctx.getGenDepth() - 1 - (ctx.chunk.isUpgrading() ? 0 : 7);
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
                            final int yO = aY - minY;

                            if ((dX * dX + dZ * dZ) * mut[yO - 1] + dY * dY / 6.0 < 1.0) {
                                cfg.placer.placeUnchecked(ctx, aX, aY, aZ);
                            }
                        }
                    }
                }
            }
        }
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
            field(HeightProvider.CODEC, "height", c -> c.height),
            field(FloatProvider.CODEC, "pitch", c -> c.pitch),
            field(FloatProvider.CODEC, "vertical_scale", c -> c.verticalScale),
            field(FloatProvider.CODEC, "thickness", c -> c.thickness),
            field(FloatProvider.CODEC, "distance_factor", c -> c.distanceFactor),
            field(FloatProvider.CODEC, "horizontal_radius_factor", c -> c.horizontalRadiusFactor),
            field(Codec.FLOAT, "vertical_radius_default_factor", c -> c.verticalRadiusDefaultFactor),
            field(Codec.FLOAT, "vertical_radius_center_factor", c -> c.verticalRadiusCenterFactor),
            field(Codec.INT, "width_smoothness", c -> c.widthSmoothness),
            field(BlockPlacer.CODEC, "placer", c -> c.placer),
            union(GiantFeatureConfiguration.CODEC, c -> c),
            Configuration::new
        );
        public final HeightProvider height;
        public final FloatProvider pitch;
        public final FloatProvider verticalScale;
        public final FloatProvider thickness;
        public final FloatProvider distanceFactor;
        public final FloatProvider horizontalRadiusFactor;
        public final float verticalRadiusDefaultFactor;
        public final float verticalRadiusCenterFactor;
        public final int widthSmoothness;
        public final BlockPlacer placer;

        public Configuration(
                HeightProvider height,
                FloatProvider pitch,
                FloatProvider verticalScale,
                FloatProvider thickness,
                FloatProvider distanceFactor,
                FloatProvider horizontalRadiusFactor,
                float verticalRadiusDefaultFactor,
                float verticalRadiusCenterFactor,
                int widthSmoothness,
                BlockPlacer placer,
                GiantFeatureConfiguration parent) {
            super(parent, false);
            this.height = height;
            this.pitch = pitch;
            this.verticalScale = verticalScale;
            this.thickness = thickness;
            this.distanceFactor = distanceFactor;
            this.horizontalRadiusFactor = horizontalRadiusFactor;
            this.verticalRadiusDefaultFactor = verticalRadiusDefaultFactor;
            this.verticalRadiusCenterFactor = verticalRadiusCenterFactor;
            this.widthSmoothness = widthSmoothness;
            this.placer = placer;
        }
    }
}
