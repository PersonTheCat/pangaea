package personthecat.pangaea.world.road;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate.Sampler;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.world.level.LevelExtras;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class RoadMap {
  private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();
  private static final int CACHE_SIZE = 16;
  private final RoadRegion[] regionCache = new RoadRegion[CACHE_SIZE];
  private final ReferenceQueue<RoadNetwork> networkReferences = new ReferenceQueue<>();
  private final Map<Point, WeakReference<RoadNetwork>> networks = new HashMap<>();
  private final ServerLevel level;
  private final RoadGenerator generator;

  public RoadMap(ServerLevel level) {
    this.level = level;
    this.generator = this.newGenerator();
  }

  public void pregen(final Sampler sampler, final short x, final short z) {
    Pregenerator.create(this).run(sampler, x, z);
  }

  public RoadGenerator newGenerator() {
    return new AStarRoadGenerator(this, this.level);
  }

  public RoadRegion getRegion(final Sampler sampler, final short x, final short z) {
    final RoadRegion r = this.loadPartial(x, z);
    if (!r.isFullyGenerated()) {
      this.generateRegion(r, sampler);
    }
    this.cleanupNetworkCache();
    return this.cacheRegion(r);
  }

  public RoadRegion loadPartial(final short x, final short z) {
    RoadRegion r = this.getCachedRegion(x, z);
    if (r != null) return r;
    r = this.loadRegionFromDisk(x, z);
    if (r != null) return r;
    return new RoadRegion(x, z);
  }

  private RoadRegion getCachedRegion(final short x, final short z) {
    for (int i = 0; i < this.regionCache.length; i++) {
      final RoadRegion r = this.regionCache[i];
      if (r != null && r.x == x && r.z == z) {
        if (i > 0) {
          final RoadRegion up = this.regionCache[i - 1];
          this.regionCache[i] = up;
          this.regionCache[i - 1] = r;
        }
        return r;
      }
    }
    return null;
  }

  public RoadRegion loadRegionFromDisk(final short x, final short z) {
    return RoadRegion.loadFromDisk(this, this.level, x, z);
  }

  public void generateRegion(final Sampler sampler, final short x, final short z) {
    this.generateRegion(new RoadRegion(x, z), this.generator, sampler, Cfg.generatePartial());
  }

  public void generateRegion(
      RoadGenerator gen, Sampler sampler, short x, short z, final boolean partial) {
    this.generateRegion(new RoadRegion(x, z), gen, sampler, partial);
  }

  protected void generateRegion(RoadRegion region, Sampler sampler) {
    this.generateRegion(region, this.generator, sampler, Cfg.generatePartial());
  }

  protected void generateRegion(
      RoadRegion region, RoadGenerator gen, Sampler sampler, boolean partial) {
    final Map<Point, RoadRegion> generated = gen.generateRegion(region, sampler, region.x, region.z, partial);
    for (final RoadRegion r : generated.values()) {
      if (r != region) {
        this.cacheRegion(r);
      }
    }
    if (Cfg.persistRoads()) {
      this.runInBackground(() -> {
        region.saveToDisk(this.level);
        region.forEach(n -> n.saveToDisk(this.level));
      });
    }
  }

  public void runInBackground(final Runnable task) {
    BACKGROUND.submit(task);
  }

  private RoadRegion cacheRegion(final RoadRegion r) {
    for (int i = 0; i < this.regionCache.length; i++) {
      final RoadRegion cached = this.regionCache[i];
      if (cached == r) {
        if (i > 0) {
          final RoadRegion up = this.regionCache[i - 1];
          this.regionCache[i] = up;
          this.regionCache[i - 1] = r;
        }
        return r;
      }
    }
    this.pushToCache(r);
    return r;
  }

  private void pushToCache(final RoadRegion r) {
    System.arraycopy(this.regionCache, 0, this.regionCache, 1, this.regionCache.length - 1);
    this.regionCache[0] = r;
  }

  public void clearCache() {
    Arrays.fill(this.regionCache, null);
    this.networks.clear();
    log.debug("Cleaned region cache for level: {}", this.level);
  }

  public static void clearAll(final MinecraftServer server) {
    for (final ServerLevel level : server.getAllLevels()) {
      LevelExtras.getRoadMap(level).clearCache();
    }
  }

  public void addNetwork(final int x, final int z, final RoadNetwork n) {
    this.networks.put(new Point(x, z), new WeakReference<>(n, this.networkReferences));
  }

  public RoadNetwork getNetwork(final int x, final int z) {
    RoadNetwork n = this.lookupNetwork(x, z);
    if (n != null) return n;
    n = this.loadNetworkFromDisk(x, z);
    if (n != null) {
      this.addNetwork(x, z, n);
    }
    return n;
  }

  protected RoadNetwork lookupNetwork(final int x, final int z) {
    final WeakReference<RoadNetwork> ref = this.networks.get(new Point(x, z));
    return ref != null ? ref.get() : null;
  }

  public RoadNetwork loadNetworkFromDisk(final int x, final int z) {
    return RoadNetwork.loadFromDisk(this.level, x, z);
  }

  public void cleanupNetworkCache() {
    WeakReference<?> ref;
    while ((ref = (WeakReference<?>) this.networkReferences.poll()) != null) {
      this.networks.values().remove(ref);
    }
  }
}
