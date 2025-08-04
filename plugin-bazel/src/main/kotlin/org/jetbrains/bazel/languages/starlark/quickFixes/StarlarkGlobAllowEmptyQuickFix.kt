package org.jetbrains.bazel.languages.starlark.quickFixes

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.equalsIgnoreWhitespaces
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement

class StarlarkGlobAllowEmptyQuickFix(callExpression: StarlarkGlobExpression) :
  PsiUpdateModCommandAction<StarlarkGlobExpression>(callExpression) {
  private val argumentName = "allow_empty"
  private val argumentValue = "True"

  override fun getPresentation(context: ActionContext, element: StarlarkGlobExpression): Presentation = Presentation.of(familyName)

  override fun getFamilyName(): String = StarlarkBundle.message("quickfix.name.glob.allow.empty")

  override fun invoke(
    actionContext: ActionContext,
    element: StarlarkGlobExpression,
    updater: ModPsiUpdater,
  ) {
    val writableElement = updater.getWritable(element)
    val argumentList = writableElement.getArguments()
    val allowEmptyArgument = writableElement.getKeywordArgument(argumentName)

    if (allowEmptyArgument != null) {
      val newArgument = createFromText(allowEmptyArgument.project, "$argumentName = $argumentValue")
      updater.getWritable(allowEmptyArgument).replace(newArgument)
    } else {
      addAllowEmptyArgument(updater, writableElement, argumentList)
    }
  }

  private fun addAllowEmptyArgument(
    updater: ModPsiUpdater,
    element: StarlarkGlobExpression,
    argumentList: Array<StarlarkArgumentElement>,
  ) {
    val project = element.project
    val lastArgument = argumentList.lastOrNull()

    if (lastArgument != null) {
      val newArgumentElement = createFromText(project, "$argumentName = $argumentValue")
      val writableLastArgument = updater.getWritable(lastArgument)
      val parent = writableLastArgument.parent
      var anchor: PsiElement = writableLastArgument

      // add comma if needed
      if (!writableLastArgument.text.trimEnd().endsWith(",")) {
        val comma = createFromText(project, ",")
        anchor = parent.addAfter(comma, anchor)
      }

      // add allow_empty argument
      val addedArgument = parent.addAfter(newArgumentElement, anchor)

      // add space before allow_empty argument
      val space = createFromText(project, " ")
      parent.addBefore(space, addedArgument)
    } else {
      val newArgumentList = createFromText(project, "($argumentName = $argumentValue)")
      val writableElement = updater.getWritable(element)

      val next = writableElement.nextSibling
      if (next != null && next.text.equalsIgnoreWhitespaces("()")) {
        next.replace(newArgumentList)
      } else {
        writableElement.addAfter(newArgumentList, writableElement.firstChild)
      }
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
