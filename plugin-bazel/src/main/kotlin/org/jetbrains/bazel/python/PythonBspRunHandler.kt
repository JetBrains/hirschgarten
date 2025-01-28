package org.jetbrains.bazel.python

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.commandLine.BspRunCommandLineState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import java.util.UUID

class PythonBspRunHandler(configuration: BspRunConfiguration) : BspRunHandler {
  private val buildToolName = configuration.project.assets.presentableName
  override val name: String = "Python $buildToolName Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
      PythonDebugCommandLineState(environment, originId, state)
    } else {
      BspRunCommandLineState(environment, originId, state)
    }
  }

  class Provider : BspRunHandlerProvider {
    override val id: String = "PythonBspRunHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = PythonBspRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      targetInfos.all {
        it.languageIds.contains("python") && it.capabilities.canRun
      }

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
  }
}
