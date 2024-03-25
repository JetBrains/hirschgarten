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
      val (keepSearching, inFunction) = searchInParent(parent, stopAt, processor)
      if (keepSearching) searchInFile(parent, processor, inFunction || fromFunction) else Unit
    }
  }

  private fun searchInParent(
    parent: PsiElement, stopAt: PsiElement?, processor: Processor<StarlarkElement>
  ): Pair<Boolean, Boolean> {
    val keepSearching = when (parent) {
      is StarlarkFile -> parent.searchInTopLevel(processor, stopAt)
      is StarlarkFunctionDeclaration -> parent.searchInParameters(processor)
      is StarlarkForStatement -> parent.searchInLoopVariables(processor)
      is StarlarkStatementList -> parent.searchInAssignments(processor)
      else -> true
    }
    return Pair(keepSearching, parent is StarlarkFunctionDeclaration)
  }

  private fun StarlarkFile.searchInTopLevel(
    processor: Processor<StarlarkElement>, stopAt: PsiElement?
  ): Boolean {
    for (child in findChildrenByClass(StarlarkElement::class.java)) {
      val keepSearching = when (child) {
        stopAt -> break
        is StarlarkAssignmentStatement -> child.check(processor)
        is StarlarkFunctionDeclaration -> processor.process(child)
        else -> true
      }
      if (!keepSearching) return false
    }
    return true
  }

  private fun StarlarkStatementList.searchInAssignments(processor: Processor<StarlarkElement>): Boolean {
    for (assignment in getAssignments()) {
      if (!assignment.check(processor)) return false
    }
    return true
  }

  private fun StarlarkFunctionDeclaration.searchInParameters(processor: Processor<StarlarkElement>): Boolean {
    for (parameter in getParameters()) {
      if (!processor.process(parameter)) return false
    }
    return true
  }

  private fun StarlarkForStatement.searchInLoopVariables(processor: Processor<StarlarkElement>): Boolean {
    for (loopVariable in getLoopVariables()) {
      if (!processor.process(loopVariable)) return false
    }
    return true
  }

  private fun StarlarkAssignmentStatement.check(processor: Processor<StarlarkElement>): Boolean =
    getTargetExpression()?.let { processor.process(it) } ?: true
}
