package org.jetbrains.bazel.jvm.junit

import com.intellij.execution.PsiLocation
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.bazel.config.workspaceName
import org.jetbrains.bazel.runfiles.RunfilesUtils
import org.jetbrains.bazel.runnerAction.LocalJvmRunnerRunConfigurationProvider
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class JunitLocalJvmRunnerRunConfigurationProvider : LocalJvmRunnerRunConfigurationProvider {
  override fun provideRunConfiguration(
    configurationName: String,
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    callerPsiElement: PsiElement,
  ): RunConfiguration? =
    if (project.bazelProjectSettings.useIntellijTestRunner) {
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
    val psiMethod = psiElement.getPsiMethodOrNull()
    val psiClass = psiElement.getPsiClassOrNull()

    runReadAction {
      if (psiMethod != null) {
        beMethodConfiguration(PsiLocation(psiMethod))
      } else {
        beClassConfiguration(psiClass)
      }
    }
  }

  private fun PsiElement.getPsiClassOrNull(): PsiClass? =
    parent as? PsiClass ?: runReadAction { getParentOfType<KtClass>(false)?.toLightClass() }

  private fun PsiElement.getPsiMethodOrNull(): PsiMethod? = parent as? PsiMethod ?: runReadAction { parent.getRepresentativeLightMethod() }

  private fun defineDefaultBazelEnvs(project: Project, environment: JvmEnvironmentItem): Map<String, String> =
    mapOf(
      "TEST_SRCDIR" to RunfilesUtils.calculateTargetRunfiles(project, environment.target).toString(),
      ("TEST_WORKSPACE" to (project.workspaceName ?: "_main")),
    )
}
