package personthecat.pangaea.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import personthecat.pangaea.io.ByteReader;
import personthecat.pangaea.io.ByteWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntFunction;

public class NeighborMap<T> {
  private static final Entry<?>[] EMPTY = new Entry<?>[0];

  private final Int2ObjectMap<Entry<T>> entries;
  private Entry<T>[] sorted;

  public NeighborMap() {
    this(256);
  }

  public NeighborMap(int expected) {
    this(new Int2ObjectOpenHashMap<>(expected), newEntries(0));
  }

  protected NeighborMap(
      final Int2ObjectMap<Entry<T>> entries, final Entry<T>[] sorted) {
    this.entries = entries;
    this.sorted = sorted;
  }

  @SuppressWarnings("unchecked")
  protected static <T> Entry<T>[] newEntries(final int size) {
    return (Entry<T>[]) (size > 0 ? new Entry[size] : EMPTY);
  }

  public T get(final int c) {
    final Entry<T> e = this.entries.get(c);
    return e != null ? e.t : null;
  }

  public T compute(final int c, final ComputeFunction<T> f) {
    final T t = f.apply(c, this.get(c));
    this.put(c, t);
    return t;
  }

  public T computeIfAbsent(final int c, final IntFunction<T> ifAbsent) {
    if (this.entries.containsKey(c)) {
      return this.get(c);
    }
    final T t = ifAbsent.apply(c);
    this.entries.put(c, new Entry<>(c, t));
    return t;
  }

  public NeighborMap<T> put(final int c, final T t) {
    this.entries.put(c, new Entry<>(c, t));
    return this;
  }

  public NeighborMap<T> endBatch() {
    final ObjectCollection<Entry<T>> values = this.entries.values();
    this.sorted = values.toArray(newEntries(values.size()));
    Arrays.sort(this.sorted, Comparator.comparingInt(e -> e.c));
    return this;
  }

  public void clear() {
    this.entries.clear();
    this.sorted = newEntries(0);
  }

  public EntryResult first() {
    final Entry<T>[] entries = this.sorted;
    return entries.length != 0 ? new EntryResult(0, entries[0]) : null;
  }

  public EntryResult last() {
    final Entry<T>[] entries = this.sorted;
    final int lastIdx = entries.length - 1;
    return lastIdx != -1 ? new EntryResult(lastIdx, entries[lastIdx]) : null;
  }

  public EntryResult nearestAbove(final int c) {
    final Entry<T>[] entries = this.sorted;
    if (entries.length == 0) {
      return null;
    }
    final Entry<T> first = entries[0];
    if (c < first.c) {
      return new EntryResult(0, first);
    }
    int lo = 0;
    int hi = entries.length;
    while (lo != hi) {
      int mid = (lo + hi) >> 1;
      if (entries[mid].c <= c) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return hi < entries.length ? new EntryResult(hi, entries[hi]) : null;
  }

  public EntryResult getNearest(final int c) {
    final Entry<T>[] entries = this.sorted;
    if (entries.length == 0) {
      return null;
    }
    final Entry<T> first = entries[0];
    if (c < first.c) {
      return new EntryResult(0, first);
    }
    final int lastIdx = entries.length - 1;
    final Entry<T> last = entries[lastIdx];
    if (c > last.c) {
      return new EntryResult(lastIdx, last);
    }
    int lo = 0;
    int hi = lastIdx;
    while (lo <= hi) {
      int mid = (hi + lo) >> 1;
      final Entry<T> e = entries[mid];
      if (c < e.c) {
        hi = mid - 1;
      } else if (c > e.c) {
        lo = mid + 1;
      } else {
        return new EntryResult(mid, e);
      }
    }
    final Entry<T> eHi = entries[hi];
    final Entry<T> eLo = entries[lo];
    return (eLo.c - c) < (c - eHi.c) ? new EntryResult(lo, eLo) : new EntryResult(hi, eHi);
  }

  public ObjectCollection<Entry<T>> entries() {
    return this.entries.values();
  }

  public void writeTo(
      final ByteWriter writer, final WriteFunction<T> tWriter) throws IOException {
    writer.writeInt32(this.sorted.length); // disregard unsorted entries
    for (final Entry<T> e : this.sorted) {
      writer.writeInt32(e.c);
      tWriter.accept(writer, e.t);
    }
  }

  public static <T> NeighborMap<T> fromReader(
      final ByteReader reader, final ReadFunction<T> tReader) throws IOException {
    final int len = reader.readInt32();
    final Entry<T>[] sorted = newEntries(len);
    for (int i = 0; i < len; i++) {
      final int c = reader.readInt32();
      final T t = tReader.apply(reader);
      sorted[i] = new Entry<>(c, t);
    } // disregard unsorted entries. we won't need them for this application
    return new NeighborMap<>(new Int2ObjectOpenHashMap<>(), sorted);
  }

  @FunctionalInterface
  public interface ComputeFunction<T> {
    T apply(final int c, final T t);
  }

  @FunctionalInterface
  public interface WriteFunction<T> {
    void accept(final ByteWriter writer, final T t) throws IOException;
  }

  @FunctionalInterface
  public interface ReadFunction<T> {
    T apply(final ByteReader reader) throws IOException;
  }

  public static class Entry<T> {
    public final int c;
    public T t;

    protected Entry(final int c, final T t) {
      this.c = c;
      this.t = t;
    }
  }

  public class EntryResult extends Entry<T> {
    protected final int i;

    protected EntryResult(final int i, final Entry<T> e) {
      super(e.c, e.t);
      this.i = i;
    }

    public EntryResult previous() {
      final int previous = this.i - 1;
      if (previous >= 0) {
        return new EntryResult(previous, NeighborMap.this.sorted[previous]);
      }
      return null;
    }

    public EntryResult next() {
      final Entry<T>[] entries = NeighborMap.this.sorted;
      final int next = this.i + 1;
      if (next < entries.length) {
        return new EntryResult(next, entries[next]);
      }
      return null;
    }
  }
}
