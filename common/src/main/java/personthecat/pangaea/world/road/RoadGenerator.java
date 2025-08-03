package personthecat.pangaea.world.road;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.data.VertexGraph;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.road.RoadConfig.DestinationStrategy;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class RoadGenerator<C extends RoadConfig> {
    protected static final int SCAN_RADIUS = 32;
    protected static final int SCAN_STEP = Road.STEP;
    protected static final float TAU = (float) (Math.PI * 2);

    protected final ServerLevel level;
    protected final RoadMap map;
    protected final NoiseGraph graph;
    protected final C cfg;

    protected RoadGenerator(ServerLevel level, C cfg) {
        this.level = level;
        this.map = LevelExtras.getRoadMap(level);
        this.graph = LevelExtras.getNoiseGraph(level);
        this.cfg = cfg;
    }

    public final @Nullable RoadNetwork generateNetwork(
            PangaeaContext ctx, RoadRegion region, int cX, int cZ, short pX, short pZ, boolean partial) {
        if (!this.isValidForChunk(ctx, cX, cZ)) {
            return null;
        }
        final var nearest = this.getNearestSuitable(ctx, (cX << 4) + 8, (cZ << 4) + 8);
        if (nearest == null) {
            return null;
        }
        final var n = this.map.getNetwork(nearest);
        if (n == null) {
            return this.generateNetwork(ctx, region, nearest, pX, pZ, partial);
        }
        return n;
    }

    protected abstract boolean isValidForChunk(PangaeaContext ctx, int cX, int cZ);

    protected Point getNearestSuitable(PangaeaContext ctx, int x, int z) {
        // skip if obviously in ocean biome
        if (this.isClearlyUnsuitable(x, z)) {
            return null;
        }
        double minWeight = Double.MAX_VALUE;
        int bestX = Integer.MAX_VALUE;
        int bestZ = Integer.MAX_VALUE;
        for (int xO = x - SCAN_RADIUS; xO < x + SCAN_RADIUS; xO += SCAN_STEP) {
            for (int zO = z - SCAN_RADIUS; zO < z + SCAN_RADIUS; zO += SCAN_STEP) {
                final double weight = this.getWeight(ctx, xO, zO);
                if (weight < 5) {
                    return new Point(xO, zO);
                } else if (weight < minWeight) {
                    minWeight = weight;
                    bestX = xO;
                    bestZ = zO;
                }
            }
        }
        if (minWeight != Double.MAX_VALUE) {
            return new Point(bestX, bestZ);
        }
        return null;
    }

    protected boolean isClearlyUnsuitable(int x, int z) {
        return this.graph.getApproximateContinentalness(x, z) < 0;
    }

    protected abstract double getWeight(PangaeaContext ctx, int x, int z);

    protected @Nullable RoadNetwork generateNetwork(
            PangaeaContext ctx, RoadRegion region, Point src, short pX, short pZ, boolean partial) {
        // build main road
        final Road r0 = this.getMainRoad(ctx, region, src);
        if (r0 == null || (!partial && !r0.isInRegion(pX, pZ))) {
            return null;
        }
        log.info("Generating network at {}", src);
        // calculate bounds of network
        final int w = r0.maxX() - r0.minX();
        final int h = r0.maxZ() - r0.minZ();
        final int bx1; // left
        final int bZ1; // up
        final int bx2; // right
        final int bz2; // down
        if (w > h) {
            // increase h to w
            final int m = (w - h) / 2;
            final int pad = w / 10;
            bx1 = r0.minX() - pad;
            bx2 = r0.maxX() + pad;
            bZ1 = r0.minZ() - m - pad;
            bz2 = r0.maxZ() + m + pad;
        } else {
            // increase w to h;
            final int m = (h - w) / 2;
            final int pad = h / 10;
            bx1 = r0.minX() - m - pad;
            bx2 = r0.maxX() + m + pad;
            bZ1 = r0.minZ() - pad;
            bz2 = r0.maxZ() + pad;
        }
        // setup list of roads
        final List<Road> roads = new ArrayList<>();
        final VertexGraph graph = new VertexGraph();
        roads.add(r0);
        graph.plot(r0);
        // generate random points in circle from center
        final int max = (bx2 - bx1) / 2; // 1/2 from center
        final int min = max / 2;         // 1/4 from center
        final int cX = (bx2 + bx1) / 2;
        final int cZ = (bz2 + bZ1) / 2;
        final float a = r0.broadAngle();
        final VertexGraph.Target target = graph.getTarget(cX, cZ, 10.0);
        assert target != null;
        for (int i = 0; i < this.getBranchCount(ctx); i++) {
            final double aO = a + Math.PI / 2 + ctx.rand.nextFloat() * Math.PI;
            final double d = min + ctx.rand.nextInt(max - min + 1);
            final int xO = (int) (cX + d * Mth.cos((float) aO));
            final int zO = (int) (cZ + d * Mth.sin((float) aO));
            final Point s = this.getNearestSuitable(ctx, xO, zO);
            if (s == null) {
                continue;
            }
            // trace road to the nearest vertex
            final Road rN = this.trace(ctx, s, target);
            if (rN != null) {
                // to be correct, we need to flag all points in range.
                rN.last().addFlag(RoadVertex.INTERSECTION);
                roads.add(rN);
                graph.plot(rN);
            }
        }
        return new RoadNetwork(roads, graph);
    }

    protected @Nullable Road getMainRoad(PangaeaContext ctx, RoadRegion region, Point src) {
        final int minL = Cfg.minRoadLength();
        final int maxL = Cfg.maxRoadLength();
        final int d = minL + ctx.rand.nextInt(maxL - minL);

        if (this.cfg.destinationStrategy() == DestinationStrategy.PATH_BETWEEN) {
            final float a = ctx.rand.nextFloat() * TAU; // any angle
            final int aX = (int) (src.x + d * Mth.cos(a));
            final int aZ = (int) (src.z + d * Mth.sin(a));
            if (!this.isTooClose(region, src, new Point(aX, aZ))) {
                final var dest = this.getNearestSuitable(ctx, aX, aZ);
                if (dest != null) {
                    return this.trace(ctx, src, dest);
                }
            }
            return null;
        }
        final Road r = this.trace(ctx, src, Destination.distanceFrom(src, d));
        return r == null || this.isTooClose(region, r) ? null : r;
    }

    protected final boolean isTooClose(RoadRegion region, Point src, Point dest) {
        final int cX = (src.x + dest.x) / 2;
        final int cZ = (src.z + dest.z) / 2;
        return this.isTooClose(region, src, dest, new Point(cX, cZ));
    }

    protected final boolean isTooClose(RoadRegion region, Road r) {
        return this.isTooClose(region, r.first().toPoint(), r.last().toPoint(), r.mid().toPoint());
    }

    protected boolean isTooClose(RoadRegion region, Point src, Point dest, Point mid) {
        final int r1 = (int) Utils.distance(src.x, src.z, dest.x, dest.z) / 2;

        for (final RoadNetwork n : region) {
            final Road r = n.getMainRoad();
            final RoadVertex cv = r.mid();
            final int r2 = r.length() / 2;
            // too close if >30% overlap
            if (Utils.distance(mid.x, mid.z, cv.x, cv.z) <= (r1 + r2) * 0.7) {
                return true;
            }
        }
        return false;
    }

    protected abstract int getBranchCount(PangaeaContext ctx);

    protected abstract @Nullable Road trace(PangaeaContext ctx, Point src, Destination dest);
}