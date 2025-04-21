package personthecat.pangaea.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.feature.PositionalBiomePredicate;

public class NoiseGraph {
    public static final int BIOME_SAMPLE_DIMENSION = 3;
    public static final int BIOME_SAMPLE_RADIUS = 16 / BIOME_SAMPLE_DIMENSION;
    public static final int BIOME_SCAN_CHUNK_RADIUS = 2;
    public static final int BIOME_SCAN_DIMENSION = BIOME_SAMPLE_DIMENSION * (BIOME_SCAN_CHUNK_RADIUS * 2 + 1);
    private static final int CLEANUP_INTERVAL = 5;
    private static final int CLEANUP_DISTANCE = 10;
    private static final Point[] BIOME_CHECKS = {
        new Point(3, 3),
        new Point(3, 12),
        new Point(8, 8),
        new Point(12, 3),
        new Point(12, 12),
    };

    protected final Long2ObjectMap<Samples> graph = new Long2ObjectOpenHashMap<>();

    public float getSd(Sampler sampler, FunctionContext ctx) {
        return this.getSd(sampler, ctx.blockX(), ctx.blockZ());
    }

    public float getSd(Sampler sampler, int x, int z) {
        final int cX = x >> 4;
        final int cZ = z >> 4;
        final int rX = x & 15;
        final int rZ = z & 15;
        final int lX = lowerQuarter(rX);
        final int lZ = lowerQuarter(rZ);
        final Samples data = this.getData(cX, cZ);
        if (data.getSd(lX + 1, lZ + 1) == 0) { // nothing interpolated between corners
            this.compute(sampler, data, cX, cZ, lX, lZ);
        }
        return data.getSd(rX, rZ);
    }

    public float getApproximateDepth(Sampler sampler, int x, int z) {
        final int cX = x >> 4;
        final int cZ = z >> 4;
        final int rX = x & 15;
        final int rZ = z & 15;
        final int lX = lowerQuarter(rX);
        final int lZ = lowerQuarter(rZ);
        final Samples data = this.getData(cX, cZ);
        return this.getOrComputeDepth(sampler, data, cX, cZ, lX, lZ);
    }

    public float getDepth(Sampler sampler, int x, int z) {
        final int cX = x >> 4;
        final int cZ = z >> 4;
        final int rX = x & 15;
        final int rZ = z & 15;
        final int lX = lowerQuarter(rX);
        final int lZ = lowerQuarter(rZ);
        final Samples data = this.getData(cX, cZ);

        final float lXlZ = this.getOrComputeDepth(sampler, data, cX, cZ, lX, lZ);
        if (rX == lX && rZ == lZ) {
            return lXlZ;
        }
        final float uXuZ = this.getOrComputeDepth(sampler, data, cX, cZ, lX + 4, lZ + 4);
        final float lXuZ = this.getOrComputeDepth(sampler, data, cX, cZ, lX, lZ + 4);
        final float uXlZ = this.getOrComputeDepth(sampler, data, cX, cZ, lX + 4, lZ);

        final float tX = (rX - lX) / 3F;
        final float tZ = (rZ - lZ) / 3F;

        final float l = Utils.lerp(lXlZ, lXuZ, tX);
        final float r = Utils.lerp(uXlZ, uXuZ, tX);

        return Utils.lerp(l, r, tZ);
    }

    public Holder<Biome> getApproximateBiome(BiomeManager biomes, int x, int z) {
        final int cX = x >> 4;
        final int cZ = z >> 4;
        final int rX = x & 15;
        final int rZ = z & 15;
        // get one of: (1,1),(1,14),(8,8),(14,1),(14,14)
        final int sX = rX < 8 ? 1 : rX > 8 ? 14 : 8;
        final int sZ = sX == 8 ? 8 : rZ < 8 ? 1 : rZ > 8 ? 14 : 8;
        final Samples data = this.getData(cX, cZ);
        return this.getOrComputeBiome(biomes, data, sX, sZ);
    }

