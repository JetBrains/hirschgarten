package org.jetbrains.bazel.sync_new.storage

import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecContext

interface FlatPersistentStore {
  val name: String
  fun write(ctx: CodecContext, buffer: CodecBuffer)
  fun read(ctx: CodecContext, buffer: CodecBuffer)
}

interface PersistentStoreWithModificationMarker {
  val wasModified: Boolean
}

interface PersistentStoreOwner {
  fun register(store: FlatPersistentStore)
  fun unregister(store: FlatPersistentStore)
}
