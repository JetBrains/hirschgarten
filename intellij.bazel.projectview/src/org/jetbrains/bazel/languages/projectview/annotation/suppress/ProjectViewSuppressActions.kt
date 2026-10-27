package org.jetbrains.bazel.languages.projectview.annotation.suppress

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.intention.AbstractEmptyIntentionAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionWithOptions
import com.intellij.codeInsight.intention.IntentionActionWithOptions.CombiningPolicy
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCommandAction
import com.intellij.modcommand.Presentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem

internal fun ProjectViewProblem.Id.suppressActions(): List<ModCommandAction> = ProjectViewSuppressScope
  .entries
  .map { ProjectViewSuppressAction(this, it) }

internal interface ProjectViewActionWithSuppressOptions : IntentionActionWithOptions {

  val suppressActions: List<ModCommandAction>

  override fun getOptions(): List<IntentionAction> = suppressActions.map { it.asIntention() }

  override fun getCombiningPolicy(): CombiningPolicy = CombiningPolicy.IntentionOptionsOnly

  companion object {

    val KEY: HighlightDisplayKey = HighlightDisplayKey.findOrRegister(
      /* shortName = */ "BazelProjectView",
      /* displayName = */ BazelProjectViewBundle.message("annotator.warning.options.title"),
    )
  }
}

/**
 * Groups the suppress actions of a warning that has no quick fixes. The actions are in the submenu of this entry.
 * Like for the Kotlin compiler warnings, the suppress actions are never at the top level of the intention list.
 * A warning with quick fixes does not need this entry, because the submenu of each fix holds the suppress actions.
 */
internal class ProjectViewSuppressOptionsAction(
  private val problemId: ProjectViewProblem.Id,
  override val suppressActions: List<ModCommandAction>,
) : AbstractEmptyIntentionAction(), ProjectViewActionWithSuppressOptions, LowPriorityAction {

  override fun getText(): String = BazelProjectViewBundle.message("annotator.warning.options", problemId.value)

  override fun getFamilyName(): String = text

  override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean = true

  override fun equals(other: Any?): Boolean = other is ProjectViewSuppressOptionsAction && other.problemId == problemId

  override fun hashCode(): Int = problemId.hashCode()
}

private class ProjectViewSuppressAction(
  private val id: ProjectViewProblem.Id,
  private val scope: ProjectViewSuppressScope,
) : ModCommandAction {

  override fun getFamilyName(): String = BazelProjectViewBundle.message("suppress.family.name")

  override fun getPresentation(context: ActionContext): Presentation? = container(context)
    ?.let { Presentation.of(BazelProjectViewBundle.message(scope.actionKey, id.value)) }

  override fun perform(context: ActionContext): ModCommand = container(context)
    ?.let { addProjectViewSuppressionCommand(it, id) }
    ?: ModCommand.nop()

  private fun container(
    context: ActionContext
  ): PsiElement? = context.findLeaf()?.let(scope::containerOf) ?: context.findLeafOnTheLeft()?.let(scope::containerOf)
}
