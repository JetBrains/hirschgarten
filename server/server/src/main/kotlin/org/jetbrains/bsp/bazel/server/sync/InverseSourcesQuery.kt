package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.StatusCode
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.server.model.Label
import java.nio.file.Path

object InverseSourcesQuery {
  fun inverseSourcesQuery(
    documentRelativePath: Path,
    bazelRunner: BazelRunner,
    bazelInfo: BazelRelease,
    cancelChecker: CancelChecker,
  ): InverseSourcesResult {
    val fileLabel =
      fileLabel(documentRelativePath, bazelRunner, cancelChecker)
        ?: return InverseSourcesResult(
          emptyList(),
        )
    val listOfLabels = targetLabels(fileLabel, bazelRunner, bazelInfo, cancelChecker)
    return InverseSourcesResult(listOfLabels.map { BuildTargetIdentifier(it.toString()) })
  }

  /**
   * @return list of targets that contain `fileLabel` in their `srcs` attribute
   */
  private fun targetLabels(
    fileLabel: String,
    bazelRunner: BazelRunner,
    bazelRelease: BazelRelease,
    cancelChecker: CancelChecker,
  ): List<Label> {
    val packageLabel = fileLabel.replace(":.*".toRegex(), ":*")
    val consistentLabelsArg = listOfNotNull(if (bazelRelease.major >= 6) "--consistent_labels" else null) // #bazel5
    val command =
      bazelRunner.buildBazelCommand {
        query {
          options.addAll(consistentLabelsArg)
          options.add("attr('srcs', $fileLabel, $packageLabel)")
        }
      }
    val targetLabelsQuery =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult(cancelChecker)
    if (targetLabelsQuery.statusCode == StatusCode.OK) {
      return targetLabelsQuery.stdoutLines.map { Label.parse(it) }
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
  private fun fileLabel(
    relativePath: Path,
    bazelRunner: BazelRunner,
    cancelChecker: CancelChecker,
  ): String? {
    val command =
      bazelRunner.buildBazelCommand {
        query {
          targets.add(BuildTargetIdentifier(relativePath.toString()))
        }
      }
    val fileLabelResult =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult(cancelChecker)
    return if (fileLabelResult.statusCode == StatusCode.OK && fileLabelResult.stdoutLines.size == 1) {
      fileLabelResult.stdoutLines.first()
    } else if (fileLabelResult.stderrLines.firstOrNull()?.startsWith("ERROR: no such target '") == true) {
      null
    } else {
      throw RuntimeException("Could not find file. Bazel query failed:\n ${fileLabelResult.stderrLines.joinToString { "\n" }}\n")
    }
  }
}
