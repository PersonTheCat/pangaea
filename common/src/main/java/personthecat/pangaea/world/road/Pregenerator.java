package personthecat.pangaea.world.road;

import lombok.extern.log4j.Log4j2;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.util.Stopwatch;
import personthecat.pangaea.util.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public abstract class Pregenerator {
    protected final RoadMap map;
    protected final Stopwatch sw = new Stopwatch();
    protected final boolean[][] shape;

    protected Pregenerator(final RoadMap map) {
        this.map = map;
        if (Cfg.debugPregenShape()) {
            final int r = Cfg.pregenRadius();
            final int d = r * 2 + 1;
            this.shape = new boolean[d][d];
        } else {
            this.shape = null;
        }
    }

    static Pregenerator create(final RoadMap map) {
        if (Cfg.pregenThreadCount() < 2) {
            return new SingleThreaded(map);
        }
        return new Multithreaded(map);
    }

    public final void run(final short x, final short y) {
        this.sw.logStart("pre-generating roads...");
        this.generateRegions(x, y);
    }

    protected abstract void generateRegions(final short x, final short y);

    protected List<Point> getSortedOrigins(final short x, final short z) {
        final List<Point> origins = new ArrayList<>();
        final int r = Cfg.pregenRadius();
        final int l = r * 2 + 1;
        final float s = Cfg.pregenSkew();
        final float a = s * (float) Math.sqrt(r);

        // generates a plus-shaped pattern to optimize number of regions generated
        for (int rX = x - r; rX <= x + r; rX++) {
            final int xO = rX - x;
            final int u = (int) -(Math.pow((1 / a) * xO, 2)) + r;
            final int d = -u;
            final int l1 = (int) (a * Math.sqrt(xO + r));
            final int l2 = -l1;
            final int r1 = (int) (a * Math.sqrt(-xO + r));
            final int r2 = -r1;
            for (int rZ = z - r; rZ <= z + r; rZ++) {
                final int zO = rZ - z;
                if ((zO <= u && zO >= d) || (xO < 0 ? (zO >= l2 && zO <= l1) : (zO >= r2 && zO <= r1))) {
                    origins.add(new Point(rX, rZ));
                    this.addToShape(l - (zO + r) -1, xO + r);
                }
            }
        }
        // sort by distance from x, y
        origins.sort(Comparator.comparingDouble(p -> Utils.distance(p.x, p.z, x, z)));
        return origins;
    }

    protected void addToShape(final int x, final int z) {
        if (this.shape != null) {
            this.shape[z][x] = true;
        }
    }

    protected void printDiagnostics(final int count, final int max) {
        final int r = Cfg.pregenRadius();
        final int d = r * 2 + 1;
        final int dB = d * RoadRegion.LEN;
        final int dC = d * RoadRegion.CHUNK_LEN;
        this.printShape();
        this.sw.logEnd("pre-generated %s / %s regions = %s^2r = %s^2b = %s^2c", count, max, d, dB, dC);
    }

    protected void printShape() {
        if (this.shape != null) {
            final StringBuilder sb = new StringBuilder("\nShape generated:\n");
            final String border = "+-" + "-".repeat(this.shape.length * 2) + "+\n";
            sb.append(border);
            for (final boolean[] row : this.shape) {
                sb.append("| ");
                for (final boolean b : row) {
                    sb.append(b ? '#' : ' ').append(' ');
                }
                sb.append("|\n");
            }
            sb.append(border);
            log.info(sb);
        }
    }

    public static class SingleThreaded extends Pregenerator {

        protected SingleThreaded(final RoadMap map) {
            super(map);
        }

        @Override
        protected void generateRegions(final short x, final short z) {
            final List<Point> origins = this.getSortedOrigins(x, z);
            int count = 0;
            for (final Point o : origins) {
                if (this.map.loadRegionFromDisk((short) o.x, (short) o.z) == null) {
                    this.map.generateRegion((short) o.x, (short) o.z);
                    count++;
                }
            }
            this.printDiagnostics(count, origins.size());
        }
    }

    public static class Multithreaded extends Pregenerator {
        protected Multithreaded(final RoadMap map) {
            super(map);
        }

        @Override
        protected void generateRegions(final short x, final short y) {
            try (ExecutorService executor = Executors.newFixedThreadPool(Cfg.pregenThreadCount())) {
                final ThreadLocal<RoadSystem> system = ThreadLocal.withInitial(this.map::createSystem);
                final AtomicInteger count = new AtomicInteger(0);

                final List<Future<Void>> futures = this.getSortedOrigins(x, y).stream()
                        .map(o -> executor.<Void>submit(() -> {
                            if (this.map.loadRegionFromDisk((short) o.x, (short) o.z) == null) {
                                this.map.generateRegion(system.get(), (short) o.x, (short) o.z, false);
                                count.incrementAndGet();
                            }
                        }, null))
                        .toList();

                this.map.runInBackground(() -> this.awaitCompletion(executor, futures, count));
            }
        }

        private void awaitCompletion(
                final ExecutorService executor, final List<Future<Void>> futures, final AtomicInteger count) {
            for (final Future<?> f : futures) {
                try {
                    f.get();
                } catch (final ExecutionException | InterruptedException e) {
                    log.error("Error generating region", e);
                }
            }
            this.printDiagnostics(count.get(), futures.size());
            executor.shutdown();
        }
    }
}