    public SimpleNeighborGraph graphBiomes(BiomeManager biomes, PositionalBiomePredicate predicate, int cX, int cZ) {
        final var graph = new SimpleNeighborGraph(BIOME_SCAN_DIMENSION, BIOME_SAMPLE_RADIUS);
        for (int x = cX - BIOME_SCAN_CHUNK_RADIUS; x <= cX + BIOME_SCAN_CHUNK_RADIUS; x++) {
            for (int z = cZ - BIOME_SCAN_CHUNK_RADIUS; z <= cZ + BIOME_SCAN_CHUNK_RADIUS; z++) {
                this.graphBiomesForChunk(graph, biomes, predicate, x, z);
            }
        }
        graph.endBatch();
        return graph.withCacheEnabled();
    }

    private void graphBiomesForChunk(
            SimpleNeighborGraph graph, BiomeManager biomes, PositionalBiomePredicate predicate, int cX, int cZ) {
        final Samples data = this.getData(cX, cZ);
        for (final var check : BIOME_CHECKS) {
            final var biome = this.getOrComputeBiome(biomes, data, check.x, check.z);
            final int aX = (cX << 4) + check.x;
            final int aZ = (cZ << 4) + check.z;
            if (!predicate.test(biome, aX, aZ)) {
                graph.plot(aX, aZ);
            }
        }
    }

    public boolean chunkMatches(BiomeManager biomes, PositionalBiomePredicate predicate, int cX, int cZ) {
        final Samples data = this.getData(cX, cZ);
        for (final var check : BIOME_CHECKS) {
            final var biome = this.getOrComputeBiome(biomes, data, check.x, check.z);
            final int aX = (cX << 4) + check.x;
            final int aZ = (cZ << 4) + check.z;
            if (predicate.test(biome, aX, aZ)) {
                return true;
            }
        }
        return false;
    }

    protected Samples getData(int cX, int cZ) {
        return this.graph.computeIfAbsent((((long) cX) << 32) | (cZ & 0xFFFFFFFFL), c -> {
            if ((cX % CLEANUP_INTERVAL) == 0 && (cZ % CLEANUP_INTERVAL) == 0) {
                this.drainOutside(cX, cZ, CLEANUP_DISTANCE);
            }
            return new Samples();
        });
    }

    protected void compute(Sampler sampler, Samples data, int cX, int cZ, int lX, int lZ) {
        final MutableFunctionContext ctx = new MutableFunctionContext();
        final int uX = lX + 4;
        final int uZ = lZ + 4;
        // get samples around 4 corners
        for (int x = lX - 4; x <= uX + 4; x += 4) {
            for (int z = lZ - 4; z <= uZ + 4; z += 4) {
                if ((x == lX - 4 || x == uX + 4) && (z == lZ - 4 || z == uZ + 4)) { // is corner of area
                    continue;
                }
                if (data.getD(x, z) == 0) {
                    data.setD(x, z, (float) sampler.depth().compute(ctx.at((cX << 4) + x, (cZ << 4) + z)));
                }
            }
        }
        // calculate SDs at each corner
        final float lXlZ = (float) this.computeSd(data, lX, lZ);
        final float uXuZ = (float) this.computeSd(data, uX, uZ);
        final float lXuZ = (float) this.computeSd(data, lX, uZ);
        final float uXlZ = (float) this.computeSd(data, uX, lZ);

        data.setSd(lX, lZ, lXlZ);
        if (uX < 16 && uZ < 16) data.setSd(uX, uZ, uXuZ);
        if (uZ < 16) data.setSd(lX, uZ, lXuZ);
        if (uX < 16) data.setSd(uX, lZ, uXlZ);

        // interpolate left column
        final float lXlZ2 = (lXlZ + lXuZ) / 2F;
        data.setSd(lX, lZ + 2, lXlZ2);

        // interpolate right column
        final float uXlZ2 = (uXlZ + uXuZ) / 2F;
        if (uX < 16) data.setSd(uX, lZ + 2, uXlZ2);

        // interpolate between columns
        data.setSd(lX + 2, lZ, (lXlZ + uXlZ) / 2F);
        data.setSd(lX + 2, lZ + 2, (lXlZ2 + uXlZ2) / 2F);
        if (uZ < 16) data.setSd(lX + 2, uZ, (lXuZ + uXuZ) / 2F);
    }

    protected double computeSd(Samples data, int rX, int rZ) {
        return Utils.stdDev(
            data.getD(rX, rZ),
            data.getD(rX, rZ + 4),
            data.getD(rX, rZ - 4),
            data.getD(rX + 4, rZ),
            data.getD(rX - 4, rZ)
        );
    }

