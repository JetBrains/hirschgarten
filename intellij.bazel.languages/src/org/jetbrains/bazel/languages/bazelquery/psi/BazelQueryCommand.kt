package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets

@ApiStatus.Internal
class BazelQueryCommand(node: ASTNode) : BazelQueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelQueryElementVisitor) = visitor.visitCommand(this)

  val name: PsiElement?
    get() = findChildByType(BazelQueryTokenSets.COMMANDS)

  override fun getOwnReferences(): Collection<PsiSymbolReference> =
    serviceOrNull<BazelQueryReferenceSupport>()?.getOwnReferences(this) ?: emptyList()
}
