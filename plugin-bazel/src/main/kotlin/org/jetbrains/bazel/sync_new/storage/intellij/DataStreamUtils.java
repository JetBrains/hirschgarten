package org.jetbrains.bazel.sync_new.storage.intellij;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

final class DataStreamUtils {

  public static void writeVarInt32(DataOutput out, int value) throws IOException {
    while (true) {
      if ((value & ~0x7F) == 0) {
        out.write(value);
        return;
      }
      out.write((value & 0x7F) | 0x80);
      value >>>= 7;
    }
  }

  public static void writeVarInt64(DataOutput out, long value) throws IOException {
    while (true) {
      if ((value & ~((long) 0x7F)) == 0) {
        out.write((int) value);
        return;
      }
      out.write((int) ((value & 0x7F) | 0x80));
      value >>>= 7;
    }
  }

  public static int readVarInt32(DataInput in) throws IOException {
    int result = 0;
    int shift = 0;
    while (true) {
      int b = in.readUnsignedByte();
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        break;
      }
      shift += 7;
    }
    return result;
  }

  public static long readVarInt64(DataInput in) throws IOException {
    long result = 0;
    int shift = 0;
    while (true) {
      int b = in.readUnsignedByte();
      result |= (long)(b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        break;
      }
      shift += 7;
    }
    return result;
  }
}
