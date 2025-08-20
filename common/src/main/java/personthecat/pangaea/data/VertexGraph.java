package personthecat.pangaea.data;

import personthecat.pangaea.io.ByteReader;
import personthecat.pangaea.io.ByteWriter;
import personthecat.pangaea.world.road.Destination;
import personthecat.pangaea.world.road.Road;
import personthecat.pangaea.world.road.RoadVertex;

import personthecat.pangaea.data.VertexGraph.VertexNode;

import java.io.IOException;

public class VertexGraph extends NeighborGraph<VertexNode> {

    public VertexGraph() {}

    private VertexGraph(NeighborMap<NeighborMap<VertexNode>> graph, int dimension) {
        super(graph, dimension);
    }

    public void plot(final Road r) {
        for (final RoadVertex v : r.vertices()) {
            this.plot(v, r.level());
        }
        this.endBatch();
    }

    public void plot(final RoadVertex v, final byte l) {
        this.plot(v.x, v.z, n -> (n == null) ? new VertexNode(v, l) : n.putUnder(v, l));
    }

    @Override
    public void endBatch() {
        super.endBatch();
    }

    public Target getTarget(final int x, final int y, final double min) {
        final NodeResult<VertexNode> n = this.getNearest(x, y, min);
        return n != null ? new Target(n) : null;
    }

    public void writeTo(final ByteWriter writer) throws IOException {
        writer.writeInt32(this.dimension);
        this.graph.writeTo(writer,
            (w1, column) -> column.writeTo(w1, (w2, node) -> node.writeTo(w2)));
    }

    public static VertexGraph fromReader(final ByteReader reader) throws IOException {
        final int dimension = reader.readInt32();
        final var graph = NeighborMap.fromReader(reader,
            r1 -> NeighborMap.fromReader(r1, VertexNode::fromReader));
        return new VertexGraph(graph, dimension);
    }

    public static class VertexNode extends Node {
        private byte level;
        private final float xAngle;

        private VertexNode(RoadVertex v, byte level) {
            this(v.radius, level, v.xAngle);
        }

        private VertexNode(byte radius, byte level, float xAngle) {
            super(radius);
            this.level = level;
            this.xAngle = xAngle;
        }

        public byte level() {
            return this.level;
        }

        public float xAngle() {
            return this.xAngle;
        }

        private VertexNode putUnder(RoadVertex v, byte level) {
            // we don't need the actual vertex info yet, so ignoring for now
            this.radius = (byte) Math.max(this.radius, v.radius);
            this.level = (byte) Math.max(this.level, level);
            v.addFlag(RoadVertex.INTERSECTION);
            return this;
        }

        private void writeTo(ByteWriter tw) throws IOException {
            tw.write(this.radius);
            tw.write(this.level);
            tw.writeInt16((short) (this.xAngle * 1_000F));
        }

        private static VertexNode fromReader(ByteReader reader) throws IOException {
            return new VertexNode(reader.read(), reader.read(), reader.readInt16() / 1_000F);
        }
    }

    public class Target implements Destination {
        public final int x;
        public final int z;
        public byte l;

        protected Target(final NodeResult<VertexNode> v) {
            this.x = v.x();
            this.z = v.z();
            this.l = (byte) (v.n().level + 1);
        }

        // to optimize slightly, store the target once we are close enough
        public double distance(int x, int z, double min) {
            final NodeResult<VertexNode> nearest = VertexGraph.this.getNearest(x, z, min);
            if (nearest == null) return Double.MAX_VALUE;
            final double d = nearest.distance();
            if (d < SCAN_RADIUS) {
                this.l = (byte) (nearest.n().level + 1);
                return d;
            }
            // tried interpolating, wasn't noticeable enough to be worth it
            return Math.sqrt(((this.x - x) * (this.x - x)) + ((this.z - z) * (this.z - z)));
        }

        @Override
        public byte getRoadLevel() {
            return this.l;
        }
    }
}