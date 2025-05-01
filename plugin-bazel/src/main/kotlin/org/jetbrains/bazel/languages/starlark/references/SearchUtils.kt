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
  /**
   * Invokes [processor] on each binding in the current scope as well as parent scopes of
   * [element] such that a binding is visited before any bindings it may shadow.
   *
   * https://github.com/bazelbuild/starlark/blob/6dd78ee3a66820a8b7571239946466cc702b209e/spec.md#name-binding-and-variables
   */
  fun searchInFile(element: PsiElement, processor: Processor<StarlarkElement>) {
    var element: PsiElement? = element
    while (element != null) {
      val scopeRoot = findScopeRoot(element) ?: return
      if (!processBindingsInScope(
          scopeRoot,
          processor = processor,
          elementIsScopeRoot = true,
        )
      ) {
        return
      }
      element = scopeRoot.parent
    }
  }

  private tailrec fun findScopeRoot(element: PsiElement): PsiElement? =
    when (element) {
      is StarlarkFile, is StarlarkCallable -> element
      // Default values of parameters refer to the parent scope of the function, not the function scope.
      is StarlarkParameterList -> findScopeRoot(element.parent.parent)
      else -> when (val parent = element.parent) {
        null -> null
        is StarlarkCompExpression -> {
          // In a comprehension, the first 'for' resolves using the scope that contains the
          // comprehension, while all later 'for's resolve within the comprehension's scope.
          val inFirstFor = generateSequence(element) { it.prevSibling }
            .filter { it.elementType == StarlarkTokenTypes.FOR_KEYWORD || it.elementType == StarlarkTokenTypes.IF_KEYWORD }
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
    processor: Processor<StarlarkElement>,
    elementIsScopeRoot: Boolean = false,
  ): Boolean =
    when (element) {
      is StarlarkFile -> element.searchInLoads(processor) && element.children.all { processBindingsInScope(it, processor) }

      is StarlarkCallable -> if (elementIsScopeRoot) {
        element.searchInParameters(processor) && (element !is StarlarkFunctionDeclaration || processBindingsInScope(
          element.getStatementList(),
          processor,
        ))
      } else {
        element !is StarlarkFunctionDeclaration || processor.process(element)
      }

      is StarlarkStatementContainer -> element.getStatementLists()
        .all { processBindingsInScope(it, processor) } && (element !is StarlarkForStatement || element.searchInLoopVariables(
        processor,
      ))

      is StarlarkCompExpression -> element.searchInComprehension(processor)
      is StarlarkStatementList -> element.children.all { processBindingsInScope(it, processor) }
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
