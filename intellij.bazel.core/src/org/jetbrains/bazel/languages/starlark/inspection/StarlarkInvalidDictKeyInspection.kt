package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkKeyValueExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression

@ApiStatus.Internal
class StarlarkInvalidDictKeyInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = InvalidDictKeyVisitor(holder)

  private class InvalidDictKeyVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitKeyValueExpression(node: StarlarkKeyValueExpression) {
      val key = node.children.firstOrNull { isExpression(it) } ?: return
      if (isUnhashableLiteral(key)) {
        holder.registerProblem(
          key,
          StarlarkBundle.message("inspection.description.dict.key.not.hashable")
        )
      }
    }

    private fun isUnhashableLiteral(element: PsiElement): Boolean =
      when (val expression = unwrapParenthesized(element)) {
        is StarlarkListLiteralExpression,
        is StarlarkDictLiteralExpression -> true
        is StarlarkTupleExpression -> tupleElements(expression).any(::isUnhashableLiteral)
        else -> false
      }

    private fun unwrapParenthesized(element: PsiElement): PsiElement =
      when (element) {
        is StarlarkParenthesizedExpression -> element.getTuple()
          ?: element.children.firstOrNull { isExpression(it) }?.let(::unwrapParenthesized)
          ?: element
        else -> element
      }

    private fun tupleElements(tuple: StarlarkTupleExpression): Sequence<PsiElement> =
      tuple.children.asSequence().filter { isExpression(it) }

    private fun isExpression(element: PsiElement): Boolean =
      StarlarkElementTypes.EXPRESSIONS.contains(element.elementType)
  }
}
