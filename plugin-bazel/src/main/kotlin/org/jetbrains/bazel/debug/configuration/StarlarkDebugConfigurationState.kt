package org.jetbrains.bazel.debug.configuration

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import java.util.concurrent.CompletableFuture

class StarlarkDebugConfigurationState(
  val project: Project,
  val target: String,
  environment: ExecutionEnvironment,
) : CommandLineState(environment) {
  // allows passing execution results (both successful and exceptional) to BspProcessHandler from outside
  val futureProxy = CompletableFuture<AnalysisDebugResult>()

  override fun startProcess(): BspProcessHandler<AnalysisDebugResult> =
    BspProcessHandler(futureProxy).apply {
      startNotify()
    }
}
