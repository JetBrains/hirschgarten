package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

class DiagnosticsService internal constructor(
  private val parser: DiagnosticsParser,
  private val mapper: DiagnosticBspMapper,
) {
  constructor(workspaceRoot: Path) : this(DiagnosticsParserImpl(), DiagnosticBspMapper(workspaceRoot))

  fun extractDiagnostics(
    bazelOutputLines: List<String>,
    targetLabel: Label,
    taskId: TaskId,
    isCommandLineFormattedOutput: Boolean = false,
    onlyFromParsedOutput: Boolean = false,
  ): List<PublishDiagnosticsParams> {
    val parsedDiagnostics = parser.parse(bazelOutputLines, targetLabel, isCommandLineFormattedOutput, onlyFromParsedOutput)
    val events = mapper.createDiagnostics(parsedDiagnostics, taskId)
    return events
  }
}
