package org.jetbrains.bazel.sync_new.flow.hash_diff

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.consistentLabels
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncDiffFlags
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.set
import org.jetbrains.bazel.sync_new.util.iterator

class SyncHasherProcessor {
  suspend fun process(ctx: SyncHasherContext, diff: SyncColdDiff): SyncColdDiff {
    val target2Hash = ctx.service.target2Hash

    val newTarget2Hash = computeTargetHashes(ctx, diff.added + diff.changed)
    for (added in diff.added) {
      target2Hash[hash(added)] = newTarget2Hash[added] ?: continue
    }

    val newChanged = mutableSetOf<Label>()
    for (changed in diff.changed) {
      val oldHash = target2Hash[hash(changed)]
      if (oldHash == null) {
        newChanged.add(changed)
        continue
      }
      val newHash = newTarget2Hash[changed] ?: continue
      if (oldHash != newHash) {
        newChanged.add(changed)
      }
    }

    for (changed in diff.changed) {
      target2Hash[hash(changed)] = newTarget2Hash[changed] ?: continue
    }

    for (removed in diff.removed) {
      target2Hash.remove(hash(removed))
    }

    for ((target, flags) in diff.flags) {
      if (flags.contains(SyncDiffFlags.FORCE_INVALIDATION)) {
        newChanged += target
      }
    }

    return SyncColdDiff(
      added = diff.added,
      changed = newChanged,
      removed = diff.removed
    )

  }

  private suspend fun computeTargetHashes(ctx: SyncHasherContext, targets: Collection<Label>): Map<Label, HashValue128> {
    if (targets.isEmpty()) {
      return emptyMap()
    }
    val connector = ctx.project.service<BazelConnectorService>().ofLegacyTask()
    val query = targets.joinToString(separator = " + ") { it.toString() }
    val result = connector.query {
      defaults()
      keepGoing()
      consistentLabels()
      output(QueryOutput.PROTO)
      query(query)
    }
    // TODO: maybe parallelize
    return result.unwrap().unwrapProtos()
      .filter { it.hasRule() }
      .map { it.rule }
      .associate { Label.parse(it.name) to BuildRuleProtoHasher.hash(it) }
  }
}
