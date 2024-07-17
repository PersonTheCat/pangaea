package personthecat.pangaea.world.road;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate.Sampler;
import personthecat.pangaea.data.Point;

import java.util.List;

public class AStarRoadGenerator extends RoadGenerator {
  private static final byte DEMO_RADIUS_0 = 3;
  private static final byte DEMO_RADIUS_1 = 2;
  private static final byte DEMO_RADIUS_2 = 2;
  private static final float DEMO_INTEGRITY = 0.65F;

  private final AStar aStar;

  public AStarRoadGenerator(final RoadMap map, ServerLevel level) {
    super(map, level);
    this.aStar = new AStar(this.graph);
  }

  @Override
  protected Road trace(Sampler sampler, Point src, Destination dest) {
    this.aStar.reset(sampler);
    final List<Point> path = this.aStar.search(src, dest);
    if (path == null) {
      return null;
    }
    final int l = dest.getRoadLevel();
    final byte radius = switch (l) {
      case 0 -> DEMO_RADIUS_0;
      case 1 -> DEMO_RADIUS_1;
      default -> DEMO_RADIUS_2;
    };
    int minX = Integer.MAX_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxZ = Integer.MIN_VALUE;
    final int len = path.size();
    final RoadVertex[] vertices = new RoadVertex[len];
    for (int i = len - 1; i >= 0; i--) {
      final Point p = path.get(i);
      final float theta;
      final float xAngle;
      if (i > 0 && i < len - 1) {
        // Check ahead by 2, if possible. Lazy way to get the angle over a large distance
        final Point prev = i < len - 2 ? path.get(i + 2) : path.get(i + 1);
        final Point next = i > 1 ? path.get(i - 2) : path.getFirst(); // i - 1 = 0
        final float a1 = (float) Math.atan2(prev.z - p.z, prev.x - p.x);
        final float a2 = (float) Math.atan2(next.z - p.z, next.x - p.x);
        final float t = a2 - a1;
        theta = t < 0 ? t + TAU : t;
        xAngle = a2;
      } else {
        theta = -1;
        xAngle = -1;
      }
      if (p.x < minX) minX = p.x;
      if (p.z < minZ) minZ = p.z;
      if (p.x > maxX) maxX = p.x;
      if (p.z > maxZ) maxZ = p.z;
      final RoadVertex v = new RoadVertex(p.x, p.z, radius, DEMO_INTEGRITY, theta, xAngle, (short) 0);
      vertices[path.size() - i - 1] = v;
      if (i == len - 1) {
        v.addFlag(RoadVertex.START);
      } else if (i == 0) {
        v.addFlag(RoadVertex.END);
      } else {
        v.addFlag(RoadVertex.MIDPOINT);
      }
    }
    this.smoothAngles(vertices);
    return new Road((byte) l, minX, minZ, maxX, maxZ, vertices);
  }

  private void smoothAngles(final RoadVertex[] vertices) {
    final int amount = 1;
    final float[] angles = new float[vertices.length - amount];
    for (int i = amount; i < vertices.length - amount; i++) {
      float sum = 0;
      for (int s = -amount; s <= amount; s++) {
        sum += vertices[i + s].theta;
      }
      angles[i - amount] = sum / (amount * 2 + 1);
    }
    for (int i = 0; i < angles.length; i++) {
      vertices[i + amount].theta = angles[i];
    }
  }

}
