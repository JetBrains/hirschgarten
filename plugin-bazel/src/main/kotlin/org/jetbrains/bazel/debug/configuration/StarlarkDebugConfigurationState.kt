package org.jetbrains.bazel.debug.configuration

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import kotlin.io.path.Path

class StarlarkDebugConfigurationState(
  val project: Project,
  val target: String,
  environment: ExecutionEnvironment,
) : CommandLineState(environment) {
  // allows passing execution results (both successful and exceptional) to BspProcessHandler from outside
  val futureProxy = CompletableDeferred<AnalysisDebugResult>()

  override fun startProcess(): BazelProcessHandler =
    BazelProcessHandler(project, futureProxy, Path(".")).apply {
      startNotify()
    }
}
