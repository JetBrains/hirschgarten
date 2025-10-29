package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiParameter
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class BazelKotlinRunLineMarkerContributor : BazelJavaRunLineMarkerContributor() {
  override fun PsiNameIdentifierOwner.getClassName(): String? = this.getNonStrictParentOfType<KtClassOrObject>()?.name

  override fun PsiNameIdentifierOwner.getFullyQualifiedClassName(): String? {
    val classOrObject = this.getNonStrictParentOfType<KtClassOrObject>() ?: return null
    return KotlinPsiHeuristics.getJvmName(classOrObject)
  }

  override fun PsiNameIdentifierOwner.isClass(): Boolean = this is KtClassOrObject

  override fun PsiNameIdentifierOwner.isMethod(): Boolean = this is KtNamedFunction

  override fun PsiNameIdentifierOwner.getPsiParameters(): Array<out PsiParameter>? {
    if (this !is KtNamedFunction) return null
    val psiMethod = LightClassUtil.getLightClassMethod(this) ?: return null
    return psiMethod.parameterList.parameters
  }
}
