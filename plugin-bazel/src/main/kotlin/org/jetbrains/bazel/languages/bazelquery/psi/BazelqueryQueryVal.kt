package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets

class BazelqueryQueryVal(node: ASTNode) : BazelqueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitQueryVal(this)

  val words: Array<BazelqueryWord>
    get() = this.findChildrenByClass(BazelqueryWord::class.java)

  val word: String = this.findChildByType<PsiElement>(BazelqueryTokenSets.WORDS)?.text ?: ""

}
