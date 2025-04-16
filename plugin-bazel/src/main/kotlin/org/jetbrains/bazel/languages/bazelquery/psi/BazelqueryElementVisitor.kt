package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.psi.PsiElementVisitor

open class BazelqueryElementVisitor : PsiElementVisitor() {
  fun visitCommand(node: BazelqueryCommand) {
    visitElement(node)
  }

  fun visitWord(node: BazelqueryWord) {
    visitElement(node)
  }

  fun visitFlag(node: BazelqueryFlag) {
    visitElement(node)
  }

  fun visitFlagVal(node: BazelqueryFlagVal) {
    visitElement(node)
  }

  fun visitInteger(node: BazelqueryInteger) {
    visitElement(node)
  }
}
