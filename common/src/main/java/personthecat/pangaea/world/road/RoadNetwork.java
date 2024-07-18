package personthecat.pangaea.world.road;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.data.VertexGraph;
import personthecat.pangaea.io.ByteReader;
import personthecat.pangaea.io.ByteWriter;
import personthecat.pangaea.world.level.LevelExtras;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Log4j2
public class RoadNetwork implements Iterable<Road> {
  private static final String SAVE_DIR = "data/networks";
  private static final String EXTENSION = "rn";
  private static final int PADDING = Road.PADDING;

  public final int minX;
  public final int maxX;
  public final int minZ;
  public final int maxZ;
  public final List<Road> roads;
  public final VertexGraph graph;

  public RoadNetwork(final List<Road> roads, final VertexGraph graph) {
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxZ = Integer.MIN_VALUE;
    for (final Road r : roads) {
      minX = Math.min(minX, r.minX());
      maxX = Math.max(maxX, r.maxX());
      minZ = Math.min(minZ, r.minZ());
      maxZ = Math.max(maxZ, r.maxZ());
    }
    this.roads = roads;
    this.minX = minX;
    this.maxX = maxX;
    this.minZ = minZ;
    this.maxZ = maxZ;
    this.graph = graph;
  }

  private RoadNetwork(int minX, int maxX, int minZ, int maxZ, List<Road> roads, VertexGraph graph) {
    this.minX = minX;
    this.maxX = maxX;
    this.minZ = minZ;
    this.maxZ = maxZ;
    this.roads = roads;
    this.graph = graph;
  }

  public boolean containsPoint(final int x, final int z) {
    return x > this.minX - PADDING && x < this.maxX + PADDING && z > this.minZ - PADDING && z < this.maxZ + PADDING;
  }

  public boolean isInQuad(final short qX, final short qZ) {
    final RoadVertex o = this.origin();
    final int aX1 = RoadRegion.quadToAbsolute(qX);
    final int aY1 = RoadRegion.quadToAbsolute(qZ);
    final int aX2 = aX1 + RoadRegion.QUAD_BLOCKS;
    final int aY2 = aY1 + RoadRegion.QUAD_BLOCKS;
    return o.x >= aX1 && o.x < aX2 && o.z >= aY1 && o.z < aY2;
  }

  public double distanceFrom(final int x, final int z, final double min) {
    return this.graph.distance(x, z, min);
  }

  public double checkDistance(final int x, final int z, final double min) {
    if (this.containsPoint(x, z)) {
      return this.distanceFrom(x, z, min);
    }
    return Double.MAX_VALUE;
  }

  public boolean isInRegion(final short rX, final short rZ) {
    return this.getMainRoad().isInRegion(rX, rZ);
  }

  public RoadVertex origin() {
    return this.getMainRoad().vertices()[0];
  }

  public RoadVertex dest() {
    final RoadVertex[] r0 = this.getMainRoad().vertices();
    return r0[r0.length - 1];
  }

  public Road getMainRoad() {
    if (this.roads.isEmpty()) {
      throw new IllegalStateException("Network regenerated empty");
    }
    final Road r0 = this.roads.getFirst();
    if (r0.vertices().length == 0) {
      throw new IllegalStateException("Road generated empty");
    }
    return r0;
  }

  @Override
  public @NotNull Iterator<Road> iterator() {
    return this.roads.iterator();
  }

  public void saveToDisk(final ServerLevel level) {
    final RoadVertex o = this.origin();
    try (final ByteWriter bw = new ByteWriter(getOutputFile(level, o.x, o.z))) {
      this.writeTo(bw);
    } catch (final IOException e) {
      log.error("Error saving network {} to disk", new Point(o.x, o.z));
    }
  }

  private static File getOutputFile(final ServerLevel level, final int x, final int z) {
    final String filename = String.format("%sx%s.%s", x, z, EXTENSION);
    final File f = LevelExtras.getDimensionPath(level).resolve(SAVE_DIR).resolve(filename).toFile();
    try {
      FileUtils.forceMkdir(f.getParentFile());
    } catch (final IOException e) {
      log.error(e); // this exception will get handled down the line
    }
    return f;
  }

  public void writeTo(final ByteWriter writer) throws IOException {
    writer.writeInt32(this.minX);
    writer.writeInt32(this.maxX);
    writer.writeInt32(this.minZ);
    writer.writeInt32(this.maxZ);
    writer.writeInt32(this.roads.size());
    for (final Road road : this.roads) {
      road.writeTo(writer);
    }
    this.graph.writeTo(writer);
  }

  public static RoadNetwork loadFromDisk(final ServerLevel level, final int x, final int z) {
    try (final ByteReader br = new ByteReader(getOutputFile(level, x, z))) {
      return fromReader(br);
    } catch (final FileNotFoundException fnf) {
      return null;
    } catch (final IOException e) {
      log.error("Error loading network ({}, {}) from disk", x, z, e);
      return null;
    }
  }

  private static RoadNetwork fromReader(final ByteReader reader) throws IOException {
    final int minX = reader.readInt32();
    final int maxX = reader.readInt32();
    final int minY = reader.readInt32();
    final int maxY = reader.readInt32();
    final int len = reader.readInt32();
    final List<Road> roads = new ArrayList<>(len);
    for (int i = 0; i < len; i++) {
      roads.add(Road.fromReader(reader));
    }
    final VertexGraph graph = VertexGraph.fromReader(reader);
    return new RoadNetwork(minX, maxX, minY, maxY, roads, graph);
  }

  public static void deleteAllNetworks(final MinecraftServer server) {
    for (final ServerLevel level : server.getAllLevels()) {
      final File f = LevelExtras.getDimensionPath(level).resolve(SAVE_DIR).toFile();
      try {
        FileUtils.forceDelete(f);
      } catch (final IOException e) {
        log.error("Error cleaning road files for dim {}", level.dimension(), e);
      } catch (final NullPointerException ignored) {
        // nothing to delete
      }
    }
  }

  @Override
  public String toString() {
    if (this.roads.isEmpty() || this.roads.getFirst().vertices().length == 0) {
      return "Network[?,?]";
    }
    final RoadVertex o = this.roads.getFirst().vertices()[0];
    return "Network[" + o.x + ',' + o.z + ']';
  }
}
