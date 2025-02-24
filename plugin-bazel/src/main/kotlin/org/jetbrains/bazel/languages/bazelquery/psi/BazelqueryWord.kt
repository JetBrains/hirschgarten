package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets

class BazelqueryWord(node: ASTNode) : BazelqueryBaseElement(node) {
    override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitWord(this)

  val word: String = this.findChildByType<PsiElement>(BazelqueryTokenSets.WORDS)?.text ?: ""

}
