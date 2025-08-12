package personthecat.pangaea.world.road;

import personthecat.pangaea.data.Point;
import personthecat.pangaea.io.ByteReader;
import personthecat.pangaea.io.ByteWriter;

import java.io.IOException;

public class RoadVertex {
    public static final byte MAX_RADIUS = 8;
    public static final short START = 1;
    public static final short END = 1 << 1;
    public static final short MIDPOINT = 1 << 2;
    public static final short INTERSECTION = 1 << 3;
    public static final short BEND = 1 << 4;
    public static final short TEST = 1 << 5;
    public static final short TEST_2 = 1 << 6;

    public final int x;
    public final int z;
    public byte radius;
    public float theta;
    public float xAngle;
    public short flags;

    public RoadVertex(int x, int z, byte radius, float theta, float xAngle, short flags) {
        this.x = x;
        this.z = z;
        this.radius = radius;
        this.theta = theta;
        this.xAngle = xAngle;
        this.flags = flags;
    }

    public boolean hasFlag(final short flag) {
        return (this.flags & flag) == flag;
    }

    public void addFlag(final short flag) {
        this.flags |= flag;
    }

    public void removeFlag(final short flag) {
        this.flags &= (short) ~flag;
    }

    public Point toPoint() {
        return new Point(this.x, this.z);
    }

    public static RoadVertex fromReader(final ByteReader br) throws IOException {
        return new RoadVertex(
            br.readInt32(),
            br.readInt32(),
            br.read(),
            br.readInt16() / 1_000F,
            br.readInt16() / 1_000F,
            br.readInt16());
    }

    public void writeToStream(final ByteWriter bw) throws IOException {
        bw.writeInt32(this.x);
        bw.writeInt32(this.z);
        bw.write(this.radius);
        bw.writeInt16((short) (this.theta * 1_000F));
        bw.writeInt16((short) (this.xAngle * 1_000F));
        bw.writeInt16(this.flags);
    }

    @Override
    public String toString() {
        return "Vertex[" + this.x + ',' + this.z + ']';
    }
}
