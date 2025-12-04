package org.jetbrains.bazel.sync_new.lang.store

fun interface IncrementalResourceIdCreator<R : IncrementalResourceId> {
  fun create(id: Int): R
}
