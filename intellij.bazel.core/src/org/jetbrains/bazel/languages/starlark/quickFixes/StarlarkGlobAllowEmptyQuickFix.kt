package org.jetbrains.bazel.languages.starlark.quickFixes

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.globbing.asGlobCallOrNull
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.rename.StarlarkElementGenerator

internal class StarlarkGlobAllowEmptyQuickFix(
  callExpression: StarlarkCallExpression
) : PsiUpdateModCommandAction<StarlarkCallExpression>(callExpression) {

  override fun getFamilyName(): String = StarlarkBundle.message("quickfix.glob.allow.empty")

  override fun invoke(
    actionContext: ActionContext,
    element: StarlarkCallExpression,
    updater: ModPsiUpdater,
  ) {
    val glob = updater.getWritable(element).asGlobCallOrNull() ?: return
    val allowEmptyArgument = glob.allowEmptyArgument
    val newArgument = StarlarkElementGenerator(glob.call.project).createAllowEmptyArgument().psi
    if (allowEmptyArgument != null) {
      allowEmptyArgument.replace(newArgument)
    } else {
      addAllowEmptyArgument(updater, glob.call, newArgument)
    }
  }

  private fun addAllowEmptyArgument(
    updater: ModPsiUpdater,
    element: StarlarkCallExpression,
    newArgument: PsiElement,
  ) {
    val project = element.project
    val lastArgument = element.getArgumentList()?.getArguments()?.lastOrNull()

    if (lastArgument != null) {
      val writableLastArgument = updater.getWritable(lastArgument)
      val parent = writableLastArgument.parent
      var anchor: PsiElement = writableLastArgument

      // add comma if needed
      if (!writableLastArgument.text.trimEnd().endsWith(",")) {
        anchor = parent.addAfter(createFromText(project, ","), anchor)
      }

      // add allow_empty argument separated by space
      val addedArgument = parent.addAfter(newArgument, anchor)
      parent.addBefore(createFromText(project, " "), addedArgument)
    } else {
      val argumentList = updater.getWritable(element).getArgumentList() ?: return
      argumentList.addAfter(newArgument, argumentList.firstChild)
    }
  }

  private fun createFromText(project: Project, text: String): PsiElement {
    val file =
      PsiFileFactory.getInstance(project).createFileFromText(
        "dummy.starlark",
        StarlarkLanguage,
        text,
      )
    return file.firstChild
  }
}
