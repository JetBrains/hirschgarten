package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfiguration
import java.util.UUID

public open class GenericBspRunHandler : BspRunHandler {
  override fun canRun(targets: List<BuildTargetInfo>): Boolean = targets.all { it.capabilities.canRun }

  override fun canDebug(targets: List<BuildTargetInfo>): Boolean = false

  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    configuration: BspRunConfigurationBase,
  ): RunProfileState =
    when (configuration) {
      is BspTestConfiguration -> {
        thisLogger().warn("Using generic test handler for ${configuration.name}")
        BspTestCommandLineState(project, environment, configuration, UUID.randomUUID().toString())
      }

      is BspRunConfiguration -> {
        thisLogger().warn("Using generic run handler for ${configuration.name}")
        BspRunCommandLineState(project, environment, configuration, UUID.randomUUID().toString())
      }

      else -> {
        throw IllegalArgumentException("GenericBspRunHandler can only handle BspRunConfiguration")
      }
    }
}
