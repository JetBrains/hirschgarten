package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.psi.PsiElementVisitor

open class BazelrcElementVisitor : PsiElementVisitor() {
  fun visitLine(node: BazelrcLine) {
    visitElement(node)
  }
}
