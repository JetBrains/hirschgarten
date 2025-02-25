package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes

class BazelrcLine(node: ASTNode) : BazelrcBaseElement(node) {
  override fun acceptVisitor(visitor: BazelrcElementVisitor) = visitor.visitLine(this)

  val config: String?
    get() = this.findChildByType<PsiElement>(BazelrcTokenTypes.CONFIG)?.text

  val command: String
    get() = this.findChildByType<PsiElement>(COMMAND_TOKENS)?.text ?: ""

  val flags: Array<BazelrcFlag>
    get() = this.findChildrenByClass(BazelrcFlag::class.java)

  companion object {
    val COMMAND_TOKENS = TokenSet.create(BazelrcTokenTypes.COMMAND)
  }
}
