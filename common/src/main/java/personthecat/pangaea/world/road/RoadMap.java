package personthecat.pangaea.world.road;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.LruCache;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.world.level.PangaeaContext;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class RoadMap {
    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();
    private static final int CACHE_SIZE = 16;
    private final LruCache<Point, RoadRegion> regionCache = new LruCache<>(CACHE_SIZE);
    private final ReferenceQueue<RoadNetwork> networkReferences = new ReferenceQueue<>();
    private final Map<Point, WeakReference<RoadNetwork>> networks = new HashMap<>();
    private final ServerLevel level;
    private final RoadSystem system;

    public RoadMap(ServerLevel level) {
        this.level = level;
        this.system = this.createSystem();
    }

    public void pregen(final short x, final short z) {
        Pregenerator.create(this).run(x, z);
    }

    public RoadSystem createSystem() {
        return new RoadSystem(this.level, this);
    }

    public RoadRegion getRegion(final short x, final short z) {
        final RoadRegion r = this.loadPartial(x, z);
        if (!r.isFullyGenerated()) {
            throw new IllegalStateException("Road Region not generated in sequence: " + new Point(x, z));
        }
        return this.cacheRegion(r);
    }

    public void loadOrGenerateRegion(final short x, final short z) {
        final RoadRegion r = this.loadPartial(x, z);
        if (!r.isFullyGenerated()) {
            this.generateRegion(r);
        }
        this.cleanupNetworkCache();
        this.cacheRegion(r);
    }

    public RoadRegion loadPartial(final short x, final short z) {
        RoadRegion r = this.getCachedRegion(x, z);
        if (r != null) return r;
        r = this.loadRegionFromDisk(x, z);
        if (r != null) return r;
        return new RoadRegion(x, z);
    }

    private RoadRegion getCachedRegion(final short x, final short z) {
        return this.regionCache.get(new Point(x, z));
    }

    public RoadRegion loadRegionFromDisk(final short x, final short z) {
        return RoadRegion.loadFromDisk(this, this.level, x, z);
    }

    public void generateRegion(final short x, final short z) {
        this.generateRegion(new RoadRegion(x, z), this.system, Cfg.generatePartial());
    }

    public void generateRegion(RoadSystem system, short x, short z, final boolean partial) {
        this.generateRegion(new RoadRegion(x, z), system, partial);
    }

    protected void generateRegion(RoadRegion region) {
        this.generateRegion(region, this.system, Cfg.generatePartial());
    }

    protected void generateRegion(RoadRegion region, RoadSystem system, boolean partial) {
        final Map<Point, RoadRegion> generated = system.populateRegion(PangaeaContext.get(), region, partial);
        for (final RoadRegion r : generated.values()) {
            if (r != region) {
                this.cacheRegion(r);
            }
        }
        if (Cfg.persistRoads()) {
            this.runInBackground(() -> region.saveToDisk(this.level));
        }
    }

    public void runInBackground(final Runnable task) {
        BACKGROUND.submit(task);
    }

    private RoadRegion cacheRegion(final RoadRegion r) {
        this.regionCache.put(new Point(r.x, r.z), r);
        return r;
    }

    public void clearCache() {
        this.regionCache.clear();
        log.debug("Cleaned region cache for level: {}", this.level);
    }

    public static void clearAll(final MinecraftServer server) {
        for (final ServerLevel level : server.getAllLevels()) {
            LevelExtras.getRoadMap(level).clearCache();
        }
    }

    public void addNetwork(final int x, final int z, final RoadNetwork n) {
        this.addNetwork(new Point(x, z), n);
    }

    public void addNetwork(final Point p, final RoadNetwork n) {
        this.networks.put(p, new WeakReference<>(n, this.networkReferences));
    }

    public RoadNetwork getNetwork(final Point p) {
        final RoadNetwork n = this.lookupNetwork(p);
        if (n != null) return n;
        final short rx = RoadRegion.absToRegion(p.x);
        final short rz = RoadRegion.absToRegion(p.z);
        final RoadRegion r = this.loadPartial(rx, rz);
        return r != null ? r.getNetwork(p) : null;
    }

    protected RoadNetwork lookupNetwork(final Point p) {
        final WeakReference<RoadNetwork> ref = this.networks.get(p);
        return ref != null ? ref.get() : null;
    }

    public void cleanupNetworkCache() {
        WeakReference<?> ref;
        while ((ref = (WeakReference<?>) this.networkReferences.poll()) != null) {
            this.networks.values().remove(ref);
        }
    }
}
