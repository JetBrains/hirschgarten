package org.jetbrains.bazel.junit

import com.intellij.execution.PsiLocation
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.bazel.config.workspaceName
import org.jetbrains.bazel.runfiles.RunfilesUtils
import org.jetbrains.bazel.runnerAction.LocalJvmRunnerRunConfigurationProvider
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bsp.protocol.JvmEnvironmentItem

class JunitLocalJvmRunnerRunConfigurationProvider : LocalJvmRunnerRunConfigurationProvider {
  override fun provideRunConfiguration(
    configurationName: String,
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    callerPsiElement: PsiElement,
  ): RunConfiguration? =
    if (project.bazelJVMProjectSettings.useIntellijTestRunner) {
      createConfiguration(configurationName, project, callerPsiElement, module, environment)
    } else {
      null
    }

  private fun createConfiguration(
    configurationName: String,
    project: Project,
    callerPsiElement: PsiElement,
    module: Module,
    environment: JvmEnvironmentItem,
  ): JUnitConfiguration =
    JUnitConfiguration(configurationName, project).apply {
      setModule(module)
      setClassOrMethodConfiguration(callerPsiElement)
      classpathModifications.addAll(
        environment.classpath.map {
          ModuleBasedConfigurationOptions.ClasspathModification(it.toString(), false)
        },
      )
      workingDirectory = environment.workingDirectory.toString()
      vmParameters += environment.jvmOptions.joinToString(" ", prefix = " ")
      envs = environment.environmentVariables + defineDefaultBazelEnvs(project, environment)
      shortenCommandLine = ShortenCommandLine.MANIFEST
    }

  private fun JUnitConfiguration.setClassOrMethodConfiguration(psiElement: PsiElement) {
    val psiMethod = PsiElementConfigurationLocator.ep.extensionList.firstNotNullOfOrNull { it.getPsiMethod(psiElement) }
    val psiClass = PsiElementConfigurationLocator.ep.extensionList.firstNotNullOfOrNull { it.getPsiClass(psiElement) }

    runReadAction {
      if (psiMethod != null) {
        beMethodConfiguration(PsiLocation(psiMethod))
      } else {
        beClassConfiguration(psiClass)
      }
    }
  }

  private fun defineDefaultBazelEnvs(project: Project, environment: JvmEnvironmentItem): Map<String, String> =
    mapOf(
      "TEST_SRCDIR" to RunfilesUtils.calculateTargetRunfiles(project, environment.target).toString(),
      ("TEST_WORKSPACE" to (project.workspaceName ?: "_main")),
    )
}

interface PsiElementConfigurationLocator {
  fun getPsiMethod(callerPsiElement: PsiElement): PsiMethod?

  fun getPsiClass(callerPsiElement: PsiElement): PsiClass?

  companion object {
    val ep = ExtensionPointName.create<PsiElementConfigurationLocator>("org.jetbrains.bazel.psiElementConfigurationLocator")
  }
}
