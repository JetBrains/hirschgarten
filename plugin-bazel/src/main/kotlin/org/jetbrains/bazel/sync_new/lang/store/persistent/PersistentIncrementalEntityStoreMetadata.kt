package org.jetbrains.bazel.sync_new.lang.store.persistent

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged

@Tagged
data class PersistentIncrementalEntityStoreMetadata(
  @field:Tag(1)
  var resourceIdCounter: Int = 1
)
