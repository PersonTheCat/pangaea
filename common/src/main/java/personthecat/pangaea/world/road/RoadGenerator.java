package personthecat.pangaea.world.road;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate.Sampler;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.data.VertexGraph;
import personthecat.pangaea.util.Stopwatch;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.extras.LevelExtras;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Log4j2
public abstract class RoadGenerator {
  protected static final short QUAD_RADIUS = 2;
  protected static final int SCAN_RADIUS = 32;
  protected static final int SCAN_STEP = Road.STEP;
  protected static final float TAU = (float) (Math.PI * 2);

  protected final RoadMap map;
  protected final ServerLevel level;
  protected final NoiseGraph graph;
  protected final Stopwatch sw = new Stopwatch();

  protected RoadGenerator(final RoadMap map, final ServerLevel level) {
    this.map = map;
    this.level = level;
    this.graph = LevelExtras.getNoiseGraph(level);
  }

  public final Map<Point, RoadRegion> generateRegion(
      RoadRegion region, Sampler sampler, short x, short z, boolean partial) {
    this.sw.logStart("Generating road map %s, %s", x, z);
    final Map<Point, RoadRegion> generated = this.generate(region, sampler, x, z, partial);
    this.sw.logEnd("map done %s, %s", x, z);
    this.sw.logAverage("average");
    this.graph.clear();
    return generated;
  }

  protected Map<Point, RoadRegion> generate(
      RoadRegion region, Sampler sampler, short x, short z, boolean partial) {
    final Map<Point, RoadRegion> generated = new HashMap<>();
    final short cQX = (short) ((x * 2) + 1);
    final short cQZ = (short) ((z * 2) + 1);
    generated.put(new Point(x, z), region);

    for (short qX = (short) (cQX - QUAD_RADIUS); qX < cQX + QUAD_RADIUS; qX++) {
      for (short qZ = (short) (cQZ - QUAD_RADIUS); qZ < cQZ + QUAD_RADIUS; qZ++) {
        final short xO = (short) (qX / 2);
        final short zO = (short) (qZ / 2);
        // in current region
        if (xO == x && zO == z) {
          if (!region.hasQuad(qX, qZ)) {
            this.generateQuad(region, region, sampler, qX, qZ, x, z, partial);
            region.setQuadGenerated(qX, qZ);
          }
          continue;
        }
        final Point pO = new Point(xO, zO);
        final RoadRegion rO = generated.computeIfAbsent(pO, p -> this.map.loadPartial((short) p.x, (short) p.z));
        if (rO.hasQuad(qX, qZ)) {
          rO.copyQuadInto(region, qX, qZ);
        } else {
          this.generateQuad(region, rO, sampler, qX, qZ, x, z, partial);
          rO.setQuadGenerated(qX, qZ);
        }
        generated.put(pO, rO);
      }
    }
    // Previously kept all networks to avoid some overlap. Can safely remove them now.
    region.getData().removeIf(n -> !n.isInRegion(x, z));
    return generated;
  }

  protected void generateQuad(
      RoadRegion region, RoadRegion rO, Sampler sampler, short qX, short qZ, short pX, short pZ, boolean partial) {
    final int cXO = RoadRegion.quadToChunk(qX);
    final int cZO = RoadRegion.quadToChunk(qZ);
    final long seed = this.level.getSeed();
    final Random rand = new Random(seed);
    final float chance = Cfg.roadChance();
    for (int cX = cXO; cX < cXO + RoadRegion.QUAD_CHUNK_LEN; cX++) {
      for (int cZ = cZO; cZ < cZO + RoadRegion.QUAD_CHUNK_LEN; cZ++) {
        Utils.setFeatureSeed(rand, seed, cX, cZ);
        if (rand.nextFloat() > chance) {
          continue;
        }
        final Point center = new Point((cX << 4) + 8, (cZ << 4) + 8);
        final Point nearest = this.getNearestSuitable(sampler, center);
        if (nearest == null) {
          continue;
        }
        RoadNetwork n = this.map.getNetwork(nearest.x, nearest.z);
        if (n == null) {
          n = this.generateNetwork(region, sampler, rand, nearest, pX, pZ, partial);
          if (n != null) {
            this.map.addNetwork(nearest.x, nearest.z, n);
          }
        }
        if (n != null) {
          region.getData().add(n);
          if (partial && region != rO) {
            rO.getData().add(n);
          }
        }
      }
    }
  }

  protected RoadNetwork generateNetwork(
      RoadRegion region, Sampler sampler, Random rand, Point src, short pX, short pZ, boolean partial) {
    // build main road
    final Road r0 = this.getMainRoad(region, sampler, src, rand);
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
    for (int i = 0; i < Cfg.maxBranches(); i++) {
      final double aO = a + Math.PI / 2 + rand.nextFloat() * Math.PI;
      final double d = min + rand.nextInt(max - min + 1);
      final int xO = (int) (cX + d * Mth.cos((float) aO));
      final int zO = (int) (cZ + d * Mth.sin((float) aO));
      final Point s = this.getNearestSuitable(sampler, new Point(xO, zO));
      if (s == null) {
        continue;
      }
      // trace road to the nearest vertex
      final Road rN = this.trace(sampler, s, target);
      if (rN != null) {
        // to be correct, we need to flag all points in range.
        rN.last().addFlag(RoadVertex.INTERSECTION);
        roads.add(rN);
        graph.plot(rN);
      }
    }
    return new RoadNetwork(roads, graph);
  }

  protected Road getMainRoad(RoadRegion region, Sampler sampler, Point src, Random rand) {
    final int minL = Cfg.minRoadLength();
    final int maxL = Cfg.maxRoadLength();
    final int d = minL + rand.nextInt(maxL - minL);
    final Road r = this.trace(sampler, src, Destination.distanceFrom(src, d));
    return r == null || this.isTooClose(region, r) ? null : r;
  }

  protected boolean isTooClose(RoadRegion region, Road r) {
    final RoadVertex cv1 = r.vertices()[r.vertices().length / 2];
    final int cX1 = cv1.x;
    final int cZ1 = cv1.z;
    final int r1 = r.length() / 2;

    for (final RoadNetwork n : region) {
      final Road m = n.getMainRoad();
      final RoadVertex cv2 = m.vertices()[m.vertices().length / 2];
      final int cX2 = cv2.x;
      final int cZ2 = cv2.z;
      final int r2 = m.length() / 2;
      // too close if >30% overlap
      if (Utils.distance(cX1, cZ1, cX2, cZ2) <= (r1 + r2) * 0.7) {
        return true;
      }
    }
    return false;
  }

  protected Point getNearestSuitable(final Sampler sampler, final Point point) {
    final MutableFunctionContext ctx = new MutableFunctionContext();
    if (sampler.continentalness().compute(ctx.at(point.x, point.z)) < 0) {
      return null;
    }
    double minWeight = Double.MAX_VALUE;
    int bestX = Integer.MAX_VALUE;
    int bestZ = Integer.MAX_VALUE;
    for (int xO = point.x - SCAN_RADIUS; xO < point.x + SCAN_RADIUS; xO += SCAN_STEP) {
      for (int zO = point.z - SCAN_RADIUS; zO < point.z + SCAN_RADIUS; zO += SCAN_STEP) {
        final double weight = TmpRoadUtils.getWeight(this.graph, sampler, ctx.at(xO, zO));
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

  protected abstract Road trace(final Sampler sampler, final Point src, final Destination dest);
}
