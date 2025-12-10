package org.jetbrains.bazel.sync_new.flow.universe

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.flow.DisabledSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping
import java.nio.file.Path

@Tagged
@ClassTag(1683599960)
data class SyncUniverseState(
  @field:Tag(1)
  val phase: SyncUniversePhase,
  @field:Tag(2)
  val importState: SyncUniverseImportState = SyncUniverseImportState(),
  @field:Tag(3)
  val repoMapping: SyncRepoMapping = DisabledSyncRepoMapping,
)

@EnumTagged
@ClassTag(1675775949)
enum class SyncUniversePhase {
  @EnumTag(1)
  BEFORE_FIRST_SYNC,

  @EnumTag(2)
  AFTER_FIRST_SYNC,
}

@Tagged
@ClassTag(740253041)
data class SyncUniverseImportState(
  @field:Tag(1)
  val patterns: Set<SyncUniverseFilteredCondition<Label>> = setOf(),

  @field:Tag(10)
  val directories: Set<SyncUniverseFilteredCondition<Path>> = setOf(),

  @field:Tag(2)
  val internalRepos: Set<String> = setOf(),
)

@SealedTagged
@ClassTag(1218923988)
sealed interface SyncUniverseFilteredCondition<T> {
  @SealedTag(1)
  @Tagged
  @ClassTag(1755225442)
  data class Include<T>(
    @field:Tag(1)
    val element: T,
  ) : SyncUniverseFilteredCondition<T>

  @SealedTag(2)
  @Tagged
  @ClassTag(353835244)
  data class Exclude<T>(
    @field:Tag(1)
    val element: T,
  ) : SyncUniverseFilteredCondition<T>
}
