package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList

object SearchUtils {
  tailrec fun searchInFile(
    currentElement: PsiElement,
    processor: Processor<StarlarkElement>,
    fromFunction: Boolean = false,
  ): Unit =
    when (currentElement) {
      is PsiFileSystemItem -> Unit
      else -> {
        val parent = currentElement.parent
        val stopAt = if (!fromFunction) currentElement else null
        val keepSearching = searchInParent(parent, stopAt, processor)
        val inFunction = parent is StarlarkCallable
        if (keepSearching) searchInFile(parent, processor, inFunction || fromFunction) else Unit
      }
    }

  private fun searchInParent(
    parent: PsiElement,
    stopAt: PsiElement?,
    processor: Processor<StarlarkElement>,
  ): Boolean =
    when (parent) {
      is StarlarkFile -> parent.searchInTopLevel(processor, stopAt)
      is StarlarkCallable -> parent.searchInParameters(processor)
      is StarlarkForStatement -> parent.searchInLoopVariables(processor)
      is StarlarkStatementList -> parent.searchInAssignmentsAndFunctionDeclarations(processor)
      is StarlarkCompExpression -> parent.searchInComprehension(processor)
      else -> true
    }

  private fun StarlarkStatementList.searchInAssignmentsAndFunctionDeclarations(processor: Processor<StarlarkElement>): Boolean =
    getAssignments().all { it.check(processor) } &&
      getFunctionDeclarations().all { processor.process(it) }

  private fun StarlarkCallable.searchInParameters(processor: Processor<StarlarkElement>): Boolean =
    getParameters().all { processor.process(it) }

  private fun StarlarkForStatement.searchInLoopVariables(processor: Processor<StarlarkElement>): Boolean =
    getLoopVariables().all { processor.process(it) }

  private fun StarlarkCompExpression.searchInComprehension(processor: Processor<StarlarkElement>): Boolean =
    getCompVariables().all { processor.process(it) }
}
