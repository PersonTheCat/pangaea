package personthecat.pangaea.util;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.NoiseRouterData;

import java.util.Random;

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
}
