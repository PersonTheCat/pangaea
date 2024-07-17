package personthecat.pangaea.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.slf4j.helpers.Util.getCallingClass;

public final class Stopwatch {
  private final Logger log = LogManager.getLogger(getCallingClass());
  private BigDecimal sumTime = BigDecimal.ZERO;
  private long startNs = 0;
  private long count = 0;

  public void start() {
    this.startNs = System.nanoTime();
  }

  public void logStart(final String fmt, final Object... args) {
    this.log.printf(Level.INFO, fmt, args);
    this.start();
  }

  public long end() {
    final long diff = System.nanoTime() - this.startNs;
    this.sumTime = this.sumTime.add(BigDecimal.valueOf(diff));
    this.startNs = 0;
    this.count++;
    return diff;
  }

  public long average() {
    return this.sumTime.divide(BigDecimal.valueOf(this.count), RoundingMode.HALF_UP).longValueExact();
  }

  public void logEnd(final String fmt, final Object... args) {
    this.log.printf(Level.INFO, fmt + " (" + formatTime(this.end()) + ")", args);
  }

  public void logAverage(final String fmt, final Object... args) {
    this.log.printf(Level.INFO, fmt + " (" + formatTime(this.average()) + ")", args);
  }

  private static String formatTime(final long ns) {
    if (ns < 1_000) { // prefer decimal ms unless extremely small
      return String.format("%,dns", ns);
    }
    final double ms = exactMs(ns);
    if (ms < 1000) {
      return String.format("%.2fms", ms);
    }
    final double s = ms / 1000;
    if (s < 60) {
      return String.format("%.2fs", s);
    }
    final double m = s / 60;
    if (m < 60) {
      return String.format("%.2fm", m);
    }
    final double h = m / 60;
    if (h < 24) {
      return String.format("%.2fh", h);
    }
    final double d = h / 24;
    return String.format("%.2fd", d);
  }

  @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
  private static double exactMs(final long ns) {
    return new BigDecimal(ns).divide(new BigDecimal(1_000_000L)).doubleValue();
  }
}
