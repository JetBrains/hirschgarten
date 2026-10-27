package org.jetbrains.bazel.languages.projectview.annotation.suppress

import com.intellij.codeInsight.daemon.impl.HighlightVisitorBasedInspection
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommandExecutor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parents
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.checker.check
import org.jetbrains.bazel.languages.projectview.checker.isSuppressible

/**
 * Gives the suppress actions of project view warnings to Inspect Code.
 *
 * Inspect Code runs the annotator through the platform annotator tool, [HighlightVisitorBasedInspection].
 * That tool has no suppress actions of its own, so the Inspect Code view asks the inspection suppressors of the language for them.
 * The editor does not use this class, because the submenu of each fix holds the suppress actions there.
 * [isSuppressedFor] is always false, because the checker already leaves out the suppressed warnings.
 */
internal class ProjectViewAnnotatorSuppressor : InspectionSuppressor {

  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean = false

  override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> = when (toolId) {
    HighlightVisitorBasedInspection.SHORT_NAME -> ProjectViewSuppressScope.entries
      .map(::ProjectViewBatchSuppressFix)
      .toTypedArray()
    else -> SuppressQuickFix.EMPTY_ARRAY
  }
}

private class ProjectViewBatchSuppressFix(private val scope: ProjectViewSuppressScope) : SuppressQuickFix {

  override fun getFamilyName(): String = name

  override fun getName(): String = BazelProjectViewBundle.message(scope.batchKey)

  override fun isAvailable(
    project: Project,
    context: PsiElement
  ): Boolean = scope.containerOf(context) != null && context.suppressibleProblem() != null

  override fun isSuppressAll(): Boolean = false

  override fun startInWriteAction(): Boolean = false

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement ?: return
    val problem = element.suppressibleProblem() ?: return
    val container = scope.containerOf(element) ?: return
    val context = ActionContext.from(null, element.containingFile)
    val command = addProjectViewSuppressionCommand(container, problem.id)
    ModCommandExecutor
      .getInstance()
      .executeInteractively(context, command, null)
  }

  private fun PsiElement.suppressibleProblem(): ProjectViewProblem? {
    val topLevel = parents(withSelf = true).firstOrNull { it.parent is PsiFile } ?: return null
    return ProjectViewChecker.Main
      .check(topLevel)
      .firstOrNull { it.isSuppressible && it.element?.textRange == textRange }
  }
}