    protected float getOrComputeDepth(Sampler sampler, Samples data, int cX, int cZ, int rX, int rZ) {
        float d = data.getD(rX, rZ);
        if (d == 0) {
            d = (float) sampler.depth().compute(new SinglePointContext((cX << 4) + rX, 0, (cZ << 4) + rZ));
            data.setD(rX, rZ, d);
        }
        return d;
    }

    protected Holder<Biome> getOrComputeBiome(BiomeManager biomes, Samples data, int rX, int rZ) {
        var biome = data.getBiome(rX, rZ);
        if (biome == null) {
            biome = biomes.getNoiseBiomeAtPosition(rX, 63, rZ);
            data.setBiome(rX, rZ, biome);
        }
        return biome;
    }

    public void drainOutside(int cX, int cZ, int r) {
        this.graph.long2ObjectEntrySet().removeIf(entry -> {
            final long l = entry.getLongKey();
            final int x = (int) (l >> 32);
            final int z = (int) l;
            return Utils.distance(cX, cZ, x, z) > r;
        });
    }

    public void clear() {
        this.graph.clear();
    }

    /**
     * <h2>
     *   Samples
     * </h2>
     * <p>
     *   A record of various interpolated noise samples. These
     *   samples are taken at an interval which matches their
     *   default frequencies, thus how likely they are to
     *   significantly change from block to block.
     * </p>
     * <h3>
     *   sd
     * </h3>
     * <p>
     *   SD of D noise in every other block.
     * </p>
     * <h4>
     *   Structure
     * </h4>
     * <p>
     *   This field captures a SD value at every other coordinate.
     *   Values are stored sequentially by row. Values are
     *   interpolated from samples taken at every 4th block. Note
     *   that <b>an interval of 2 blocks is the highest practical
     *   resolution due to the fact that road vertices are also
     *   calculated at every other block.</b> This drastically
     *   reduces the memory footprint of our data structure.
     * </p>
     * <p>
     *   We can access the approximate value by getting the nearest
     *   even relative coordinate, rounding down, then offsetting
     *   to the appropriate row by x and adding z.
     * </p>
     * <pre>
     *   i = floor(rx / 2) * 8 + floor(rz / 2)
     * </pre>
     * <h4>
     *   Visualization
     * </h4>
     * <p>
     *   Each x in the following grid represents a coordinate
     *   captured by this array.
     * </p>
     * <pre>
     *   F |
     *   E X   x   x   x   x   x   x   x
     *   D |
     *   C X   x   x   x   x   x   x   x
     *   B |
     *   A X   x   x   x   x   x   x   x
     *   9 |
     *   8 X   x   x   x   x   x   x   x
     *   7 |
     *   6 X   x   x   x   x   x   x   x
     *   5 |
     *   4 X   x   x   x   x   x   x   x
     *   3 |
     *   2 X   x   x   x   x   x   x   x
     *   1 |
     *   0 X - X - X - X - X - X - X - X -
     *     0 1 2 3 4 5 6 7 8 9 A B C D E F
     * </pre>
     * <h3>
     *   d
     * </h3>
     * <p>
     *   Raw depth noise calculated at every 4th block.
     * </p>
     * <h4>
     *   Structure
     * </h4>
     * <p>
     *   This field captures raw D noise at every 4th block. This
     *   interval is chosen for 2 reasons:
     * </p>
     * <ul>
     *   <li>To reduce the frequency and cost of calculation</li>
     *   <li>To increase the sensitivity to change over distance</li>
     * </ul>
     * <p>
     *   Because these samples are needed for SD calculations, <b>an
     *   additional buffer of values is calculated outside the
     *   current chunk</b>. Because the 4 corner values are not needed
     *   in this array, <b>the extra space is wasted.</b>
     * </p>
     * <p>
     *   We can access the approximate value by getting the nearest
     *   quarter relative coordinate at an offset of 4, then offsetting
     *   to the appropriate row by x and adding z.
     * </p>
     * <pre>
     *   i = round((rx + 4) / 4) * 7 + round((rz + 4) / 4)
     * </pre>
     * <h4>
     *   Visualization
     * </h4>
     * <pre>
     *   4         X       x       x       x       X
     *   3         |                               |
     *   2         |                               |
     *   1         |                               |
     *   0 x - - - X - - - X - - - X - - - X - - - X - - - x
     *   F         |                               |
     *   E         |                               |
     *   D         |                               |
     *   C x       X       x       x       x       X       x
     *   B         |                               |
     *   A         |                               |
     *   9         |                               |
     *   8 x       X       x       x       x       X       x
     *   7         |                               |
     *   6         |                               |
     *   5         |                               |
     *   4 x       X       x       x       x       X       x
     *   3         |                               |
     *   2         |                               |
     *   1         |                               |
     *   0 x - - - X - - - X - - - X - - - X - - - X - - - x
     *   F         |                               |
     *   E         |                               |
     *   D         |                               |
     *   C         X       x       x       x       X
     *     C D E F 0 1 2 3 4 5 6 7 8 9 A B C D E F 0 1 2 3 4
     * </pre>
     * <h3>
     *   pv
     * </h3>
     * <p>
     *   Raw Peeks-and-Valley noise calculated at every 3rd block.
     * </p>
     * <h4>
     *   Structure
     * </h4>
     * <p>
     *   This field captures the raw PV noise at every 4th block. This
     *   interval is higher than that of PV primarily because we need
     *   higher accuracy in avoiding rivers to reduce obvious jagged
     *   edges.
     * </p>
     * <p>
     *   We can access the approximate value by getting the nearest
     *   relative coordinate at intervals of 3, then offsetting to
     *   the appropriate row by x and adding z.
     * </p>
     * <pre>
     *   i = round(rx / 3) * 6 + round(rz / 3)
     * </pre>
     * <h4>
     *   Visualization
     * </h4>
     * <pre>
     *   F X     x     x     x     x     x
     *   E |
     *   D |
     *   C X     x     x     x     x     x
     *   B |
     *   A |
     *   9 X     x     x     x     x     x
     *   8 |
     *   7 |
     *   6 X     x     x     x     x     x
     *   5 |
     *   4 |
     *   3 X     x     x     x     x     x
     *   2 |
     *   1 |
     *   0 X - - X - - X - - X - - X - - X
     *     0 1 2 3 4 5 6 7 8 9 A B C D E F
     * </pre>
     */
    public record Samples(float[] sd, float[] d, float[] pv, float[] e, Holder<Biome>[] biomes) {
        @SuppressWarnings("unchecked")
        public Samples() {
            this(new float[64], new float[49], new float[36], new float[0], new Holder[5]);
        }

