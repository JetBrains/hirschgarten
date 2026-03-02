package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode

internal class BazelQueryInteger(node: ASTNode) : BazelQueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelQueryElementVisitor) = visitor.visitInteger(this)
}
