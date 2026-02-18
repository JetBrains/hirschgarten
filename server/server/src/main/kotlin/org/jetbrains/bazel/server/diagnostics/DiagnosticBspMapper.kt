package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import java.nio.file.Path
import org.jetbrains.bsp.protocol.Diagnostic as BspDiagnostic
import org.jetbrains.bsp.protocol.Position as BspPosition
import org.jetbrains.bsp.protocol.Range as BspRange

class DiagnosticBspMapper(private val workspaceRoot: Path) {
  fun createDiagnostics(diagnostics: List<Diagnostic>, taskId: TaskId): List<PublishDiagnosticsParams> =
    diagnostics
      .groupBy {
        val path = it.fileLocation?.let { workspaceRoot.resolve(it) }
        Pair(path, it.targetLabel)
      }.map { kv ->
        val bspDiagnostics = kv.value.map { createDiagnostic(it) }
        val doc = kv.key.first?.let { TextDocumentIdentifier(it) }
        val publishDiagnosticsParams =
          PublishDiagnosticsParams(taskId, doc, kv.key.second, bspDiagnostics, true)
        publishDiagnosticsParams
      }

  private fun createDiagnostic(it: Diagnostic): BspDiagnostic {
    val position = BspPosition(it.position.line - 1, it.position.character - 1)
    val range = BspRange(position, position)
    return BspDiagnostic(range, message = it.message, severity = it.level)
  }
}
