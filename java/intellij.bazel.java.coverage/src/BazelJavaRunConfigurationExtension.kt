package org.jetbrains.bazel.java.coverage

import com.intellij.execution.Location
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.coverage.JavaCoverageEnabledConfiguration
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.ApiStatus

internal class BazelJavaRunConfigurationExtension : RunConfigurationExtension() {
  override fun <T : RunConfigurationBase<*>> updateJavaParameters(
    configuration: T,
    params: JavaParameters,
    runnerSettings: RunnerSettings?,
  ) {
  }

  override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean =
    isJavaAgentCoverageApplicableTo(configuration)

  /**
   * See [com.intellij.execution.coverage.CoverageJavaRunConfigurationExtension.extendCreatedConfiguration]
   * which assumes a configuration implements [com.intellij.execution.CommonJavaRunConfigurationParameters]
   */
  override fun extendCreatedConfiguration(configuration: RunConfigurationBase<*>, location: Location<*>) {
    val coverageEnabledConfiguration = JavaCoverageEnabledConfiguration.getFrom(configuration)
    if (coverageEnabledConfiguration == null) return

    val psiElement = location.psiElement
    val testClassFqn: String? = FullyQualifiedNameProvider.ep.extensionList.firstNotNullOfOrNull { extension ->
      ReadAction.nonBlocking<String> { extension.getFullyQualifiedName(psiElement) }.executeSynchronously()
    }
    if (testClassFqn == null) return

    // By default, only consider classes having the same package as the test class
    coverageEnabledConfiguration.setUpCoverageFilters(testClassFqn, null)
  }
}

@ApiStatus.Internal
interface FullyQualifiedNameProvider {
  @RequiresReadLock
  fun getFullyQualifiedName(psiElement: PsiElement): String?

  companion object {
    val ep: ExtensionPointName<FullyQualifiedNameProvider> = ExtensionPointName("org.jetbrains.bazel.fullyQualifiedNameProvider")
  }
}

internal class JavaFullyQualifiedNameProvider : FullyQualifiedNameProvider {
  override fun getFullyQualifiedName(psiElement: PsiElement): String? {
    val psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java, false) ?: return null
    return psiClass.qualifiedName
  }
}
