package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import java.nio.file.Path

class DiagnosticsService(
  private val workspaceRoot: Path
) {
  private val parser = DiagnosticsParserImpl()

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
        val path = diagnostic.fileLocation?.let { workspaceRoot.resolve(it) }
        Pair(path, diagnostic.targetLabel)
      }.map { kv ->
        val diagnostics = kv.value.map { it.copy(fileLocation = kv.key.first) }
        val doc = kv.key.first?.let { TextDocumentIdentifier(it) }
        PublishDiagnosticsParams(taskId, doc, kv.key.second, diagnostics, true)
      }

}
