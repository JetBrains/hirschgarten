package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.Executor
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.runnerAction.RunSyntheticTargetAction
import org.jetbrains.bazel.ui.widgets.tool.window.utils.getSupportedExecutors as getSupportedRunConfigurationExecutors
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
object SyntheticRunTargetUtils {
  private val escapePattern = Regex("[^A-Za-z0-9_]")

  fun getSyntheticTargetLabel(vararg packageParts: String, targetName: String): Label {
    return ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf(Constants.DOT_BAZELBSP_DIR_NAME, Constants.SYNTHETIC_TARGETS_DIR_NAME) + packageParts),
      target = SingleTarget(targetName),
    )
  }

  fun getTemplateGenerators(target: ExecutableTarget, language: Language): List<SyntheticRunTargetTemplateGenerator> =
    SyntheticRunTargetTemplateGenerator.ep.allForLanguage(language)
      .filter { it.isSupported(target) }

  fun escapeTargetLabel(input: String): String {
    return input.replace(escapePattern, "_")
  }

  fun addSyntheticRunActions(
    group: DefaultActionGroup,
    project: Project,
    target: ExecutableTarget,
    element: PsiElement,
  ) {
    val language = element.language
    for (generator in getTemplateGenerators(target, language)) {
      for (executor in getSupportedExecutors(project, target, element, generator)) {
        group.addAction(
          RunSyntheticTargetAction(
            project = project,
            target = target,
            executor = executor,
            templateGenerator = generator,
            targetElement = element,
          ),
        )
      }
    }
  }

  private fun getSupportedExecutors(
    project: Project,
    target: ExecutableTarget,
    element: PsiElement,
    generator: SyntheticRunTargetTemplateGenerator,
  ): List<Executor> {
    val settings = RunSyntheticTargetAction(
      project = project,
      target = target,
      templateGenerator = generator,
      targetElement = element,
    ).createRunConfiguration() ?: return emptyList()
    return getSupportedRunConfigurationExecutors(settings)
  }
}
