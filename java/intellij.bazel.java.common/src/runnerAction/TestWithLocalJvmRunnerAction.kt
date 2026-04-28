package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.ExecutableTarget
import org.jetbrains.bsp.protocol.JvmEnvironmentItem

@ApiStatus.Internal
class TestWithLocalJvmRunnerAction(
  project: Project,
  targetInfo: ExecutableTarget,
  executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
  private val callerPsiElement: PsiElement? = null,
) : LocalJvmRunnerAction(
  project = project,
  target = targetInfo,
  configurationName = BazelPluginBundle.message(
    "target.run.with.jvm.runner.action.text",
    targetInfo.id.toShortString(project),
  ),
  executor = executor,
  ) {
  override suspend fun getEnvironment(project: Project): JvmEnvironmentItem? =
    project.service<RunEnvironmentProvider>().getJvmEnvironmentItem(target.id)

  override fun calculateConfiguration(
    configurationName: String,
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: ExecutableTarget,
  ): RunConfiguration? =
    if (callerPsiElement != null) {
      LocalJvmRunnerRunConfigurationProvider.ep.extensionList.firstNotNullOfOrNull {
        it.provideRunConfiguration(
          configurationName = configurationName,
          environment = environment,
          module = module,
          project = project,
          callerPsiElement = callerPsiElement,
        )
      } ?: super.calculateConfiguration(configurationName, environment, module, project, targetInfo)
    } else {
      super.calculateConfiguration(configurationName, environment, module, project, targetInfo)
    }
}

@ApiStatus.Internal
interface LocalJvmRunnerRunConfigurationProvider {
  fun provideRunConfiguration(
    configurationName: String,
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    callerPsiElement: PsiElement,
  ): RunConfiguration?

  companion object {
    val ep = ExtensionPointName.create<LocalJvmRunnerRunConfigurationProvider>("org.jetbrains.bazel.localJvmRunnerRunConfigurationProvider")
  }
}
