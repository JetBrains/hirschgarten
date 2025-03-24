package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.references.BazelqueryFunctionReference

class BazelqueryCommand(node: ASTNode) : BazelqueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitCommand(this)

  val name: PsiElement?
    get() = findChildByType(BazelqueryTokenSets.COMMANDS)

  override fun getOwnReferences(): Collection<BazelqueryFunctionReference> =
      name?.let { listOf(BazelqueryFunctionReference(this)) } ?: emptyList()
}
