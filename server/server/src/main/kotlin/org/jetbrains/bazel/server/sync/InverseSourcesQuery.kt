package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.InverseSourcesResult
import java.nio.file.Path
import kotlin.time.measureTimedValue

object InverseSourcesQuery {
  suspend fun performBenchmarks(
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
    workspaceRoot: Path,
  ) {
    //val benchmarks =
    //  AbuBenchmark
    //    //.dataloreFileSets
    //    .ultimateFileSets
    //    .mapValues {
    //      NewInverseSourcesQuery(workspaceRoot, bazelRunner, workspaceContext)
    //        .performBenchmarks(it.key, it.value)
    //        .also(::printAsCsv)
    //    }
  }

  private fun printAsCsv(results: List<AbuBenchmark>) {
    val csv = AbuBenchmark.toCsv(results)
    println(csv)
  }

  suspend fun inverseSourcesQuery(
    documentRelativePath: Path,
    bazelRunner: BazelRunner,
    bazelInfo: BazelRelease,
    workspaceContext: WorkspaceContext,
    workspaceRoot: Path,
  ): InverseSourcesResult {
    return InverseSourcesResult(emptyMap())
    //val fileLabel =
    //  fileLabel(documentRelativePath, bazelRunner, workspaceContext)
    //    ?: return InverseSourcesResult(
    //      empty(),
    //    )
    //val listOfLabels = targetLabels(fileLabel, bazelRunner, bazelInfo, workspaceContext, workspaceRoot)
    //return InverseSourcesResult(listOfLabels)
  }

  /**
   * @return list of targets that contain `fileLabel` in their `srcs` attribute
   */
  private suspend fun targetLabels(
    fileLabel: String,
    bazelRunner: BazelRunner,
    bazelRelease: BazelRelease,
    workspaceContext: WorkspaceContext,
    workspaceRoot: Path,
  ): List<Label> {
    //val abuFile1 = "/Users/mkocot/Documents/semi/tests/simple-bazel/src/python/PythonPrinter.py"
    //val abuFile2 = "/Users/mkocot/Documents/semi/tests/simple-bazel/src/kotlin/numberer/LargeNumberPrinter.java"
    //val abuFile3 = "/Users/mkocot/Documents/semi/tests/simple-bazel/src/kotlin/numberer/NumberPrinter.java"
    //val aResult = NewInverseSourcesQuery(workspaceRoot, bazelRunner, workspaceContext).doStuff(abuFile1, abuFile2, abuFile3)

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
      measureTimedValue {
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult()
    }.also { println("Good old query took ${it.duration.inWholeMilliseconds}ms") }.value

    if (targetLabelsQuery.isSuccess) {
      return targetLabelsQuery.stdoutLines.mapNotNull { Label.parseOrNull(it) }
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
    val command = bazelRunner.buildBazelCommand(workspaceContext) { fileQuery(listOf(relativePath)) }
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
