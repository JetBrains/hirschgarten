package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor

abstract class BazelqueryBaseElement(node: ASTNode) :
    ASTWrapperPsiElement(node),
    BazelqueryElement {

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is BazelqueryElementVisitor) {
            acceptVisitor(visitor)
        } else {
            super.accept(visitor)
        }
    }

    protected abstract fun acceptVisitor(visitor: BazelqueryElementVisitor)
}
