package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode

class BazelqueryQueryVal(node: ASTNode) : BazelqueryBaseElement(node) {
  override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitQueryVal(this)
}