        public float getSd(int rX, int rZ) {
            return this.sd[indexInterval2(rX, rZ)];
        }

        public void setSd(int rX, int rZ, float f) {
            this.sd[indexInterval2(rX, rZ)] = f;
        }

        public float getD(int rX, int rZ) {
            return this.d[indexInterval4o4(rX, rZ)];
        }

        public void setD(int rX, int rZ, float f) {
            this.d[indexInterval4o4(rX, rZ)] = f;
        }

        public float getPv(int rX, int rZ) {
            throw new UnsupportedOperationException("todo");
        }

        public void setPv(int rX, int rZ, float f) {
            throw new UnsupportedOperationException("todo");
        }

        public float getE(int rX, int rZ) {
            throw new UnsupportedOperationException("todo");
        }

        public void setE(int rX, int rZ, float e) {
            throw new UnsupportedOperationException("todo");
        }

        public @Nullable Holder<Biome> getBiome(int rX, int rZ) {
            return this.biomes[indexSpread5(rX, rZ)];
        }

        public void setBiome(int rX, int rZ, Holder<Biome> biome) {
            this.biomes[indexSpread5(rX, rZ)] = biome;
        }
    }

    protected static int lowerQuarter(int i) {
        return i >> 2 << 2;
    }

    protected static int indexInterval2(int x, int z) {
        return ((x >> 1) << 3) + (z >> 1);
    }

    protected static int indexInterval4o4(int x, int z) {
        return ((x + 4) >> 2) * 7 + ((z + 4) >> 2);
    }

    protected static int indexSpread5(int x, int z) {
        if (x == 8 || z == 8) {
            return 2;
        } else if (x < 8) {
            return z < 8 ? 0 : 1;
        }
        return z < 8 ? 3 : 4;
    }
}