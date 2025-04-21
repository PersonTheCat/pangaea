package personthecat.pangaea.data;

import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.road.RoadVertex;
import personthecat.pangaea.data.NeighborGraph.Node;

import java.util.function.UnaryOperator;

public abstract class NeighborGraph<N extends Node> {
    protected static final int MIN_SIG_DISTANCE = 32;
    protected static final int SCAN_RADIUS = MIN_SIG_DISTANCE + RoadVertex.MAX_RADIUS;
    protected static final int DEFAULT_DIMENSION = 256;

    protected final NeighborMap<NeighborMap<N>> graph;
    protected final int dimension;

    public NeighborGraph() {
        this(DEFAULT_DIMENSION);
    }

    public NeighborGraph(int dimension) {
        this(new NeighborMap<>(dimension), dimension);
    }

    protected NeighborGraph(NeighborMap<NeighborMap<N>> graph, int dimension) {
        this.graph = graph;
        this.dimension = dimension;
    }

    protected void plot(int x, int z, UnaryOperator<N> f) {
        this.graph.computeIfAbsent(x, _x -> new NeighborMap<>())
            .compute(z, (_z, n) -> f.apply(n));
    }

    protected void endBatch() {
        this.graph.endBatch();
        this.graph.entries().forEach(e -> e.t.endBatch());
    }

    public boolean isEmpty() {
        return this.graph.entries().isEmpty();
    }

    public double distance(int x, int z, double min) {
        final NodeResult<N> nearest = this.getNearest(x, z, min);
        return nearest != null ? nearest.distance : Double.MAX_VALUE;
    }

    public NodeResult<N> getNearest(int x, int z, double min) {
        final NeighborMap<NeighborMap<N>>.EntryResult centerColumn = this.graph.getNearest(x);
        if (centerColumn == null) {
            return null;
        }
        double d = Double.MAX_VALUE;
        N n = null;
        int nX = -1;
        int nY = -1;
        // scan right
        NeighborMap<NeighborMap<N>>.EntryResult column = centerColumn;

        while (column != null) {
            final NeighborMap<N>.EntryResult centerNode = column.t.getNearest(z);
            if (centerNode == null) {
                continue;
            }
            // scan up
            NeighborMap<N>.EntryResult node = centerNode;
            while (node != null) {
                final double d1 = Utils.distance(column.c, node.c, x, z) - node.t.radius;
                if (d1 < d) {
                    d = d1;
                    n = node.t;
                    nX = column.c;
                    nY = node.c;
                    if (d <= min) {
                        return new NodeResult<>(column.c, node.c, n, d);
                    }
                }
                if (Math.abs(node.c - z) > SCAN_RADIUS) {
                    break;
                }
                node = node.next();
            }
            // scan down
            node = centerNode.previous();
            while (node != null && Math.abs(node.c - z) <= SCAN_RADIUS) {
                final double d1 = Utils.distance(column.c, node.c, x, z) - node.t.radius;
                if (d1 < d) {
                    d = d1;
                    n = node.t;
                    nX = column.c;
                    nY = node.c;
                    if (d <= min) {
                        return new NodeResult<>(column.c, node.c, n, d);
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
            final NeighborMap<N>.EntryResult centerNode = column.t.getNearest(z);
            if (centerNode == null) {
                continue;
            }
            // scan up
            NeighborMap<N>.EntryResult node = centerNode;
            while (node != null) {
                final double d1 = Utils.distance(column.c, node.c, x, z) - node.t.radius;
                if (d1 < d) {
                    d = d1;
                    n = node.t;
                    nX = column.c;
                    nY = node.c;
                    if (d <= min) {
                        return new NodeResult<>(column.c, node.c, n, d);
                    }
                }
                if (Math.abs(node.c - z) > SCAN_RADIUS) {
                    break;
                }
                node = node.next();
            }
            // scan down
            node = centerNode.previous();
            while (node != null && Math.abs(node.c - z) <= SCAN_RADIUS) {
                final double d1 = Utils.distance(column.c, node.c, x, z) - node.t.radius;
                if (d1 < d) {
                    d = d1;
                    n = node.t;
                    nX = column.c;
                    nY = node.c;
                    if (d <= min) {
                        return new NodeResult<>(column.c, node.c, n, d);
                    }
                }
                node = node.previous();
            }
            column = column.previous();
        }
        return n != null ? new NodeResult<>(nX, nY, n, d) : null;
    }

    public record NodeResult<N>(int x, int z, N n, double distance) {}

    public static abstract class Node {
        protected byte radius;

        protected Node(byte radius) {
            this.radius = radius;
        }

        public byte radius() {
            return this.radius;
        }
    }
}
