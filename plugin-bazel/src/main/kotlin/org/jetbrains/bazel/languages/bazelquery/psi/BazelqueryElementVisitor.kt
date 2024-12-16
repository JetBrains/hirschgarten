package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.psi.PsiElementVisitor

open class BazelqueryElementVisitor : PsiElementVisitor() {
    fun visitCommand(node: BazelqueryCommand) {
        visitElement(node)
    }

    fun visitPrompt(node: BazelqueryPrompt) {
        visitElement(node)
    }

    fun visitWord(node: BazelqueryWord) {
        visitElement(node)
    }

    fun visitQueryVal(node: BazelqueryQueryVal) {
        visitElement(node)
    }
}
