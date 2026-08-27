package org.jetbrains.bazel.languages.starlark

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement

internal object StarlarkUtils {
  val STARLARK_ASSIGNMENT_OPERATORS = TokenSet.create(
    StarlarkTokenTypes.EQ,
    *StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS.types,
    StarlarkTokenTypes.IN_KEYWORD,
  )
  val STARLARK_WHITESPACES_AND_COMMENTS = TokenSet.orSet(
    StarlarkTokenSets.WHITESPACE,
    StarlarkTokenSets.COMMENT,
  )

  fun selectLeftHandSideOfAssignment(node: PsiElement): PsiElement? =
    nearestRelevantBeforeOperator(node.firstChild)

  fun nearestRelevantBeforeOperator(node: PsiElement?): PsiElement? {
    var current = node
    while (current != null) {
      val elementType = current.node?.elementType
      if (STARLARK_ASSIGNMENT_OPERATORS.contains(elementType)) return null
      if (current is StarlarkBaseElement && !STARLARK_WHITESPACES_AND_COMMENTS.contains(elementType)) return current
      current = current.nextSibling
    }
    return null
  }

  fun nextRelevantSibling(element: PsiElement): PsiElement? {
    var current = element.nextSibling
    while (current != null) {
      val elementType = current.node?.elementType
      if (current is StarlarkBaseElement && !STARLARK_WHITESPACES_AND_COMMENTS.contains(elementType)) return current
      current = current.nextSibling
    }
    return null
  }

  fun nearestRelevantAfter(node: PsiElement, separator: StarlarkTokenType): PsiElement? {
    var current = node.firstChild
    while (current != null) {
      if (current.node?.elementType == separator) return nextRelevantSibling(current)
      current = current.nextSibling
    }
    return null
  }

  fun relevantChildrenAfterEach(node: PsiElement, separator: StarlarkTokenType): List<PsiElement> =
    buildList {
      var current = node.firstChild
      while (current != null) {
        if (current.node?.elementType == separator) {
          nextRelevantSibling(current)?.let(::add)
        }
        current = current.nextSibling
      }
    }

  /**
   * original version: `com.jetbrains.python.psi.impl.PyPsiUtils.getNextNonWhitespaceSibling(com.intellij.lang.ASTNode)`
   */
  fun getNextNonWhitespaceSibling(after: ASTNode): ASTNode? {
    var next = after.treeNext
    while (next != null) {
      if (next !is PsiWhiteSpace) {
        return next
      }
      next = next.treeNext
    }
    return null
  }
}
