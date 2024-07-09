package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse

object SearchUtils {
  tailrec fun searchInFile(
    currentElement: PsiElement, processor: Processor<StarlarkElement>, fromFunction: Boolean = false
  ): Unit = when (currentElement) {
    is PsiFileSystemItem -> Unit
    else -> {
      val parent = currentElement.parent
      val stopAt = fromFunction.ifFalse { currentElement }
      val keepSearching = searchInParent(parent, stopAt, processor)
      val inFunction = parent is StarlarkFunctionDeclaration
      if (keepSearching) searchInFile(parent, processor, inFunction || fromFunction) else Unit
    }
  }

  private fun searchInParent(
    parent: PsiElement, stopAt: PsiElement?, processor: Processor<StarlarkElement>
  ): Boolean = when (parent) {
    is StarlarkFile -> parent.searchInTopLevel(processor, stopAt)
    is StarlarkFunctionDeclaration -> parent.searchInParameters(processor)
    is StarlarkForStatement -> parent.searchInLoopVariables(processor)
    is StarlarkStatementList -> parent.searchInAssignments(processor)
    else -> true
  }

  private fun StarlarkFile.searchInTopLevel(
    processor: Processor<StarlarkElement>, stopAt: PsiElement?
  ): Boolean = findChildrenByClass(StarlarkElement::class.java).all {
    when (it) {
      stopAt -> false
      is StarlarkAssignmentStatement -> it.check(processor)
      is StarlarkFunctionDeclaration -> processor.process(it)
      else -> true
    }
  }

  private fun StarlarkStatementList.searchInAssignments(processor: Processor<StarlarkElement>): Boolean =
    getAssignments().all { it.check(processor) }

  private fun StarlarkFunctionDeclaration.searchInParameters(processor: Processor<StarlarkElement>): Boolean =
    getParameters().all { processor.process(it) }

  private fun StarlarkForStatement.searchInLoopVariables(processor: Processor<StarlarkElement>): Boolean =
    getLoopVariables().all { processor.process(it) }

  private fun StarlarkAssignmentStatement.check(processor: Processor<StarlarkElement>): Boolean =
    getTargetExpression()?.let { processor.process(it) } ?: true
}
