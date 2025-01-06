package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode

class BazelqueryFlagVal(node: ASTNode) : BazelqueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitFlagVal(this)
}
