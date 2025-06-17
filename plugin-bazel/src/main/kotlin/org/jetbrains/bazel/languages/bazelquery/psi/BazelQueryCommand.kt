package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.references.BazelQueryFunctionReference

class BazelQueryCommand(node: ASTNode) : BazelQueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelQueryElementVisitor) = visitor.visitCommand(this)

  val name: PsiElement?
    get() = findChildByType(BazelQueryTokenSets.COMMANDS)

  override fun getOwnReferences(): Collection<BazelQueryFunctionReference> =
    name?.let { listOf(BazelQueryFunctionReference(this)) } ?: emptyList()
}
