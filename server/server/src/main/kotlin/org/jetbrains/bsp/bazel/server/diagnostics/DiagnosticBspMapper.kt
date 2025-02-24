package org.jetbrains.bsp.bazel.server.diagnostics

import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import java.nio.file.Path
import java.nio.file.Paths
import org.jetbrains.bsp.protocol.Diagnostic as BspDiagnostic
import org.jetbrains.bsp.protocol.Position as BspPosition
import org.jetbrains.bsp.protocol.Range as BspRange

class DiagnosticBspMapper(private val workspaceRoot: Path) {
  fun createDiagnostics(diagnostics: List<Diagnostic>, originId: String): List<PublishDiagnosticsParams> =
    diagnostics
      .groupBy { Pair(it.fileLocation, it.targetLabel) }
      .map { kv ->
        val bspDiagnostics = kv.value.map { createDiagnostic(it) }
        val doc = TextDocumentIdentifier(toAbsoluteUri(kv.key.first))
        val publishDiagnosticsParams =
          PublishDiagnosticsParams(doc, BuildTargetIdentifier(kv.key.second.toString()), originId = originId, bspDiagnostics, true)
        publishDiagnosticsParams
      }

  private fun createDiagnostic(it: Diagnostic): BspDiagnostic {
    val position = BspPosition(it.position.line - 1, it.position.character - 1)
    val range = BspRange(position, position)
    return BspDiagnostic(range, message = it.message, severity = it.level)
  }

  private fun toAbsoluteUri(rawFileLocation: String): String {
    var path = Paths.get(rawFileLocation)
    if (!path.isAbsolute) {
      path = workspaceRoot.resolve(path)
    }
    return path.toUri().toString()
  }
}
