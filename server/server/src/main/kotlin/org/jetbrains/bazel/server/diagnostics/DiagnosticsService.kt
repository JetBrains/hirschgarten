package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import java.nio.file.Path

class DiagnosticsService(
  workspaceRoot: Path,
  private val parser: DiagnosticsParser = DiagnosticsParserImpl(),
  private val mapper: DiagnosticBspMapper = DiagnosticBspMapper(workspaceRoot),
) {
  fun extractDiagnostics(
    bazelOutput: String,
    targetLabel: Label,
    originId: String,
    isCommandLineFormattedOutput: Boolean = false,
  ): List<PublishDiagnosticsParams> {
    val parsedDiagnostics = parser.parse(bazelOutput, targetLabel, isCommandLineFormattedOutput)
    val events = mapper.createDiagnostics(parsedDiagnostics, originId)
    return events
  }
}
