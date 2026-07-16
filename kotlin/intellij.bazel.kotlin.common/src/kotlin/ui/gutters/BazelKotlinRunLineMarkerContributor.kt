package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiParameter
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinMainFunctionDetector
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

@ApiStatus.Internal
open class BazelKotlinRunLineMarkerContributor : BazelJavaRunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    return if (isKotlinMainFunction(element)) {
      null
    }
    else {
      super.getInfo(element)
    }
  }

  override fun getSlowInfo(element: PsiElement): Info? {
    return if (isKotlinMainFunction(element)) {
      super.getInfo(element)
    }
    else {
      null
    }
  }

  /**
   * KotlinRunLineMarkerContributor adds its run gutter via `getSlowInfo`, while we do so via `getInfo`.
   * IDEA can't merge the two run gutters unless it's the same type.
   */
  protected fun isKotlinMainFunction(element: PsiElement): Boolean {
    val function = element.parent as? KtNamedFunction ?: return false
    val detector = KotlinMainFunctionDetector.getInstanceDumbAware(element.project)
    return detector.isMain(function)
  }

  override fun PsiNameIdentifierOwner.getClassName(): String? = this.getNonStrictParentOfType<KtClassOrObject>()?.name

  override fun PsiElement.getFullyQualifiedClassName(): String? {
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
