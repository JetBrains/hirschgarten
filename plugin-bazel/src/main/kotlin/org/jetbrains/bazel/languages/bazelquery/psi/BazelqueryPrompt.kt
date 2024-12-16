package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode

class BazelqueryPrompt(node: ASTNode) : BazelqueryBaseElement(node) {
    override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitPrompt(this)
}
