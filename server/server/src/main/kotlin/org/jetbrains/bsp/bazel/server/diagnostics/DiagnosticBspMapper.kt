package org.jetbrains.bsp.bazel.server.diagnostics

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import java.nio.file.Path
import java.nio.file.Paths
import ch.epfl.scala.bsp4j.Diagnostic as BspDiagnostic
import ch.epfl.scala.bsp4j.Position as BspPosition
import ch.epfl.scala.bsp4j.Range as BspRange

class DiagnosticBspMapper(private val workspaceRoot: Path) {
  fun createDiagnostics(diagnostics: List<Diagnostic>, originId: String?): List<PublishDiagnosticsParams> =
    diagnostics
      .groupBy { Pair(it.fileLocation, it.targetLabel) }
      .map { kv ->
        val bspDiagnostics = kv.value.map { createDiagnostic(it) }
        val doc = TextDocumentIdentifier(toAbsoluteUri(kv.key.first))
        val publishDiagnosticsParams =
          PublishDiagnosticsParams(doc, BuildTargetIdentifier(kv.key.second.toString()), bspDiagnostics, true)
        publishDiagnosticsParams.originId = originId
        publishDiagnosticsParams
      }

  private fun createDiagnostic(it: Diagnostic): BspDiagnostic {
    val position = BspPosition(it.position.line - 1, it.position.character - 1)
    val range = BspRange(position, position)
    return BspDiagnostic(range, it.message).apply {
      severity = it.level
    }
  }

  private fun toAbsoluteUri(rawFileLocation: String): String {
    var path = Paths.get(rawFileLocation)
    if (!path.isAbsolute) {
      path = workspaceRoot.resolve(path)
    }
    return path.toUri().toString()
  }
}
