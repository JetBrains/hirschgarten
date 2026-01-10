package org.jetbrains.bazel.sync_new.storage.util;

public final class ByteBufferUtils {
  public static void writeVarInt(ByteBufferCompat buffer, int x) {
    while ((x & ~0x7f) != 0) {
      buffer.put((byte) (x | 0x80));
      x >>>= 7;
    }
    buffer.put((byte) x);
  }

  public static void writeVarLong(ByteBufferCompat buffer, long x) {
    while ((x & ~0x7f) != 0) {
      buffer.put((byte) (x | 0x80));
      x >>>= 7;
    }
    buffer.put((byte) x);
  }

  public static int readVarInt(ByteBufferCompat buffer) {
    // unrolled read loop
    int b = buffer.get();
    if (b >= 0) {
      return b;
    }
    int x = b & 0x7f;
    b = buffer.get();
    if (b >= 0) {
      return x | (b << 7);
    }
    x |= (b & 0x7f) << 7;
    b = buffer.get();
    if (b >= 0) {
      return x | (b << 14);
    }
    x |= (b & 0x7f) << 14;
    b = buffer.get();
    if (b >= 0) {
      return x | b << 21;
    }
    x |= ((b & 0x7f) << 21) | (buffer.get() << 28);
    return x;
  }

  public static long readVarLong(ByteBufferCompat buffer) {
    long x = buffer.get();
    if (x >= 0) {
      return x;
    }
    x &= 0x7f;
    for (int s = 7; s < 64; s += 7) {
      long b = buffer.get();
      x |= (b & 0x7f) << s;
      if (b >= 0) {
        break;
      }
    }
    return x;
  }


  public static void writeInt(ByteBufferCompat buffer, int value) {
    buffer.put((byte)(value >> 24));
    buffer.put((byte)(value >> 16));
    buffer.put((byte)(value >> 8));
    buffer.put((byte)value);
  }

  public static void writeLong(ByteBufferCompat buffer, long value) {
    buffer.put((byte)(value >> 56));
    buffer.put((byte)(value >> 48));
    buffer.put((byte)(value >> 40));
    buffer.put((byte)(value >> 32));
    buffer.put((byte)(value >> 24));
    buffer.put((byte)(value >> 16));
    buffer.put((byte)(value >> 8));
    buffer.put((byte)value);
  }

  public static int readInt(ByteBufferCompat buffer) {
    byte b1 = buffer.get();
    byte b2 = buffer.get();
    byte b3 = buffer.get();
    byte b4 = buffer.get();
    return ((b1 & 0xFF) << 24) |
           ((b2 & 0xFF) << 16) |
           ((b3 & 0xFF) << 8) |
           (b4 & 0xFF);
  }

  public static long readLong(ByteBufferCompat buffer) {
    byte b1 = buffer.get();
    byte b2 = buffer.get();
    byte b3 = buffer.get();
    byte b4 = buffer.get();
    byte b5 = buffer.get();
    byte b6 = buffer.get();
    byte b7 = buffer.get();
    byte b8 = buffer.get();
    return ((b1 & 0xFFL) << 56) |
           ((b2 & 0xFFL) << 48) |
           ((b3 & 0xFFL) << 40) |
           ((b4 & 0xFFL) << 32) |
           ((b5 & 0xFFL) << 24) |
           ((b6 & 0xFFL) << 16) |
           ((b7 & 0xFFL) << 8) |
           (b8 & 0xFFL);
  }
}
