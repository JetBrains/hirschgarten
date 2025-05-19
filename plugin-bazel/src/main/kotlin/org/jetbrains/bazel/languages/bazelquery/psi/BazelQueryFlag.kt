package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes
import org.jetbrains.bazel.languages.bazelquery.references.BazelQueryFlagNameReference

class BazelQueryFlag(node: ASTNode) : BazelQueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelQueryElementVisitor) = visitor.visitFlag(this)

  val name: PsiElement?
    get() = findChildByType(BazelQueryTokenTypes.FLAG)

  override fun getOwnReferences(): Collection<BazelQueryFlagNameReference> =
    name?.let { listOf(BazelQueryFlagNameReference(this)) } ?: emptyList()
}
