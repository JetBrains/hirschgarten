package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.starlark.StarlarkUtils
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.rename.StarlarkElementGenerator

class StarlarkListLiteralExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitListLiteralExpression(this)

  fun getElementAt(index: Int): StarlarkElement? = getElements().getOrNull(index)

  fun getElements(): List<StarlarkElement> = PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkElement::class.java)

  fun appendString(string: String) = appendNode(StarlarkElementGenerator(this.project).createStringLiteral(string))

  /**
   * modified method from `com.jetbrains.python.psi.impl.PyElementGeneratorImpl.insertItemIntoList`
   */
  fun insertString(toInsert: String) {
    val add = StarlarkElementGenerator(this.project).createStringLiteral(toInsert)
    val exprNode = this.node
    val closingTokens = exprNode.getChildren(TokenSet.create(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.LPAR))
    if (closingTokens.isEmpty()) {
      // we tried our best. let's just insert it at the end
      exprNode.addChild(add)
    } else {
      val next = StarlarkUtils.getNextNonWhitespaceSibling(closingTokens.last())
      if (next != null) {
        val whitespaceToAdd = (next.treePrev as? PsiWhiteSpace)?.text
        exprNode.addChild(add, next)
        val comma = createComma()
        exprNode.addChild(comma, next)
        whitespaceToAdd?.let {
          exprNode.addLeaf(StarlarkTokenTypes.SPACE, whitespaceToAdd, next)
        }
      } else {
        exprNode.addChild(add)
      }
    }
  }

  private fun appendNode(node: ASTNode) {
    val myNode = this.node
    while (isNotActualListElementOrOpeningBracket(myNode.lastChildNode)) myNode.removeChild(myNode.lastChildNode)
    if (myNode.lastChildNode.elementType != StarlarkTokenTypes.LBRACKET) {
      myNode.addChild(StarlarkElementGenerator(this.project).createTokenType(","))
    }

    myNode.addChild(node)
    myNode.addChild(StarlarkElementGenerator(this.project).createTokenType("]"))
  }

  private fun createComma(): ASTNode = StarlarkElementGenerator(this.project).createTokenType(",")

  private fun isNotActualListElementOrOpeningBracket(node: ASTNode): Boolean =
    node is PsiWhiteSpace || node.elementType == StarlarkTokenTypes.COMMA || node.elementType == StarlarkTokenTypes.RBRACKET
}
