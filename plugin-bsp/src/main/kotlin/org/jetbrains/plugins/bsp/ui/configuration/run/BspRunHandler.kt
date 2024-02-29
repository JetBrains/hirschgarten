package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo

public interface BspRunHandler {
  public fun canRun(target: BuildTargetInfo): Boolean

  public fun canDebug(target: BuildTargetInfo): Boolean

  public fun prepareRunConfiguration(configuration: BspRunConfiguration) {}

  public fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    target: BuildTargetInfo,
  ): RunProfileState

  public fun getBeforeRunTasks(configuration: BspRunConfiguration): List<BeforeRunTask<*>> = emptyList()

  public companion object {
    public val ep: ExtensionPointName<BspRunHandler> =
      ExtensionPointName.create("org.jetbrains.bsp.bspRunHandler")

    public fun getRunHandler(target: BuildTargetInfo): BspRunHandler =
      ep.extensionList.firstOrNull { it.canRun(target) } ?: GenericBspRunHandler()
  }
}
