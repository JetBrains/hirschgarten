package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
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
