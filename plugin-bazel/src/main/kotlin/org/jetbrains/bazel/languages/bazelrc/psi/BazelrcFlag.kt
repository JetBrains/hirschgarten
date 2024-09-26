package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.bazel.languages.bazelrc.references.BazelrcFlagNameReference

class BazelrcFlag(node: ASTNode) : BazelrcBaseElement(node) {
  override fun acceptVisitor(visitor: BazelrcElementVisitor) = visitor.visitFlag(this)

  val name: PsiElement?
    get() = findChildByType(BazelrcTokenTypes.FLAG)

  override fun getOwnReferences(): Collection<BazelrcFlagNameReference> =
    name?.let { listOf(BazelrcFlagNameReference(this)) } ?: emptyList()
}
