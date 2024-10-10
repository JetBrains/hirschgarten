package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
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

  private fun appendNode(node: ASTNode) {
    val myNode = this.node
    while (isNotActualListElementOrOpeningBracket(myNode.lastChildNode)) myNode.removeChild(myNode.lastChildNode)
    if (myNode.lastChildNode.elementType != StarlarkTokenTypes.LBRACKET) {
      myNode.addChild(StarlarkElementGenerator(this.project).createTokenType(","))
    }

    myNode.addChild(node)
    myNode.addChild(StarlarkElementGenerator(this.project).createTokenType("]"))
  }

  private fun isNotActualListElementOrOpeningBracket(node: ASTNode) =
    node is PsiWhiteSpace || node.elementType == StarlarkTokenTypes.COMMA || node.elementType == StarlarkTokenTypes.RBRACKET
}
