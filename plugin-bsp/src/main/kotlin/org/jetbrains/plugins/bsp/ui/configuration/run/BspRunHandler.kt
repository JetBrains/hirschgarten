package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase

public interface BspRunHandler {
  public fun canRun(targets: List<BuildTargetInfo>): Boolean

  public fun canDebug(targets: List<BuildTargetInfo>): Boolean

  public fun prepareRunConfiguration(configuration: BspRunConfigurationBase) {}

  public fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    configuration: BspRunConfigurationBase,
  ): RunProfileState

  public fun getBeforeRunTasks(configuration: BspRunConfigurationBase): List<BeforeRunTask<*>> = emptyList()

  public companion object {
    public val ep: ExtensionPointName<BspRunHandler> =
      ExtensionPointName.create("org.jetbrains.bsp.bspRunHandler")

    public fun getRunHandler(targets: List<BuildTargetInfo>): BspRunHandler =
      ep.extensionList.firstOrNull { it.canRun(targets) } ?: GenericBspRunHandler()
  }
}
