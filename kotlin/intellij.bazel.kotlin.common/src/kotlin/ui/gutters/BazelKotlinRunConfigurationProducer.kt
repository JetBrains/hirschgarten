package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunConfigurationProducer
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinMainFunctionDetector
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

@ApiStatus.Internal
class BazelKotlinRunConfigurationProducer : BazelJavaRunConfigurationProducer() {
  override fun isMainMethod(element: PsiElement): Boolean {
    val function = element.getParentOfType<KtNamedFunction>(strict = true) ?: return false
    val detector = KotlinMainFunctionDetector.getInstanceDumbAware(element.project)
    return detector.isMain(function)
  }

  override fun getContainingClassFqn(element: PsiElement): String? {
    val classOrObject = element.getNonStrictParentOfType<KtClassOrObject>() ?: return null
    return KotlinPsiHeuristics.getJvmName(classOrObject)
  }

  override fun toPsiClassOrMethod(element: PsiNameIdentifierOwner): Pair<PsiClass?, PsiMethod?> = when (element) {
    is KtClassOrObject -> element.toLightClass() to null
    is KtNamedFunction -> null to element.getRepresentativeLightMethod()
    else -> null to null
  }
}
