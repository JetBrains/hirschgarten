package org.jetbrains.bazel.sync_new.lang.store

fun interface IncrementalEntityCreator<E : IncrementalEntity> {
  fun create(id: Int): E
}
