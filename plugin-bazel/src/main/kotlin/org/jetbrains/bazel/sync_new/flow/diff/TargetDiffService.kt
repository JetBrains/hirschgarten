package org.jetbrains.bazel.sync_new.flow.diff

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.ofLabel
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofSet
import org.jetbrains.bazel.sync_new.flow.ChangedTarget
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.diff.query.QueryTargetPattern
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseTargetPattern
import org.jetbrains.bazel.sync_new.graph.TargetReference
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetGraph
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class TargetDiffService(
  private val project: Project,
) {
  internal val target2Hash: KVStore<Label, TargetData> =
    project.storageContext.createKVStore<Label, TargetData>("bazel.sync.target2Hash", StorageHints.USE_IN_MEMORY)
      .withKeyCodec { ofLabel() }
      .withValueCodec { TargetData.codec }
      .build()

  internal val path2Targets: KVStore<HashValue128, Set<Label>> =
    project.storageContext.createKVStore<HashValue128, Set<Label>>("bazel.sync.path2targets", StorageHints.USE_IN_MEMORY)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofSet(ofLabel()) }
      .build()

  suspend fun computeIncrementalDiff(contributor: TargetHashContributor, graph: BazelTargetGraph): SyncDiff {
    val patterns = QueryTargetPattern.getProjectTargetUniverse(project)
    return computeIncrementalDiff(contributor, patterns, graph, clearHashes = true)
  }

  suspend fun computeIncrementalDiff(
    contributor: TargetHashContributor,
    patterns: List<SyncUniverseTargetPattern>,
    graph: BazelTargetGraph,
    clearHashes: Boolean = false,
  ): SyncDiff {
    val newHashes = contributor.computeHashes(project, patterns)
      .associateBy { it.target }

    val added = hashSetOf<Label>()
    val removed = hashSetOf<Label>()
    val changed = hashSetOf<Label>()

    val allTargets = target2Hash.keys() + newHashes.keys.asSequence()
    for (target in allTargets.toSet()) {
      val oldHash = target2Hash.get(target)
      val newHash = newHashes[target]

      when {
        oldHash == null && newHash != null -> added.add(target)
        oldHash != null && newHash == null -> removed.add(target)
        oldHash != newHash -> changed.add(target)
        else -> {}
      }
    }

    if (clearHashes) {
      clearTargets()
    } else {
      removed.forEach { target2Hash.get(it)?.let { data -> removeTarget(it, data.path) } }
    }

    newHashes
      .filter { (target, _) -> target !in removed }
      .forEach { (_, hash) -> addTarget(hash) }

    return SyncDiff(
      added = added.map { TargetReference.ofGraphLazy(it, graph) }.toSet(),
      removed = removed.map { TargetReference.ofGraphNow(it, graph) }.toSet(),
      changed = changed.map {
        ChangedTarget(
          label = it,
          old = TargetReference.ofGraphNow(it, graph),
          new = TargetReference.ofGraphLazy(it, graph),
        )
      }.toSet(),
    )
  }

  suspend fun computeFreshDiff(contributor: TargetHashContributor, graph: BazelTargetGraph): SyncDiff {
    val patterns = QueryTargetPattern.getProjectTargetUniverse(project)
    val newHashes = contributor.computeHashes(project, patterns)

    clearTargets()
    newHashes.forEach { addTarget(it) }

    return SyncDiff(
      added = newHashes.map { TargetReference.ofGraphLazy(it.target, graph) }.toSet(),
      removed = emptySet(),
      changed = emptySet(),
    )
  }

  fun clear() {
    target2Hash.clear()
  }

  private fun addTarget(target: TargetHash) {
    target2Hash.set(target.target, TargetData.ofTargetHash(target))
    if (target.path != null) {
      path2Targets.compute(hash { putString(target.path) }) { _, v ->
        if (v == null) {
          setOf(target.target)
        } else {
          v + target.target
        }
      }
    }
  }

  private fun removeTarget(label: Label, path: String?) {
    target2Hash.remove(label)
    if (path != null) {
      path2Targets.compute(hash { putString(path) }) { _, v ->
        if (v == null) {
          null
        } else {
          v - label
        }
      }
    }
  }

  private fun clearTargets() {
    target2Hash.clear()
    path2Targets.clear()
  }
}
