package personthecat.pangaea.world.road;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.io.ByteReader;
import personthecat.pangaea.io.ByteWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Log4j2
public class RoadRegion implements Iterable<RoadNetwork> {
  private static final String TEMP_SAVE_DIR = "regions";
  private static final String EXTENSION = "rr";
  public static final int LEN = 2048;
  public static final int CHUNK_LEN = LEN / 16;
  public static final int QUAD_BLOCKS = LEN / 2;
  public static final int QUAD_CHUNK_LEN = CHUNK_LEN / 2;
  public static final int QUAD_CHUNK_SHIFT = (int) (Math.log(QUAD_CHUNK_LEN) / Math.log(2));
  private static final int MASK = LEN - 1;
  private static final int SHIFT = (int) (Math.log(LEN) / Math.log(2));
  public static final int OFFSET = -LEN / 2;
  public static final int CHUNK_OFFSET = OFFSET / 16;
  public static final byte UL_QUAD = 1;
  public static final byte UR_QUAD = 1 << 1;
  public static final byte DL_QUAD = 1 << 2;
  public static final byte DR_QUAD = 1 << 3;
  public static final byte ALL_QUADS = 0b1111;

  public final short x;
  public final short z;
  private byte quads;
  private final List<RoadNetwork> data;

  public RoadRegion(final short x, final short z) {
    this(x, z, (byte) 0, new ArrayList<>());
  }

  protected RoadRegion(final short x, final short z, final List<RoadNetwork> roads) {
    this(x, z, (byte) 0, roads);
  }

  private RoadRegion(final short x, final short z, final byte quads, final List<RoadNetwork> data) {
    this.x = x;
    this.z = z;
    this.quads = quads;
    this.data = data;
  }

  public boolean containsPoint(final int x, final int z) {
    final int aX = regionToAbs(this.x);
    final int aY = regionToAbs(this.z);
    return x >= aX && x < (aX + LEN) && z >= aY && z < (aY + LEN);
  }

  public boolean isFullyGenerated() {
    return this.quads == ALL_QUADS;
  }

  public List<RoadNetwork> getData() {
    return this.data;
  }

  public void setQuadGenerated(final short qX, final short qZ) {
    this.setQuadFlag(getQuadFlag(qX, qZ));
  }

  public void setQuadFlag(final byte quad) {
    this.quads |= quad;
  }

  public boolean hasQuad(final short qX, final short qZ) {
    return this.hasQuadFlag(getQuadFlag(qX, qZ));
  }

  public boolean hasQuadFlag(final byte quad) {
    return (this.quads & quad) == quad;
  }

  public void copyQuadInto(final RoadRegion r, final short qX, final short qZ) {
    for (final RoadNetwork n : this.data) {
      if (n.isInQuad(qX, qZ)) {
        r.data.add(n);
      }
    }
  }

  @NotNull
  @Override
  public Iterator<RoadNetwork> iterator() {
    return this.data.iterator();
  }

  public void saveToDisk(final long seed) {
    try (final ByteWriter bw = new ByteWriter(getOutputFile(seed, this.x, this.z))) {
      this.writeTo(bw);
    } catch (final IOException e) {
      log.error("Error saving region ({}, {}) to disk", this.x, this.z, e);
    }
  }

  private static File getOutputFile(final long seed, final int x, final int z) {
    final File f = new File(TEMP_SAVE_DIR, String.format("%s/%sx%s.%s", seed, x, z, EXTENSION));
    try {
      FileUtils.forceMkdir(f.getParentFile());
    } catch (final IOException e) {
      log.error(e); // this exception will get handled down the line
    }
    return f;
  }

  private void writeTo(final ByteWriter bw) throws IOException {
    bw.writeInt16(this.x);
    bw.writeInt16(this.z);
    bw.writeInt8(this.quads);
    bw.writeInt32(this.data.size());
    for (final RoadNetwork network : this.data) {
      final RoadVertex o = network.origin();
      bw.writeInt32(o.x);
      bw.writeInt32(o.z);
    }
  }

  public static RoadRegion loadFromDisk(final RoadMap map, final long seed, final int x, final int z) {
    try (final ByteReader br = new ByteReader(getOutputFile(seed, x, z))) {
      return fromReader(map, br);
    } catch (final FileNotFoundException fnf) {
      return null;
    } catch (final IOException e) {
      log.error("Error loading region ({}, {}) from disk", x, z, e);
      return null;
    }
  }

  private static RoadRegion fromReader(final RoadMap map, final ByteReader br) throws IOException {
    final short x = br.readInt16();
    final short z = br.readInt16();
    final byte quads = br.read();
    final int len = br.readInt32();
    final List<RoadNetwork> data = new ArrayList<>();
    for (int i = 0; i < len; i++) {
      final int nX = br.readInt32();
      final int nZ = br.readInt32();
      final RoadNetwork n = map.getNetwork(nX, nZ);
      if (n != null) {
        data.add(n);
      } else {
        System.err.printf("Error loading network: %s,%s (it must have been deleted)", nX, nZ);
      }
    }
    return new RoadRegion(x, z, quads, data);
  }

  @Override
  public String toString() {
    return "Region[" + this.x + ',' + this.z + ']';
  }

  public static void deleteAllRegions() {
    try {
      FileUtils.forceDelete(new File(TEMP_SAVE_DIR));
    } catch (final IOException e) {
      log.error("Error cleaning region files", e);
    } catch (final NullPointerException ignored) {
      // nothing to delete
    }
  }

  public static short absToRel(final int c) {
    return (short) ((c + OFFSET) & MASK);
  }

  public static short absToRegion(final int c) {
    return (short) ((c - OFFSET) >> SHIFT);
  }

  public static int regionToAbs(final int c) {
    return (c * LEN) + OFFSET;
  }

  public static int toChunkCoord(final int c) {
    return (c * CHUNK_LEN) + CHUNK_OFFSET;
  }

  public static int quadToAbsolute(final int c) {
    return (c * LEN / 2) + OFFSET;
  }

  public static int quadToChunk(final int c) {
    return quadToAbsolute(c) >> 4;
  }

  public static int chunkToQuad(final int c) {
    return (c + CHUNK_OFFSET) >> QUAD_CHUNK_SHIFT;
  }

  public static byte getQuadFlag(final short qX, final short qZ) {
    if (qX % 2 == 0) { // offset, so even quads are higher in region
      return qZ % 2 == 0 ? UL_QUAD : UR_QUAD;
    }
    return qZ % 2 == 0 ? DL_QUAD : DR_QUAD;
  }
}
