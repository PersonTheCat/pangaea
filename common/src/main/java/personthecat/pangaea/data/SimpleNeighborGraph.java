package personthecat.pangaea.data;

import oshi.annotation.concurrent.NotThreadSafe;
import personthecat.pangaea.data.SimpleNeighborGraph.SimpleNode;

public class SimpleNeighborGraph extends NeighborGraph<SimpleNode> {
    private final SimpleNode node;

    public SimpleNeighborGraph(int dimension, int nodeRadius) {
        super(dimension);
        this.node = new SimpleNode((byte) nodeRadius);
    }

    private SimpleNeighborGraph(
            NeighborMap<NeighborMap<SimpleNode>> graph, int dimension, SimpleNode node) {
        super(graph, dimension);
        this.node = node;
    }

    public void plot(int x, int z) {
        this.plot(x, z, n -> this.node);
    }

    @Override
    public void endBatch() {
        super.endBatch();
    }

    public SimpleNeighborGraph withCacheEnabled() {
        return new CacheEnabledGraph(this.graph, this.dimension, this.node);
    }

    public static class SimpleNode extends Node {
        private SimpleNode(byte radius) {
            super(radius);
        }
    }

    @NotThreadSafe
    private static class CacheEnabledGraph extends SimpleNeighborGraph {
        private Search lastSearch;
        private NodeResult<SimpleNode> lastResult;

        public CacheEnabledGraph(
                NeighborMap<NeighborMap<SimpleNode>> graph, int dimension, SimpleNode node) {
            super(graph, dimension, node);
        }

        @Override
        public NodeResult<SimpleNode> getNearest(int x, int z, double min) {
            final var lastSearch = this.lastSearch;
            if (lastSearch != null
                    && lastSearch.x == x
                    && lastSearch.z == z
                    && lastSearch.min <= min) {
                return this.lastResult;
            }
            this.lastSearch = new Search(x, z, min);
            return this.lastResult = super.getNearest(x, z, min);
        }
    }

    private record Search(int x, int z, double min) {}
}