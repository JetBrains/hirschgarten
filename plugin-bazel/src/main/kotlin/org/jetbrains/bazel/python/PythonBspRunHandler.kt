package org.jetbrains.bazel.python

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.BspRunCommandLineState
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import java.util.UUID

class PythonBspRunHandler : BspRunHandler {
  override val name: String = "Python Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
      PythonDebugCommandLineState(environment, originId, state)
    } else {
      BspRunCommandLineState(environment, originId, state)
    }
  }

  class Provider : RunHandlerProvider {
    override val id: String = "PythonBspRunHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = PythonBspRunHandler()

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      targetInfos.all {
        it.languageIds.contains("python") && it.capabilities.canRun
      }

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
  }
}
