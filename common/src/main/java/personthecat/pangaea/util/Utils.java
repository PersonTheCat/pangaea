package personthecat.pangaea.util;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import personthecat.catlib.data.Range;
import personthecat.pangaea.data.Rectangle;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class Utils {

  private Utils() {}

  public static void printMemUsage(final String event) {
    final Runtime rt = Runtime.getRuntime();
    final double used = (double) ((rt.totalMemory() - rt.freeMemory()) / 1000) / 1000;
    final long available = rt.totalMemory() / 1000 / 1000;
    System.out.printf("%s: %smb / %smb\n", event, used, available);
  }

  public static void setFeatureSeed(
      final Random rand, final long base, final int cX, final int cY) {
    rand.setSeed((long) cX * 341873128712L + (long) cY * 132897987541L + base);
  }

  public static long getFeatureSeed(final long base, final int cX, final int cY) {
    return (long) cX * 341873128712L + (long) cY * 132897987541L + base;
  }

  public static float getPv(Climate.Sampler sampler, FunctionContext ctx) {
    return NoiseRouterData.peaksAndValleys((float) sampler.weirdness().compute(ctx));
  }

  public static boolean checkDistanceWithFade(RandomSource rand, Range range, int distance, double chance, int fade) {
    if (range.contains(distance + fade)) {
      final var p = getProbabilityWithFade(range, distance, chance, fade);
      return p >= 1 || rand.nextDouble() <= p;
    }
    return false;
  }

  public static double getProbabilityWithFade(Range range, double distance, double chance, int fade) {
    final int min = range.min();
    final int max = range.max();

    if (distance > max) { // fade out
      return chance * Mth.map(distance, max, max + fade, 1, 0);
    } else if (distance < min) { // fade in
      return chance * Mth.map(distance, min - fade, min, 0, 1);
    }
    return chance;
  }

  public static double stdDev(final double... ds) {
    double sum = 0.0;
    for (final double d : ds) {
      sum += d;
    }
    final int len = ds.length;
    final double mean = sum / len;

    double stdDev = 0.0;
    for (double d : ds) {
      stdDev += Math.pow(d - mean, 2);
    }
    return Math.sqrt(stdDev / len);
  }

  public static double squareQuantized(double d) {
    return Climate.unquantizeCoord((long) Math.pow(Climate.quantizeCoord((float) d), 2));
  }

  public static double distance(final int x1, final int y1, final int x2, final int y2) {
    return Math.sqrt(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)));
  }

  public static double distance(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
    return Math.sqrt(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)) + ((z1 - z2) * (z1 - z2)));
  }

  public static float lerp(final float a, final float b, final float t) {
    return a + t * (b - a);
  }

  public static boolean rectanglesOverlap(
      int x1a, int y1a, int x2a, int y2a, int x1b, int y1b, int x2b, int y2b) {
    return !(x1a < x2b && x2a > x1b && y1a > y2b && y2a < y1b);
  }

  public static <T> Set<Rectangle> findRectangles(List<List<T>> matrix) {
    final var rectangles = new HashSet<Rectangle>();
    for (int y = 0; y < matrix.size(); y++) {
      for (int x = 0; x < matrix.getFirst().size(); x++) {
        if (rectanglesContainPoint(rectangles, x, y)) {
          continue;
        }
        final var width = getWidth(matrix, x, y);
        final var height = getHeight(matrix, x, y, width);
        if (width > 1 || height > 1) {
          rectangles.add(new Rectangle(x, y, x + width - 1, y + height - 1));
          x += width - 1;
        }
      }
    }
    return rectangles;
  }

  public static boolean rectanglesContainPoint(Set<Rectangle> rectangles, int x, int y) {
    for (final var rectangle : rectangles) {
      if (rectangle.containsPoint(x, y)) {
        return true;
      }
    }
    return false;
  }

  private static <T> int getWidth(List<List<T>> matrix, int x, int y) {
    final T value = matrix.get(y).get(x);
    int x2 = x + 1;
    for (; x2 < matrix.getFirst().size(); x2++) {
      if (!Objects.equals(matrix.get(y).get(x2), value)) {
        break;
      }
    }
    return x2 - x;
  }

  private static <T> int getHeight(List<List<T>> matrix, int x, int y, int width) {
    final T value = matrix.get(y).get(x);
    int y2 = y + 1;
    for (; y2 < matrix.size(); y2++) {
      if (!rowMatches(matrix, x, y2, value, width)) {
        break;
      }
    }
    return y2 - y;
  }

  private static <T> boolean rowMatches(List<List<T>> matrix, int x, int y, T value, int width) {
    for (int x2 = x; x2 < x + width; x2++) {
      if (!Objects.equals(matrix.get(y).get(x2), value)) {
        return false;
      }
    }
    return true;
  }
}
