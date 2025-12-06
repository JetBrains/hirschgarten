package org.jetbrains.bazel.sync_new.storage.util;

import java.nio.ByteBuffer;

public final class ByteBufferBufferUtils {
  public static void writeVarInt(ByteBuffer buffer, int x) {
    while ((x & ~0x7f) != 0) {
      buffer.put((byte) (x | 0x80));
      x >>>= 7;
    }
    buffer.put((byte) x);
  }

  public static void writeVarLong(ByteBuffer buffer, long x) {
    while ((x & ~0x7f) != 0) {
      buffer.put((byte) (x | 0x80));
      x >>>= 7;
    }
    buffer.put((byte) x);
  }

  public static int readVarInt(ByteBuffer buffer) {
    // unrolled write loop
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

  public static long readVarLong(ByteBuffer buffer) {
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
}
