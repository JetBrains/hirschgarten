package org.jetbrains.bazel.ui.projectTree.action

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bsp.protocol.id
import org.jetbrains.bsp.protocol.isManual

internal class BazelAllTestsInDirectoryRunConfigurationProducer : LazyRunConfigurationProducer<BazelRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory = bazelRunConfigurationFactory

  override fun setupConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>,
  ): Boolean {
    if (!context.project.isBazelProject) return false
    val selectedDirectory = getSelectedDirectory(context) ?: return false
    val testTargets = toChildTestTargets(selectedDirectory, context.project).takeIf { it.isNotEmpty() } ?: return false
    configuration.updateTargets(testTargets.toList())
    configuration.name = ExecutionBundle.message("test.in.scope.presentable.text", selectedDirectory.name)
    return true
  }

  override fun isConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
  ): Boolean {
    val selectedDirectory = getSelectedDirectory(context) ?: return false
    val testTargets = toChildTestTargets(selectedDirectory, context.project).takeIf { it.isNotEmpty() } ?: return false
    if (configuration.targets.toSet() != testTargets) return false
    return configuration.name.startsWith(ExecutionBundle.message("test.in.scope.presentable.text", selectedDirectory.name))
  }

  private fun getSelectedDirectory(context: ConfigurationContext): VirtualFile? =
    (context.psiLocation as? PsiDirectory)?.virtualFile

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-2709
  private fun toChildTestTargets(directory: VirtualFile, project: Project): Set<Label> {
    val targetUtils = project.targetStorage
    val path = directory.toNioPathOrNull() ?: return emptySet()
    val childTargets = targetUtils
      .allTargetSummaries()
      .asSequence()
      .filter { it.baseDirectory.startsWith(path) }

    return childTargets
      .filter { it.kind.ruleType == RuleType.TEST && !it.isManual }
      .map { it.id }
      .toSet()
  }
}
