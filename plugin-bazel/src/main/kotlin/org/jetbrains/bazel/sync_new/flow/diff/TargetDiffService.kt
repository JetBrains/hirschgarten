package org.jetbrains.bazel.sync_new.flow.diff

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofLabel
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.TargetReference
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetGraph
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class TargetDiffService(
  private val project: Project,
) {
  private val targetHashes: KVStore<Label, HashValue128> =
    project.storageContext.createKVStore<Label, HashValue128>("bazel.sync.targetHashes", StorageHints.USE_IN_MEMORY)
      .withKeyCodec { ofLabel() }
      .withValueCodec { ofHash128() }
      .build()

  suspend fun computeIncrementalDiff(contributor: TargetHashContributor, graph: BazelTargetGraph): SyncDiff {
    val patterns = computeProjectTargetPatterns(project)
    return computeIncrementalDiff(contributor, patterns, graph, clearHashes = true)
  }

  suspend fun computeIncrementalDiff(
    contributor: TargetHashContributor,
    patterns: List<TargetPattern>,
    graph: BazelTargetGraph,
    clearHashes: Boolean = false,
  ): SyncDiff {
    val newHashes = contributor.computeHashes(project, patterns)
      .associate { (target, hash) -> target to hash }

    val added = hashSetOf<Label>()
    val removed = hashSetOf<Label>()
    val changed = hashSetOf<Label>()

    val allTargets = targetHashes.keys().filter { it in newHashes.keys } + newHashes.keys.asSequence()
    for (target in allTargets) {
      val oldHash = targetHashes.get(target)
      val newHash = newHashes[target]

      when {
        oldHash == null && newHash != null -> added.add(target)
        oldHash != null && newHash == null -> removed.add(target)
        oldHash != newHash -> changed.add(target)
        else -> {}
      }
    }

    if (clearHashes) {
      targetHashes.clear()
    } else {
      removed.forEach { targetHashes.remove(it) }
    }

    // TODO: optimize - bulk update
    newHashes
      .filter { (target, _) -> target !in removed }
      .forEach { (target, hash) -> targetHashes.set(target, hash) }

    return SyncDiff(
      added = added.toTargetRefs(graph),
      removed = removed.toTargetRefs(graph),
      changed = changed.toTargetRefs(graph),
    )
  }

  suspend fun computeFreshDiff(contributor: TargetHashContributor, graph: BazelTargetGraph): SyncDiff {
    val patterns = computeProjectTargetPatterns(project)
    val newHashes = contributor.computeHashes(project, patterns)

    targetHashes.clear()
    for ((label, hash) in newHashes) {
      targetHashes.set(label, hash)
    }

    return SyncDiff(
      added = newHashes.map { it.target }
        .toList()
        .toTargetRefs(graph),
      removed = emptySet(),
      changed = emptySet(),
    )
  }

  fun clear() {
    targetHashes.clear()
  }

  private fun Iterable<Label>.toTargetRefs(graph: BazelTargetGraph) =
    map { TargetReference.ofGraph(it, { graph }) }.toSet()

  private suspend fun computeProjectTargetPatterns(project: Project): List<TargetPattern> {
    val workspaceContext = LegacyBazelFrontendBridge.fetchWorkspaceContext(project)
    return workspaceContext.targets.map {
      when (it) {
        is ExcludableValue.Excluded<Label> -> TargetPattern.Exclude(it.value)
        is ExcludableValue.Included<Label> -> TargetPattern.Include(it.value)
      }
    }
  }
}
