package org.jetbrains.bazel.java.coverage

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.run.coverage.BazelCoverageClassNodeDecoratorPsiElementClassifier

class JavaBazelCoverageClassNodeDecoratorPsiElementClassifier : BazelCoverageClassNodeDecoratorPsiElementClassifier {
  override fun shouldShowCoverageForElement(psiElement: PsiElement): Boolean = psiElement is PsiClass
}
