package personthecat.pangaea.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.util.Utils;

public class NoiseGraph {
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

    protected Samples getData(int cX, int cZ) {
        return this.graph.computeIfAbsent((((long) cX) << 32) | (cZ & 0xFFFFFFFFL), c -> new Samples());
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

    protected double computeSd(final Samples data, final int rX, final int rY) {
        return Utils.stdDev(
            data.getD(rX, rY),
            data.getD(rX, rY + 4),
            data.getD(rX, rY - 4),
            data.getD(rX + 4, rY),
            data.getD(rX - 4, rY)
        );
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
     * <h1>
     *   Samples
     * </h1>
     * <p>
     *   A record of various interpolated noise samples. These
     *   samples are taken at an interval which matches their
     *   default frequencies, thus how likely they are to
     *   significantly change from block to block.
     * </p>
     * <h2>
     *   sd
     * </h2>
     * <p>
     *   SD of D noise in every other block.
     * </p>
     * <h3>
     *   Structure
     * </h3>
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
     * <h3>
     *   Visualization
     * </h3>
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
     * <h2>
     *   d
     * </h2>
     * <p>
     *   Raw depth noise calculated at every 4th block.
     * </p>
     * <h3>
     *   Structure
     * </h3>
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
     * <h3>
     *   Visualization
     * </h3>
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
     * <h2>
     *   pv
     * </h2>
     * <p>
     *   Raw Peeks-and-Valley noise calculated at every 3rd block.
     * </p>
     * <h3>
     *   Structure
     * </h3>
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
     * <h3>
     *   Visualization
     * </h3>
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
    public record Samples(float[] sd, float[] d, float[] pv, float[] e) {
        public Samples() {
            this(new float[64], new float[49], new float[36], new float[0]);
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
}
