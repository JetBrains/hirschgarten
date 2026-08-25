package org.jetbrains.bazel.ui.projectTree.action

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.repomapping.calculateWildcardLabel
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory

@ApiStatus.Internal
open class BazelAllTestsInDirectoryRunConfigurationProducer : LazyRunConfigurationProducer<BazelRunConfiguration>() {
  override fun isDumbAware(): Boolean = true

  override fun getConfigurationFactory(): ConfigurationFactory = bazelRunConfigurationFactory

  override fun setupConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>,
  ): Boolean {
    val project = context.project
    if (!isProjectApplicable(project)) return false
    val selectedDirectory = getSelectedDirectory(context) ?: return false
    if (ProjectFileIndex.getInstance(project).isExcluded(selectedDirectory)) return false

    val testTarget = calculateWildcardLabel(context.project, selectedDirectory) ?: return false
    configuration.updateTargets(listOf(testTarget))
    configuration.name = ExecutionBundle.message("test.in.scope.presentable.text", selectedDirectory.name)
    return true
  }

  open fun isProjectApplicable(project: Project): Boolean =
    project.isBazelProject

  override fun isConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
  ): Boolean {
    val selectedDirectory = getSelectedDirectory(context) ?: return false
    val testTarget = calculateWildcardLabel(context.project, selectedDirectory) ?: return false
    if (configuration.targets != listOf(testTarget)) return false
    return configuration.name.startsWith(ExecutionBundle.message("test.in.scope.presentable.text", selectedDirectory.name))
  }

  private fun getSelectedDirectory(context: ConfigurationContext): VirtualFile? =
    (context.psiLocation as? PsiDirectory)?.virtualFile
}
