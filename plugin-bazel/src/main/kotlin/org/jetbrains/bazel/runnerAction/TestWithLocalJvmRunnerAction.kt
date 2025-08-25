package org.jetbrains.bazel.runnerAction

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams

class TestWithLocalJvmRunnerAction(
  project: Project,
  targetInfo: BuildTarget,
  text: (() -> String)? = null,
  isDebugMode: Boolean = false,
  includeTargetNameInText: Boolean = false,
  private val callerPsiElement: PsiElement? = null,
) : LocalJvmRunnerAction(
    targetInfo = targetInfo,
    text = {
      if (text != null) {
        text()
      } else if (isDebugMode) {
        BazelPluginBundle.message(
          "target.debug.with.jvm.runner.action.text",
          if (includeTargetNameInText) targetInfo.id.toShortString(project) else "",
        )
      } else {
        BazelPluginBundle.message(
          "target.test.with.jvm.runner.action.text",
          if (includeTargetNameInText) targetInfo.id.toShortString(project) else "",
        )
      }
    },
    isDebugMode = isDebugMode,
  ) {
  override suspend fun getEnvironment(project: Project): JvmEnvironmentItem? {
    val params = createJvmTestEnvironmentParams(targetInfo.id)
    return BazelWorkspaceResolveService
      .getInstance(project)
      .withEndpointProxy { it.jvmTestEnvironment(params) }
      .items
      .firstOrNull()
  }

  private fun createJvmTestEnvironmentParams(targetId: Label) = JvmTestEnvironmentParams(listOf(targetId))

  override fun calculateConfiguration(
    configurationName: String,
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: BuildTarget,
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
