package personthecat.pangaea.world.road;

public interface Destination {
  double distance(final int x, final int z, final double min);
  byte getRoadLevel();
}
