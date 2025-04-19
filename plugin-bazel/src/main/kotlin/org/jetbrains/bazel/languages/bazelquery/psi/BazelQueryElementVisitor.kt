package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.psi.PsiElementVisitor

open class BazelQueryElementVisitor : PsiElementVisitor() {
  fun visitCommand(node: BazelQueryCommand) {
    visitElement(node)
  }

  fun visitWord(node: BazelQueryWord) {
    visitElement(node)
  }

  fun visitFlag(node: BazelQueryFlag) {
    visitElement(node)
  }

  fun visitFlagVal(node: BazelQueryFlagVal) {
    visitElement(node)
  }

  fun visitInteger(node: BazelQueryInteger) {
    visitElement(node)
  }
}
