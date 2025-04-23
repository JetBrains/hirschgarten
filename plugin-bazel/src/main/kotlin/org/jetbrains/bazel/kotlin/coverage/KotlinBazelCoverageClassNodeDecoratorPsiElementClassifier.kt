package org.jetbrains.bazel.kotlin.coverage

import com.intellij.psi.PsiElement
import org.jetbrains.bazel.run.coverage.BazelCoverageClassNodeDecoratorPsiElementClassifier
import org.jetbrains.kotlin.psi.KtClassOrObject

class KotlinBazelCoverageClassNodeDecoratorPsiElementClassifier : BazelCoverageClassNodeDecoratorPsiElementClassifier {
  override fun shouldShowCoverageForElement(psiElement: PsiElement): Boolean = psiElement is KtClassOrObject
}
