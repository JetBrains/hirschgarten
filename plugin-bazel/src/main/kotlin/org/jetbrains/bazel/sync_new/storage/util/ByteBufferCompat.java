package org.jetbrains.bazel.sync_new.storage.util;

public interface ByteBufferCompat {
  byte get();
  void put(byte value);
}
