package personthecat.pangaea.world.road;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.biome.Climate.Sampler;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.util.Stopwatch;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class AStar {
  protected static final Stopwatch sw = new Stopwatch();
  protected final PriorityQueue<Candidate> openList;
  protected final BitSet closedSet;
  protected final Long2ObjectMap<Cell> cellDetails;
  protected Sampler sampler;
  protected NoiseGraph graph;
  protected Point found;

  public AStar(final NoiseGraph graph) {
    this.openList = new PriorityQueue<>(Comparator.comparingDouble(c -> c.priority));
    this.closedSet = new BitSet(RoadRegion.LEN / 2 * RoadRegion.LEN / 2);
    this.cellDetails = new Long2ObjectOpenHashMap<>(Road.MAX_DISTANCE);
    this.graph = graph;
  }

  public void reset(final Sampler sampler) {
    this.openList.clear();
    this.closedSet.clear();
    this.cellDetails.clear();
    this.sampler = sampler;
  }

  public final List<Point> search(final Point src, final Destination dest) {
    if (!Cfg.debugBranches()) {
      return this.doSearch(src, dest);
    }
    sw.logStart("generating road: %s -> %s", src, dest);
    final List<Point> path = this.doSearch(src, dest);
    if (path != null) {
      sw.logEnd("generated road: %s -> %s, len = %s", src, dest, path.size());
    } else {
      sw.logEnd("generated nothing: %s -> %s, len = 0", src, dest);
    }
    sw.logAverage("path avg");
    return path;
  }

  protected List<Point> doSearch(final Point src, final Destination dest) {
    int x = src.x;
    int z = src.z;

    this.setDetails(x, z, new Cell(x, z, 0, 0));
    this.open(0.0, x, z);

    int len = 0;
    while (this.hasNext() && len++ < Road.MAX_LENGTH) {
      final Candidate c = this.next();
      x = c.x;
      z = c.z;
      this.close(x, z);

      if (this.checkDirection(dest, 2, x, z, x - 2, z)
          || this.checkDirection(dest, 2, x, z, x + 2, z)
          || this.checkDirection(dest, 2, x, z, x , z + 2)
          || this.checkDirection(dest, 2, x, z, x, z - 2)
          || this.checkDirection(dest, 2.83, x, z, x - 2, z + 2)
          || this.checkDirection(dest, 2.83, x, z, x - 2, z - 2)
          || this.checkDirection(dest, 2.83, x, z, x + 2, z + 2)
          || this.checkDirection(dest, 2.83, x, z, x + 2, z - 2)) {
        return this.tracePath();
      }
    }
    return null;
  }

  private Cell getDetails(final int x, final int z) {
    return this.cellDetails.get((((long) x) << 32) | (z & 0xFFFFFFFFL));
  }

  private void setDetails(final int x, final int z, final Cell cell) {
    this.cellDetails.put((((long) x) << 32) | (z & 0xFFFFFFFFL), cell);
  }

  private void open(final double priority, final int x, final int z) {
    this.openList.offer(new Candidate(priority, x, z));
  }

  private boolean hasNext() {
    return !this.openList.isEmpty();
  }

  private Candidate next() {
    return this.openList.poll();
  }

  private void close(final int x, final int z) {
    final int xO = RoadRegion.absToRel(x + RoadRegion.OFFSET) / 2;
    final int zO = RoadRegion.absToRel(z + RoadRegion.OFFSET) / 2;
    this.closedSet.set(xO * (RoadRegion.LEN / 2) + zO);
  }

  private boolean isClosed(final int x, final int z) {
    final int xO = RoadRegion.absToRel(x + RoadRegion.OFFSET) / 2;
    final int zO = RoadRegion.absToRel(z + RoadRegion.OFFSET) / 2;
    return this.closedSet.get(xO * (RoadRegion.LEN / 2) + zO);
  }

  private boolean checkDirection(Destination dest, double d, int pX, int pZ, int x, int z) {
    final double h = dest.distance(x, z, 2);
    if (h < 2) {
      this.setParentIndex(x, z, pX, pZ);
      this.found = new Point(x, z);
      return true;
    }
    if (this.isClosed(x, z)) {
      return false;
    }
    final double weight = TmpRoadUtils.getWeight(this.graph, this.sampler, new MutableFunctionContext().at(x, z));
    if (weight > 100) {
      return false;
    }
    final Cell cell = this.getDetails(x, z);
    final double g = getG(cell) + d;
    final double r = getCurve(x, z);
    double f = g + h + r + weight;
    if (cell == null || cell.f > f) {
      this.open(f, x, z);
      this.setDetails(x, z, new Cell(pX, pZ, f, g));
    }
    return false;
  }

  private void setParentIndex(final int x, final int z, final int pX, final int pZ) {
    Cell cell = this.getDetails(x, z);
    if (cell == null) {
      cell = new Cell();
      this.setDetails(x, z, cell);
    }
    cell.pX = pX;
    cell.pZ = pZ;
  }

  protected List<Point> tracePath() {
    int x = this.found.x;
    int z = this.found.z;
    Cell cell = this.getDetails(x, z);

    final List<Point> path = new ArrayList<>();
    while (!(cell.pX == x && cell.pZ == z)) {
      path.add(new Point(x, z));
      x = cell.pX;
      z = cell.pZ;
      cell = getDetails(x, z);
    }
    path.add(new Point(x, z));
    return path;
  }

  protected static double getG(final Cell cell) {
    return cell != null ? cell.g : 0;
  }

  protected static double getCurve(final int x, final int z) { // will take: dest, h
    return Math.sin(x * z);
  }

  protected record Candidate(double priority, int x, int z) {}

  protected static class Cell {
    int pX;
    int pZ;
    double f;
    double g;

    Cell() {}

    Cell(final int pX, final int pZ, final double f, final double g) {
      this.pX = pX;
      this.pZ = pZ;
      this.f = f;
      this.g = g;
    }
  }
}
