package personthecat.pangaea.world.road;

import personthecat.pangaea.data.Point;
import personthecat.pangaea.util.Utils;

public interface Destination {
    double distance(final int x, final int z, final double min);
    byte getRoadLevel();

    static Destination distanceFrom(Point point, double d) {
        return new DistanceFrom(point, d);
    }

    record DistanceFrom(Point point, double d) implements Destination {

        @Override
        public double distance(int x, int z, double min) {
            final var distance = this.d - Utils.distance(this.point.x, this.point.z, x, z);
            return distance < 4.0 ? distance : 4.0 + (distance - 4.0) * 0.35;
        }

        @Override
        public byte getRoadLevel() {
            return 0;
        }
    }
}
