package org.jetbrains.bazel.sync_new.flow

import com.dynatrace.hash4j.hashing.HashValue128
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.util.containers.BidirectionalMap
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import java.nio.file.Path

@Tagged
@ClassTag(2034668847)
data class SyncMetadata(
  @field:Tag(1)
  val configHash: HashValue128,

  @field:Tag(2)
  val bazelVersion: String?,
)

@SealedTagged
@ClassTag(710118612)
sealed interface SyncRepoMapping

@SealedTag(1)
@Tagged
@ClassTag(1032108528)
object DisabledSyncRepoMapping : SyncRepoMapping

@SealedTag(2)
@Tagged
@ClassTag(942665062)
data class BzlmodSyncRepoMapping(
  @field:Tag(1)
  val canonicalRepoNameToLocalPath: Map<String, Path> = mapOf(),

  @field:Tag(2)
  val apparentToCanonical: BidirectionalMap<String, String> = BidirectionalMap(),

  @field:Tag(3)
  val canonicalToPath: Map<String, Path> = mapOf(),
) : SyncRepoMapping
