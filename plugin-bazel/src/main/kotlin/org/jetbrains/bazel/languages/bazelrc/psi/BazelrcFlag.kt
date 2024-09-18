package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.lang.ASTNode

class BazelrcFlag(node: ASTNode) : BazelrcBaseElement(node) {
  override fun acceptVisitor(visitor: BazelrcElementVisitor) = visitor.visitFlag(this)
}
