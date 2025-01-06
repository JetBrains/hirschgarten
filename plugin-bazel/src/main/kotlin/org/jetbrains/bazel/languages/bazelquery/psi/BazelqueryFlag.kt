package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode

class BazelqueryFlag(node: ASTNode) : BazelqueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitFlag(this)
}
