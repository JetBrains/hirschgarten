package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets

class BazelQueryWord(node: ASTNode) : BazelQueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelQueryElementVisitor) = visitor.visitWord(this)

  val word: String = this.findChildByType<PsiElement>(BazelQueryTokenSets.WORDS)?.text ?: ""
}
