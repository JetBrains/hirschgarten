package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementContainer
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList

object SearchUtils {
  fun searchInFile(
    currentElement: PsiElement,
    processor: Processor<StarlarkElement>,
  ) {
    if (currentElement !is PsiFileSystemItem) {
      val scope = findScope(currentElement)
      findAtOrUnder(scope, if (scope is StarlarkFile) currentElement else null, processor)
    }
  }

  private tailrec fun findScope(currentElement: PsiElement): PsiElement =
    when (currentElement) {
      is StarlarkFile, is StarlarkFunctionDeclaration, is StarlarkCompExpression -> currentElement
      // Default values of parameters refer to the parent scope of the function, not the function scope.
      is StarlarkParameterList -> findScope(currentElement.parent.parent)
      else -> findScope(currentElement.parent)
    }

  private fun findAtOrUnder(
    root: PsiElement,
    stopAt: PsiElement?,
    processor: Processor<StarlarkElement>,
  ): Boolean =
    when (root) {
      stopAt -> false
      is StarlarkFile, is StarlarkStatementList -> root.children.all { findAtOrUnder(it, stopAt, processor) }
      is StarlarkFunctionDeclaration -> processor.process(root) && root.searchInParameters(processor) && findAtOrUnder(
        root.getStatementList(),
        stopAt,
        processor,
      )

      is StarlarkForStatement -> root.searchInLoopVariables(processor) &&
        root.getStatementLists().all { findAtOrUnder(it, stopAt, processor) }

      is StarlarkStatementContainer -> root.getStatementLists().all { findAtOrUnder(it, stopAt, processor) }
      is StarlarkAssignmentStatement -> root.check(processor)
      else -> true
    }

  private fun StarlarkCallable.searchInParameters(processor: Processor<StarlarkElement>): Boolean =
    getParameters().all { processor.process(it) }

  private fun StarlarkForStatement.searchInLoopVariables(processor: Processor<StarlarkElement>): Boolean =
    getLoopVariables().all { processor.process(it) }

  private fun StarlarkCompExpression.searchInComprehension(processor: Processor<StarlarkElement>): Boolean =
    getCompVariables().all { processor.process(it) }
}
