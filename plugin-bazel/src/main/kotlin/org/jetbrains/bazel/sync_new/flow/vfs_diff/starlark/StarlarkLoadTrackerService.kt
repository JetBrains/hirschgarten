package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseService
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseState
import org.jetbrains.bazel.sync_new.flow.universe.isInsideUniverse
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile

@Service(Service.Level.PROJECT)
class StarlarkLoadTrackerService(
  val project: Project,
) {
  val starlarkLoadGraph: StarlarkLoadGraph = StarlarkLoadGraph(project)
  val starlarkParser: StarlarkFileParser = StarlarkFileParser(project)

  // TODO: correctly handle removed BUILD files
  suspend fun computeStarlarkDiffFromUniverseDiff(ctx: SyncVFSContext): SyncFileDiff {
    if (ctx.scope.isFullSync || ctx.isFirstSync) {
      starlarkLoadGraph.graph.clear()
    }
    if (!ctx.universeDiff.hasChanged) {
      return SyncFileDiff()
    }
    val connector = project.service<BazelConnectorService>().ofLegacyTask()
    val universeState = project.service<SyncUniverseService>().universe
    val universeExpr = (ctx.universeDiff.added + ctx.universeDiff.removed)
      .joinToString(separator = " + ") { it.toString() }
    val result = connector.query {
      defaults()
      keepGoing()
      output(QueryOutput.PROTO)
      //consistentLabels()
      query("buildfiles($universeExpr) union loadfiles($universeExpr)")
    }
    val files = result.unwrap().unwrapProtos()
      .asSequence()
      .filter { it.hasSourceFile() }
      .map { it.sourceFile }
      .toList()
    return toDiff(ctx, universeState, files)
  }

  private fun toDiff(ctx: SyncVFSContext, universe: SyncUniverseState, files: List<Build.SourceFile>): SyncFileDiff {
    val added = mutableListOf<SyncVFSFile>()
    val legacyRepoMapping = LegacyBazelFrontendBridge.toLegacyRepoMapping(ctx.repoMapping)
    for (file in files) {
      val label = Label.parse(file.name)
      if (!label.isInsideUniverse(universe)) {
        continue
      }
      val path = ctx.pathsResolver.toFilePath(label.assumeResolved(), legacyRepoMapping)
      when {
        label.targetName == "BUILD" || label.targetName == "BUILD.bazel" -> {
          added += SyncVFSFile.BuildFile(path)
        }

        label.targetName.endsWith(".bzl") -> {
          added += SyncVFSFile.StarlarkFile(path)
        }
      }
    }
    return SyncFileDiff(
      added = added,
    )
  }
}
