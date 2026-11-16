package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.commons.BazelHost
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

class DiagnosticsService(
  private val workspaceRoot: Path
) {
  private val parser = DiagnosticsParserImpl()
  private val host = BazelHost.of(workspaceRoot)

  /**
   * Returns the IDE path of a file that a compiler names in its output.
   * A compiler in another environment than the IDE, for example in WSL, prints an absolute path of that environment.
   * The parsers read `/home/user/A.java` as a path without a drive on Windows, and this function gives it the root of the host.
   */
  private fun resolveFileLocation(location: Path): Path =
    if (!host.isLocal && !location.isAbsolute && location.root != null) {
      host.toIdePathOrNull(location.invariantSeparatorsPathString) ?: workspaceRoot.resolve(location)
    }
    else {
      workspaceRoot.resolve(location)
    }

  fun extractDiagnostics(
    bazelOutputLines: List<String>,
    targetLabel: Label,
    taskId: TaskId,
    isCommandLineFormattedOutput: Boolean = false,
    onlyFromParsedOutput: Boolean = false,
  ): List<PublishDiagnosticsParams> {
    val parsedDiagnostics = parser.parse(bazelOutputLines, targetLabel, isCommandLineFormattedOutput, onlyFromParsedOutput)
    return mapDiagnostics(parsedDiagnostics, taskId)
  }

  private fun mapDiagnostics(diagnostics: List<Diagnostic>, taskId: TaskId): List<PublishDiagnosticsParams> =
    diagnostics
      .groupBy { diagnostic ->
        val path = diagnostic.fileLocation?.let { resolveFileLocation(it) }
        Pair(path, diagnostic.targetLabel)
      }.map { kv ->
        val diagnostics = kv.value.map { it.copy(fileLocation = kv.key.first) }
        val doc = kv.key.first?.let { TextDocumentIdentifier(it) }
        PublishDiagnosticsParams(taskId, doc, kv.key.second, diagnostics, true)
      }
}
