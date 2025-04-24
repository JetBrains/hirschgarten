package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
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
  fun searchInFile(currentElement: PsiElement, processor: Processor<StarlarkElement>) {
    var element = currentElement
    while (true) {
      val scopeRoot = findScopeRoot(element) ?: return
      if (!processBindingsInScope(
          scopeRoot,
          elementIsRoot = true,
          stopAt = if (scopeRoot is StarlarkFile && scopeRoot != currentElement) currentElement else null,
          processor = processor,
        )
      ) {
        return
      }
      element = scopeRoot.parent
    }
  }

  private tailrec fun findScopeRoot(currentElement: PsiElement): PsiElement? =
    when (currentElement) {
      is StarlarkFile, is StarlarkFunctionDeclaration -> currentElement
      // Default values of parameters refer to the parent scope of the function, not the function scope.
      is StarlarkParameterList -> findScopeRoot(currentElement.parent.parent)
      else -> when (val parent = currentElement.parent) {
        null -> null
        is StarlarkCompExpression -> {
          // In a comprehension, the first 'for' resolves using the scope that contains the
          // comprehension, while all later 'for's resolve within the comprehension's scope.
          val inFirstFor = generateSequence(currentElement) { it.prevSibling }
            .filter { it.elementType == StarlarkTokenTypes.FOR_KEYWORD }
            .take(2)
            .count() == 1
          if (inFirstFor) {
            findScopeRoot(parent)
          } else {
            parent
          }
        }

        else -> findScopeRoot(parent)
      }
    }

  private fun processBindingsInScope(
    element: PsiElement,
    elementIsRoot: Boolean = false,
    stopAt: PsiElement?,
    processor: Processor<StarlarkElement>,
  ): Boolean =
    when (element) {
      stopAt -> false
      is StarlarkFile -> element.searchInLoads(processor, stopAt) && element.children.all {
        processBindingsInScope(
          it,
          stopAt = stopAt,
          processor = processor,
        )
      }

      is StarlarkFunctionDeclaration -> processor.process(element) && (!elementIsRoot || (element.searchInParameters(processor)
        && processBindingsInScope(
        element.getStatementList(),
        stopAt = stopAt,
        processor = processor,
      )))

      is StarlarkCompExpression -> element.searchInComprehension(processor)
      is StarlarkForStatement -> element.searchInLoopVariables(processor) &&
        element.getStatementLists().all { processBindingsInScope(it, stopAt = stopAt, processor = processor) }

      is StarlarkStatementContainer -> element.getStatementLists()
        .all { processBindingsInScope(it, stopAt = stopAt, processor = processor) }

      is StarlarkStatementList -> element.children.all { processBindingsInScope(it, stopAt = stopAt, processor = processor) }
      is StarlarkAssignmentStatement -> element.check(processor)
      else -> true
    }

  private fun StarlarkCallable.searchInParameters(processor: Processor<StarlarkElement>): Boolean =
    getParameters().all { processor.process(it) }

  private fun StarlarkForStatement.searchInLoopVariables(processor: Processor<StarlarkElement>): Boolean =
    getLoopVariables().all { processor.process(it) }

  private fun StarlarkCompExpression.searchInComprehension(processor: Processor<StarlarkElement>): Boolean =
    getCompVariables().all { processor.process(it) }
}
