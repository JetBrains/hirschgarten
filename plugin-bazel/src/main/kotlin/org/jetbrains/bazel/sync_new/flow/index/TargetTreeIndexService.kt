package org.jetbrains.bazel.sync_new.flow.index

import com.dynatrace.hash4j.hashing.HashValue128
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.SynchronizedClearableLazy
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetTag
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdater
import org.jetbrains.bazel.sync_new.index.impl.createOne2OneIndex
import org.jetbrains.bazel.sync_new.index.syncIndexService
import org.jetbrains.bazel.sync_new.storage.DefaultStorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.util.buildEnumSet
import org.jetbrains.bazel.target.targetUtils
import java.util.EnumSet

@Service(Service.Level.PROJECT)
class TargetTreeIndexService(
  private val project: Project,
) : SyncIndexUpdater {
  private val targetTreeIndex = project.syncIndexService.createOne2OneIndex("target2TargetTreeEntry") { name, ctx ->
    ctx.createKVStore<HashValue128, TargetTreeEntry>(name, DefaultStorageHints.USE_IN_MEMORY)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofKryo() }
      .build()
  }

  private val targetEntriesCached = SynchronizedClearableLazy {
    targetTreeIndex.values.toList()
  }

  override suspend fun updateIndexes(ctx: SyncContext, diff: SyncDiff) {
    val (added, removed) = diff.split
    for (removed in removed) {
      targetTreeIndex.invalidate(hash(removed.label))
    }
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      if (!target.genericData.isUniverseTarget) {
        continue
      }
      val entry = TargetTreeEntry(
        label = target.label,
        name = target.label.toString(),
        flags = buildEnumSet {
          val tags = target.genericData.tags
          when {
            BazelTargetTag.TEST in tags -> add(TargetTreeFlags.TESTABLE)
            BazelTargetTag.EXECUTABLE in tags -> add(TargetTreeFlags.RUNNABLE)
            BazelTargetTag.NO_BUILD in tags -> add(TargetTreeFlags.NO_BUILD)
          }
        },
      )
      targetTreeIndex.set(hash(target.label), entry)
    }

    targetEntriesCached.drop()
    project.targetUtils.emitTargetListUpdate()
  }

  fun getTargetTreeEntriesCached(): Sequence<TargetTreeEntry> = targetEntriesCached.value.asSequence()

}

@Tagged
@ClassTag(583464019)
data class TargetTreeEntry(
  @field:Tag(1)
  val label: Label,

  @field:Tag(2)
  val name: String,

  @field:Tag(3)
  val flags: EnumSet<TargetTreeFlags>,
)

@EnumTagged
@ClassTag(356819182)
enum class TargetTreeFlags {
  @EnumTag(1)
  RUNNABLE,

  @EnumTag(2)
  TESTABLE,

  @EnumTag(3)
  NO_BUILD
}



