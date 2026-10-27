package org.jetbrains.bazel.languages.projectview.annotation.quickfix

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.modcommand.ModCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.projectview.annotation.suppress.ProjectViewActionWithSuppressOptions

/**
 * A quick fix of a project view problem for the editor and for Inspect Code.
 *
 * Inspect Code uses a fix of an annotation only when the fix is a [LocalQuickFix].
 * [ModCommandAction.asIntention] returns an intention that is not a [LocalQuickFix], so Inspect Code would show no fix.
 * This class is an [IntentionAction] for the editor and a [LocalQuickFix] for Inspect Code. It delegates both to the intention.
 */
internal class ProjectViewLocalQuickFix(
  action: ModCommandAction,
  override val suppressActions: List<ModCommandAction> = emptyList(),
) : ProjectViewActionWithSuppressOptions, LocalQuickFix {

  private val fix = action.asIntention()

  private val batchFix = IntentionWrapper(fix)

  override fun getText(): String = fix.text

  override fun getFamilyName(): String = fix.familyName

  override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean = fix.isAvailable(project, editor, psiFile)

  override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?) = fix.invoke(project, editor, psiFile)

  override fun startInWriteAction(): Boolean = fix.startInWriteAction()

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) = batchFix.applyFix(project, descriptor)

  override fun generatePreview(
    project: Project,
    previewDescriptor: ProblemDescriptor,
  ): IntentionPreviewInfo = batchFix.generatePreview(project, previewDescriptor)

  override fun asIntention(): IntentionAction = this

  override fun asModCommandAction(): ModCommandAction? = fix.asModCommandAction()

  override fun generatePreview(
    project: Project,
    editor: Editor,
    psiFile: PsiFile,
  ): IntentionPreviewInfo = fix.generatePreview(project, editor, psiFile)

  override fun getElementToMakeWritable(currentFile: PsiFile): PsiElement? = fix.getElementToMakeWritable(currentFile)

  override fun getFileModifierForPreview(target: PsiFile): FileModifier? = fix.getFileModifierForPreview(target)

  override fun isDumbAware(): Boolean = fix.isDumbAware
}
