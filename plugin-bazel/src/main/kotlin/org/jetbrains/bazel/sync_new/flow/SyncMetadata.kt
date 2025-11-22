package org.jetbrains.bazel.sync_new.flow

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import java.nio.file.Path

@Tagged
data class SyncMetadata(

  @field:Tag(1)
  val repoMapping: SyncRepoMapping = DisabledSyncRepoMapping,
)

sealed interface SyncRepoMapping

object DisabledSyncRepoMapping : SyncRepoMapping

data class BzlmodSyncRepoMapping(
  @field:Tag(1)
  val canonicalRepoNameToLocalPath: Map<String, Path> = mapOf(),

  @field:Tag(2)
  val apparentToCanonical: BiMap<String, String> = HashBiMap.create(),

  @field:Tag(3)
  val canonicalToPath: Map<String, Path> = mapOf(),
) : SyncRepoMapping
