package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.ExecutableTarget
import org.jetbrains.bsp.protocol.JvmEnvironmentItem

@ApiStatus.Internal
class RunWithLocalJvmRunnerAction(
  project: Project,
  targetInfo: ExecutableTarget,
  executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
) : LocalJvmRunnerAction(
  project = project,
  target = targetInfo,
  executor = executor,
  configurationName = BazelPluginBundle.message(
    "target.run.with.jvm.runner.action.text",
    targetInfo.id.toShortString(project),
  ),
) {
  override suspend fun getEnvironment(project: Project): JvmEnvironmentItem? =
    project.service<RunEnvironmentProvider>().getJvmEnvironmentItem(target.id)
}
