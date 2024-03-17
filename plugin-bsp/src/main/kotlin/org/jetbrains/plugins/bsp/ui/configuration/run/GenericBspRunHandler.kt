package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo

public open class GenericBspRunHandler : BspRunHandler {
  override fun canRun(target: BuildTargetInfo): Boolean = true

  override fun canDebug(target: BuildTargetInfo): Boolean = false

  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    target: BuildTargetInfo,
  ): RunProfileState = GenericBspRunHandlerState(project, environment, target.id)
}
