package org.jetbrains.bazel.sync_new.flow.diff.vfs.processor

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.diff.vfs.WildcardFileDiff
import java.nio.file.Path

class SyncVFSBuildFileProcessor {
  suspend fun process(ctx: SyncVFSContext, diff: WildcardFileDiff<SyncVFSFile.BuildFile>): SyncColdDiff {
    // TODO: optimize query by finding relatively shallow dominators
    val addedBuildFiles = mutableSetOf<Path>()
    val changedBuildFiles = mutableSetOf<Path>()

    val buildLabels = mutableSetOf<Label>()
    for (added in diff.added) {
      addedBuildFiles.add(added.path)
      buildLabels += SyncVFSLabelResolver.resolveFullLabel(ctx, added.path) ?: continue
    }
    for (changed in diff.changed) {
      changedBuildFiles.add(changed.path)
      buildLabels += SyncVFSLabelResolver.resolveFullLabel(ctx, changed.path) ?: continue
    }

    val connector = ctx.project.service<BazelConnectorService>()
      .ofLegacyTask()

    // TODO: replace with QueryBuilder
    val query = buildLabels.joinToString(separator = " union ") { it.toString() }
    val result = connector.query {
      defaults()
      keepGoing()
      output(QueryOutput.PROTO)
      query(query)
    }
    val targets = result.unwrap().unwrapProtos()

    val removedTargets = mutableSetOf<Label>()
    for (file in diff.removed) {
      removedTargets += ctx.storage.build2Targets.remove(file.path) ?: continue
    }

    val addedTargets = mutableSetOf<Label>()
    val changedTargets = mutableSetOf<Label>()

    for (target in targets) {
      val rule = target.rule ?: continue
      val location = rule.getBuildLocation() ?: continue
      val label = Label.parse(rule.name)
      ctx.storage.target2Build.set(label, location)
      when {
        location in addedBuildFiles -> addedTargets.add(label)
        location in changedBuildFiles -> changedTargets.add(label)
      }
    }

    return SyncColdDiff(
      added = addedTargets,
      removed = removedTargets,
      changed = changedTargets
    )
  }

  private fun Build.Rule.getBuildLocation(): Path? {
    val splits = location.split(':')
    return when {
      splits.size == 3 -> Path.of(splits[0])
      else -> null
    }
  }


}
