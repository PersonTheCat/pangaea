package personthecat.pangaea.data;

import personthecat.pangaea.world.road.Destination;

@SuppressWarnings("ClassCanBeRecord")
public class Point implements Destination {
  public final int x;
  public final int z;

  public Point(final int x, final int z) {
    this.x = x;
    this.z = z;
  }

  @Override
  public double distance(final int x, final int z, double _min) {
    return Math.sqrt(((this.x - x) * (this.x - x)) + ((this.z - z) * (this.z - z)));
  }

  @Override
  public byte getRoadLevel() {
    return 0;
  }

  @Override
  public int hashCode() {
    return 31 * this.x + this.z;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Point p) {
      return this.x == p.x && this.z == p.z;
    }
    return false;
  }

  @Override
  public String toString() {
    return "(" + this.x + "," + this.z + ")";
  }
}