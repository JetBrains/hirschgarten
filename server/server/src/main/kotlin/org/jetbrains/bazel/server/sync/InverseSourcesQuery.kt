package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bazel.server.bzlmod.canonicalize
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.InverseSourcesResult
import java.nio.file.Path

object InverseSourcesQuery {
  suspend fun inverseSourcesQuery(
    documentRelativePath: Path,
    bazelRunner: BazelRunner,
    bazelInfo: BazelRelease,
    workspaceContext: WorkspaceContext,
  ): InverseSourcesResult {
    val fileLabel =
      fileLabel(documentRelativePath, bazelRunner, workspaceContext)
        ?: return InverseSourcesResult(
          emptyList(),
        )
    val listOfLabels = targetLabels(fileLabel, bazelRunner, bazelInfo, workspaceContext)
    return InverseSourcesResult(listOfLabels)
  }

  /**
   * @return list of targets that contain `fileLabel` in their `srcs` attribute
   */
  private suspend fun targetLabels(
    fileLabel: String,
    bazelRunner: BazelRunner,
    bazelRelease: BazelRelease,
    workspaceContext: WorkspaceContext,
  ): List<CanonicalLabel> {
    val packageLabel = fileLabel.replace(":.*".toRegex(), ":*")
    val consistentLabelsArg = listOfNotNull(if (bazelRelease.major >= 6) "--consistent_labels" else null) // #bazel5
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        query {
          options.addAll(consistentLabelsArg)
          options.add("attr('srcs', $fileLabel, $packageLabel)")
        }
      }
    val targetLabelsQuery =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult()
    if (targetLabelsQuery.isSuccess) {
      return targetLabelsQuery.stdoutLines
        .mapNotNull {
          Label.parseOrNull(it)?.canonicalize(RepoMappingDisabled) // TODO: use repo mapping}
        }.distinct()
    } else {
      error("Could not retrieve inverse sources")
    }
  }

  /**
   * @return Bazel label corresponding to file path
   *
   * For example when you pass a relative path like "foo/Lib.kt", you will receive "//foo:Lib.kt".
   * Null is returned in case the file does not exist or does not belong to any target's sources list
   */
  private suspend fun fileLabel(
    relativePath: Path,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): String? {
    val command = bazelRunner.buildBazelCommand(workspaceContext) { fileQuery(relativePath) }
    val fileLabelResult =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult()
    return if (fileLabelResult.isSuccess) {
      fileLabelResult.stdoutLines.last()
    } else if (fileLabelResult.stderrLines.firstOrNull()?.startsWith("ERROR: no such target '") == true) {
      null
    } else {
      throw RuntimeException(
        "Could not find file. Bazel query failed:\n command:\n${command.buildExecutionDescriptor().command}\nstderr:\n${fileLabelResult.stderr}\nstdout:\n${fileLabelResult.stdout}",
      )
    }
  }
}
