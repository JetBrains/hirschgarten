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
    bazelOutputLines: List<String>,
    targetLabel: Label,
    originId: String,
    isCommandLineFormattedOutput: Boolean = false,
    onlyFromParsedOutput: Boolean = false,
  ): List<PublishDiagnosticsParams> {
    val parsedDiagnostics = parser.parse(bazelOutputLines, targetLabel, isCommandLineFormattedOutput, onlyFromParsedOutput)
    val events = mapper.createDiagnostics(parsedDiagnostics, originId)
    return events
  }
}
