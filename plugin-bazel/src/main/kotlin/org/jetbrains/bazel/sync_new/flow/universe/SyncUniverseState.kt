package org.jetbrains.bazel.sync_new.flow.universe

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.flow.DisabledSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping

@Tagged
data class SyncUniverseState(
  @field:Tag(1)
  val phase: SyncUniversePhase,
  @field:Tag(2)
  val importState: SyncUniverseImportState = SyncUniverseImportState(),
  @field:Tag(3)
  val repoMapping: SyncRepoMapping = DisabledSyncRepoMapping,
)

@EnumTagged
enum class SyncUniversePhase {
  @EnumTag(1)
  BEFORE_FIRST_SYNC,

  @EnumTag(2)
  AFTER_FIRST_SYNC,
}

@Tagged
data class SyncUniverseImportState(
  @field:Tag(1)
  val patterns: Set<SyncUniverseTargetPattern> = setOf(),

  @field:Tag(2)
  val internalRepos: Set<String> = setOf(),
)

@SealedTagged
sealed interface SyncUniverseTargetPattern {
  @SealedTag(1)
  @Tagged
  data class Include(
    @field:Tag(1)
    val label: Label,
  ) : SyncUniverseTargetPattern

  @SealedTag(2)
  @Tagged
  data class Exclude(
    @field:Tag(1)
    val label: Label,
  ) : SyncUniverseTargetPattern
}
