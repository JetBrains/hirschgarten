package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes

class BazelqueryFlag(node: ASTNode) : BazelqueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitFlag(this)

  val name: PsiElement?
    get() = findChildByType(BazelqueryTokenTypes.FLAG)

}
