package personthecat.pangaea.data;

import personthecat.pangaea.io.ByteReader;
import personthecat.pangaea.io.ByteWriter;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.road.Destination;
import personthecat.pangaea.world.road.Road;
import personthecat.pangaea.world.road.RoadVertex;

import java.io.IOException;

public final class VertexGraph {
  private static final int MIN_SIG_DISTANCE = 32;
  private static final int SCAN_RADIUS = MIN_SIG_DISTANCE + RoadVertex.MAX_RADIUS;

  private final NeighborMap<NeighborMap<Node>> graph;

  public VertexGraph() {
    this.graph = new NeighborMap<>();
  }

  private VertexGraph(final NeighborMap<NeighborMap<Node>> graph) {
    this.graph = graph;
  }

  public void plot(final Road r) {
    for (final RoadVertex v : r.vertices()) {
      this.plot(v, r.level());
    }
    this.endBatch();
  }

  public void plot(final RoadVertex v, final byte l) {
    this.graph.computeIfAbsent(v.x, _x -> new NeighborMap<>())
        .compute(v.z, (_y, n) -> (n == null) ? new Node(v, l) : n.putUnder(v, l));
  }

  public void endBatch() {
    this.graph.endBatch();
    this.graph.entries().forEach(e -> e.t.endBatch());
  }

  public double distance(final int x, final int y, final double min) {
    final VertexResult nearest = this.getNearest(x, y, min);
    return nearest != null ? nearest.distance : Double.MAX_VALUE;
  }

  public VertexResult getNearest(final int x, final int y, final double min) {
    final NeighborMap<NeighborMap<Node>>.EntryResult centerColumn = this.graph.getNearest(x);
    if (centerColumn == null) {
      return null;
    }
    double d = Double.MAX_VALUE;
    Node n = null;
    int nX = -1;
    int nY = -1;
    // scan right
    NeighborMap<NeighborMap<Node>>.EntryResult column = centerColumn;

    while (column != null) {
      final NeighborMap<Node>.EntryResult centerNode = column.t.getNearest(y);
      if (centerNode == null) {
        continue;
      }
      // scan up
      NeighborMap<Node>.EntryResult node = centerNode;
      while (node != null) {
        final double d1 = Utils.distance(column.c, node.c, x, y) - node.t.radius;
        if (d1 < d) {
          d = d1;
          n = node.t;
          nX = column.c;
          nY = node.c;
          if (d <= min) {
            return new VertexResult(column.c, node.c, n, d);
          }
        }
        if (Math.abs(node.c - y) > SCAN_RADIUS) {
          break;
        }
        node = node.next();
      }
      // scan down
      node = centerNode.previous();
      while (node != null && Math.abs(node.c - y) <= SCAN_RADIUS) {
        final double d1 = Utils.distance(column.c, node.c, x, y) - node.t.radius;
        if (d1 < d) {
          d = d1;
          n = node.t;
          nX = column.c;
          nY = node.c;
          if (d <= min) {
            return new VertexResult(column.c, node.c, n, d);
          }
        }
        node = node.previous();
      }
      if (Math.abs(column.c - x) > SCAN_RADIUS) {
        break;
      }
      column = column.next();
    }
    // scan left
    column = centerColumn.previous();

    while (column != null && Math.abs(column.c - x) <= SCAN_RADIUS) {
      final NeighborMap<Node>.EntryResult centerNode = column.t.getNearest(y);
      if (centerNode == null) {
        continue;
      }
      // scan up
      NeighborMap<Node>.EntryResult node = centerNode;
      while (node != null) {
        final double d1 = Utils.distance(column.c, node.c, x, y) - node.t.radius;
        if (d1 < d) {
          d = d1;
          n = node.t;
          nX = column.c;
          nY = node.c;
          if (d <= min) {
            return new VertexResult(column.c, node.c, n, d);
          }
        }
        if (Math.abs(node.c - y) > SCAN_RADIUS) {
          break;
        }
        node = node.next();
      }
      // scan down
      node = centerNode.previous();
      while (node != null && Math.abs(node.c - y) <= SCAN_RADIUS) {
        final double d1 = Utils.distance(column.c, node.c, x, y) - node.t.radius;
        if (d1 < d) {
          d = d1;
          n = node.t;
          nX = column.c;
          nY = node.c;
          if (d <= min) {
            return new VertexResult(column.c, node.c, n, d);
          }
        }
        node = node.previous();
      }
      column = column.previous();
    }
    return n != null ? new VertexResult(nX, nY, n, d) : null;
  }

  public Target getTarget(final int x, final int y, final double min) {
    final VertexResult v = this.getNearest(x, y, min);
    return v != null ? new Target(v) : null;
  }

  public void writeTo(final ByteWriter writer) throws IOException {
    this.graph.writeTo(writer,
        (w1, column) -> column.writeTo(w1,
            (w2, node) -> w2.write(node.radius)));
  }

  public static VertexGraph fromReader(final ByteReader reader) throws IOException {
    return new VertexGraph(NeighborMap.fromReader(reader,
        r1 -> NeighborMap.fromReader(r1,
            r2 -> new Node(r2.read()))));
  }

  public static class Node {
    protected byte radius;
    protected byte level;

    protected Node(final RoadVertex vertex, final byte level) {
      this.radius = vertex.radius;
      this.level = level;
    }

    protected Node(final byte radius) {
      this.radius = radius;
    }

    protected Node putUnder(final RoadVertex v, final byte l) {
      // we don't need the actual vertex info yet, so ignoring for now
      this.radius = (byte) Math.max(this.radius, v.radius);
      this.level = (byte) Math.max(this.level, l);
      v.addFlag(RoadVertex.INTERSECTION);
      return this;
    }
  }

  public record VertexResult(int x, int y, byte radius, byte level, double distance) {
    private VertexResult(final int x, final int y, final Node n, final double distance) {
      this(x, y, n.radius, n.level, distance);
    }
  }

  public class Target implements Destination {
    public final int x;
    public final int y;
    public byte l;

    protected Target(final VertexResult v) {
      this.x = v.x;
      this.y = v.y;
      this.l = (byte) (v.level + 1);
    }

    // to optimize slightly, store the target once we are close enough
    public double distance(int x, int z, double min) {
      final VertexResult nearest = VertexGraph.this.getNearest(x, z, min);
      if (nearest == null) return Double.MAX_VALUE;
      final double d = nearest.distance;
      if (d < SCAN_RADIUS) {
        this.l = (byte) (nearest.level + 1);
        return d;
      }
      // tried interpolating, wasn't noticeable enough to be worth it
      return Math.sqrt(((this.x - x) * (this.x - x)) + ((this.y - z) * (this.y - z)));
    }

    @Override
    public byte getRoadLevel() {
      return this.l;
    }
  }
}
