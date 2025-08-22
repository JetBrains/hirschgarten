package org.jetbrains.bazel.languages.starlark.quickFixes

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.equalsIgnoreWhitespaces
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkConstants.ALLOW_EMPTY_KEYWORD
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import java.util.Locale.getDefault

class StarlarkGlobAllowEmptyQuickFix(callExpression: StarlarkGlobExpression) :
  PsiUpdateModCommandAction<StarlarkGlobExpression>(callExpression) {
  override fun getFamilyName(): String = StarlarkBundle.message("quickfix.glob.allow.empty")

  override fun invoke(
    actionContext: ActionContext,
    element: StarlarkGlobExpression,
    updater: ModPsiUpdater,
  ) {
    val writableElement = updater.getWritable(element)
    val allowEmptyArgument = writableElement.getKeywordArgument(ALLOW_EMPTY_KEYWORD)
    val newArgument =
      createFromText(
        writableElement.project,
        "$ALLOW_EMPTY_KEYWORD = ${
          true.toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
        }",
      )

    if (allowEmptyArgument != null) {
      updater.getWritable(allowEmptyArgument).replace(newArgument)
    } else {
      addAllowEmptyArgument(updater, writableElement, newArgument)
    }
  }

  private fun addAllowEmptyArgument(
    updater: ModPsiUpdater,
    element: StarlarkGlobExpression,
    newArgument: PsiElement,
  ) {
    val project = element.project
    val argumentList = element.getArguments()
    val lastArgument = argumentList.lastOrNull()

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
    } else { // empty argument list
      val writableElement = updater.getWritable(element)

      // delete empty parentheses if needed
      val next = writableElement.nextSibling
      if (next != null && next.text.equalsIgnoreWhitespaces("()")) {
        next.delete()
      }

      // add allow_empty argument enclosed in parentheses
      val addedArgument = writableElement.addAfter(newArgument, writableElement.firstChild)
      writableElement.addBefore(createFromText(project, "("), addedArgument)
      writableElement.addAfter(createFromText(project, ")"), addedArgument)
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
