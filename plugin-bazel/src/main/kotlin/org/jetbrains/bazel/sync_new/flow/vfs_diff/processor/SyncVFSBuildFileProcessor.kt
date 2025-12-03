package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.vfs_diff.WildcardFileDiff
import org.jetbrains.bazel.sync_new.storage.put
import org.jetbrains.bazel.sync_new.storage.remove
import org.jetbrains.bazel.sync_new.storage.set
import java.nio.file.Path

class SyncVFSBuildFileProcessor {
  suspend fun process(ctx: SyncVFSContext, diff: WildcardFileDiff<SyncVFSFile.BuildFile>): SyncColdDiff {
    // TODO: optimize query by finding relatively shallow dominators

    val addedBuildFiles = diff.added.map { it.path }
    val changedBuildFiles = diff.changed.map { it.path }
    val buildLabels = SyncVFSLabelResolver.resolveSourceFileLabels(
      ctx = ctx,
      sources = addedBuildFiles + changedBuildFiles,
    )

    val connector = ctx.project.service<BazelConnectorService>()
      .ofLegacyTask()

    val targets = if (buildLabels.isEmpty()) {
      emptyMap()
    } else {
      // TODO: replace with QueryBuilder
      // TODO: consistent labels
      // transform //pkg:BUILD.bazel -> //pkg:*
      val query = buildLabels
        .flatMap { it.value }
        .map { it.assumeResolved().copy(target = AllRuleTargets) }
        .joinToString(separator = " + ") { it.toString() }
      val result = connector.query {
        defaults()
        keepGoing()
        output(QueryOutput.PROTO)
        query(query)
      }
      result.unwrap().unwrapProtos()
        .filter { it.hasRule() }
        .map { it.rule }
        .groupBy { it.getBuildLocation(ctx) }
    }

    val addedTargets = mutableSetOf<Label>()
    val changedTargets = mutableSetOf<Label>()
    val removedTargets = mutableSetOf<Label>()

    // remove all targets belonging to specific BUILD file
    for (file in diff.removed) {
      val removed = ctx.storage.build2Targets.remove(file.path) ?: continue
      for (label in removed) {
        ctx.storage.target2Build.remove(label)
      }
      removedTargets += removed
    }

    // check for changed between previous state
    for (changed in diff.changed) {
      val newTargets = targets[changed.path]
        ?.map { Label.parse(it.name) }
        ?.toSet() ?: emptySet()
      val oldTargets = ctx.storage.build2Targets[changed.path]
        ?.toSet() ?: emptySet()

      for (target in newTargets + oldTargets) {
        when {
          target in oldTargets && target !in newTargets -> {
            removedTargets.add(target)
            ctx.storage.target2Build.remove(target)
            ctx.storage.build2Targets.remove(changed.path, target)
          }
          target !in oldTargets && target in newTargets -> {
            addedTargets.add(target)
            ctx.storage.target2Build.put(target, changed.path)
            ctx.storage.build2Targets.put(changed.path, target)
          }
          else -> changedTargets.add(target)
        }
      }
    }

    for (added in diff.added) {
      val newTargets = targets[added.path]?.map { Label.parse(it.name) } ?: continue
      for (target in newTargets) {
        ctx.storage.target2Build[target] = added.path
        ctx.storage.build2Targets.put(added.path, target)
        addedTargets += target
      }
    }

    return SyncColdDiff(
      added = addedTargets,
      removed = removedTargets,
      changed = changedTargets,
    )
  }

  private fun Build.Rule.getBuildLocation(ctx: SyncVFSContext): Path {
    val path = if (location.contains(':')) {
      Path.of(location.substringBefore(':'))
    } else {
      Path.of(location)
    }
    return WorkspacePathUtils.resolveExternalRepoPath(ctx, path)
  }


}
