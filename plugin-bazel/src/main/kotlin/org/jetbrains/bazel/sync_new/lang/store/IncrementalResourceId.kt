package org.jetbrains.bazel.sync_new.lang.store

interface IncrementalResourceId {
  val id: NonHashable<Int>
}

val IncrementalResourceId.idInt: Int get() = id.inner
