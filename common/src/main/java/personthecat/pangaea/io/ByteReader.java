package personthecat.pangaea.io;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ByteReader implements Closeable {
  private final InputStream is;

  public ByteReader(final File f) throws IOException {
    this.is = new BufferedInputStream(new FileInputStream(f));
  }

  public byte read() throws IOException {
    final int b = this.is.read();
    if (b < 0) {
      throw new IOException("Unexpected end of input");
    }
    return (byte) b;
  }

  public boolean readBool() throws IOException {
    return this.read() == 1;
  }

  public short readUInt8() throws IOException {
    return (short) (this.read() & 0xFF);
  }

  public short readInt16() throws IOException {
    return (short) ((this.read() & 255) << 8 | this.read() & 255);
  }

  public int readInt32() throws IOException {
    return (this.read() & 255) << 24
        | (this.read() & 255) << 16
        | (this.read() & 255) << 8
        | this.read() & 255;
  }

  public long readInt64() throws IOException {
    return ((long) this.read() & 255) << 56
        | ((long) this.read() & 255) << 48
        | ((long) this.read() & 255) << 40
        | ((long) this.read() & 255) << 32
        | ((long) this.read() & 255) << 24
        | ((long) this.read() & 255) << 16
        | ((long) this.read() & 255) << 8
        | (long) this.read() & 255;
  }

  public float readFloat32() throws IOException {
    return Float.intBitsToFloat(this.readInt32());
  }

  public double readFloat64() throws IOException {
    return Double.longBitsToDouble(this.readInt64());
  }

  public String readString(final int size) throws IOException {
    final byte[] bytes = new byte[size];
    int bytesLeft = size;
    int offset = 0;

    while (bytesLeft > 0) {
      final int bytesRead = this.is.read(bytes, offset, size);
      if (bytesRead < 0) {
        throw new IOException("Unexpected end of input");
      }
      bytesLeft -= bytesRead;
      offset += bytesRead;
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Override
  public void close() throws IOException {
    this.is.close();
  }
}
