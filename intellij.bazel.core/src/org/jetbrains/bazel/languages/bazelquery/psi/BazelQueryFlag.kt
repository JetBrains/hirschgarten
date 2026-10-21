package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes

@ApiStatus.Internal
class BazelQueryFlag(node: ASTNode) : BazelQueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelQueryElementVisitor) = visitor.visitFlag(this)

  val name: PsiElement?
    get() = findChildByType(BazelQueryTokenTypes.FLAG) ?: findChildByType(BazelQueryTokenTypes.FLAG_NO_VAL)

  override fun getOwnReferences(): Collection<PsiSymbolReference> =
    serviceOrNull<BazelQueryReferenceSupport>()?.getOwnReferences(this) ?: emptyList()
}
