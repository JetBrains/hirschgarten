package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class BazelQueryInteger(node: ASTNode) : BazelQueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelQueryElementVisitor) = visitor.visitInteger(this)
}
