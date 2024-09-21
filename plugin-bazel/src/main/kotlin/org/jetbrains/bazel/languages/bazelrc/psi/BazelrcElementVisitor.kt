package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.psi.PsiElementVisitor

open class BazelrcElementVisitor : PsiElementVisitor() {
  fun visitImport(node: BazelrcImport) {
    visitElement(node)
  }

  fun visitLine(node: BazelrcLine) {
    visitElement(node)
  }

  fun visitFlag(node: BazelrcFlag) {
    visitElement(node)
  }
}
