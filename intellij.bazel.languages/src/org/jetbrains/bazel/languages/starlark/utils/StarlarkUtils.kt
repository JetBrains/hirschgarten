package org.jetbrains.bazel.languages.starlark

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement

@ApiStatus.Internal
object StarlarkUtils {
  fun nearestRelevantBeforeOperator(node: PsiElement?): PsiElement? {
    var current = node
    while (current != null) {
      val elementType = current.node?.elementType
      if (StarlarkTokenSets.ASSIGNMENT_OPERATORS.contains(elementType)) return null
      if (current is StarlarkBaseElement && !StarlarkTokenSets.WHITESPACE_AND_COMMENTS.contains(elementType)) return current
      current = current.nextSibling
    }
    return null
  }

  fun previousRelevantSibling(element: PsiElement): PsiElement? {
    var current = element.prevSibling
    while (current != null) {
      val elementType = current.node?.elementType
      if (current is StarlarkBaseElement && !StarlarkTokenSets.WHITESPACE_AND_COMMENTS.contains(elementType)) return current
      current = current.prevSibling
    }
    return null
  }

  fun nextRelevantSibling(element: PsiElement): PsiElement? {
    var current = element.nextSibling
    while (current != null) {
      val elementType = current.node?.elementType
      if (current is StarlarkBaseElement && !StarlarkTokenSets.WHITESPACE_AND_COMMENTS.contains(elementType)) return current
      current = current.nextSibling
    }
    return null
  }

  fun nearestRelevantChildAfter(node: PsiElement, separator: IElementType): PsiElement? {
    var current = node.firstChild
    while (current != null) {
      if (current.node?.elementType == separator) return nextRelevantSibling(current)
      current = current.nextSibling
    }
    return null
  }

  fun relevantChildrenAfterEach(node: PsiElement, separator: IElementType): List<PsiElement> =
    buildList {
      var current = node.firstChild
      while (current != null) {
        if (current.node?.elementType == separator) {
          nextRelevantSibling(current)?.let(::add)
        }
        current = current.nextSibling
      }
    }

  fun relevantChildrenAroundEach(node: PsiElement, separator: IElementType): List<Pair<PsiElement, PsiElement>> =
    buildList {
      var current = node.firstChild
      while (current != null) {
        if (current.node?.elementType == separator) {
          val left = previousRelevantSibling(current)
          val right = nextRelevantSibling(current)
          if (left != null && right != null) {
            add(left to right)
          }
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
