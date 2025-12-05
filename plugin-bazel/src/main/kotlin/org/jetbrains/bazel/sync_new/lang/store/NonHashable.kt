package org.jetbrains.bazel.sync_new.lang.store

class NonHashable<T>(val inner: T) {
  override fun equals(other: Any?): Boolean = true
  override fun hashCode(): Int = 0
}
