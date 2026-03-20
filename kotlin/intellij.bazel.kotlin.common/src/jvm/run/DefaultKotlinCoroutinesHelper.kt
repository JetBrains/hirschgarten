package org.jetbrains.bazel.jvm.run

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.debugger.coroutine.DebuggerConnection

internal class DefaultKotlinCoroutinesHelper : KotlinCoroutinesHelper {
  override fun attachCoroutinesDebuggerConnection(project: Project, runConfiguration: RunConfigurationBase<*>) {
    DebuggerConnection(
      project = project,
      configuration = runConfiguration,
      params = JavaParameters(),
      shouldAttachCoroutineAgent = false,
      alwaysShowPanel = true,
    )
  }
}
