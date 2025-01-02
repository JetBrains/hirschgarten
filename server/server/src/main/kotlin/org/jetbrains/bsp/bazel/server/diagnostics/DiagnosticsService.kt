package org.jetbrains.bsp.bazel.server.diagnostics

import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import org.jetbrains.bsp.bazel.server.model.Label
import java.nio.file.Path

class DiagnosticsService(
  workspaceRoot: Path,
  private val parser: DiagnosticsParser = DiagnosticsParserImpl(),
  private val mapper: DiagnosticBspMapper = DiagnosticBspMapper(workspaceRoot),
) {
  fun extractDiagnostics(
    bazelOutput: String,
    targetLabel: Label,
    originId: String?,
    fromProgress: Boolean,
  ): List<PublishDiagnosticsParams> {
    val parsedDiagnostics = parser.parse(bazelOutput, targetLabel, fromProgress)
    val events = mapper.createDiagnostics(parsedDiagnostics, originId)
    return events
  }
}
