package personthecat.pangaea.io;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteWriter implements Closeable {
  private final OutputStream os;

  public ByteWriter(final File f) throws IOException {
    this.os = new BufferedOutputStream(new FileOutputStream(f));
  }

  public void write(final byte b) throws IOException {
    this.os.write(b);
  }

  public void writeBool(final boolean b) throws IOException {
    this.os.write(b ? 1 : 0);
  }

  public void writeInt8(final byte value) throws IOException {
    this.os.write(value);
  }

  public void writeUInt8(final short value) throws IOException {
    this.os.write(value & 0xFF);
  }

  public void writeInt16(final short value) throws IOException {
    this.os.write(value >> 8);
    this.os.write(value);
  }

  public void writeInt32(final int value) throws IOException {
    this.os.write(value >> 24);
    this.os.write(value >> 16);
    this.os.write(value >> 8);
    this.os.write(value);
  }

  public void writeInt64(final long value) throws IOException {
    this.os.write((byte) (0xFF & (value >> 56)));
    this.os.write((byte) (0xFF & (value >> 48)));
    this.os.write((byte) (0xFF & (value >> 40)));
    this.os.write((byte) (0xFF & (value >> 32)));
    this.os.write((byte) (0xFF & (value >> 24)));
    this.os.write((byte) (0xFF & (value >> 16)));
    this.os.write((byte) (0xFF & (value >> 8)));
    this.os.write((byte) (0xFF & value));
  }

  public void writeFloat32(final float value) throws IOException {
    this.writeInt32(Float.floatToIntBits(value));
  }

  public void writeFloat64(final double value) throws IOException {
    this.writeInt64(Double.doubleToLongBits(value));
  }

  public void writeBytes(final byte[] bytes) throws IOException {
    this.os.write(bytes);
  }

  @Override
  public void close() throws IOException {
    this.os.close();
  }
}
