package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiParameter
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinMainFunctionDetector
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeInsight.KotlinRunLineMarkerHider
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import javax.swing.Icon

@ApiStatus.Internal
open class BazelKotlinRunLineMarkerContributor : BazelJavaRunLineMarkerContributor() {
  /**
   * See [DefaultKotlinRunLineMarkerHider] for explanation
   */
  override fun getRunLineMarkerIcon(element: PsiElement): Icon? {
    return if (isKotlinMainFunction(element)) {
      executeRunLineMarkerIcon
    }
    else {
      null
    }
  }

  fun isKotlinMainFunction(element: PsiElement): Boolean {
    val function = element.parent as? KtNamedFunction ?: return false
    val detector = KotlinMainFunctionDetector.getInstanceDumbAware(element.project)
    return detector.isMain(function)
  }

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

/**
 * KotlinRunLineMarkerContributor adds its run gutter via `getSlowInfo`, while we do so via `getInfo`.
 * Because of this, IDEA can't merge the two run gutters into one, like it does normally, e.g., with Java run gutters.
 * So we cannot be setting the `icon` to `null` in this case, otherwise our gutter won't be shown.
 * On top of that, Kotlin's run gutter needs to be hidden, otherwise there will be two gutter icons!
 */
internal class DefaultKotlinRunLineMarkerHider : KotlinRunLineMarkerHider {
  override fun shouldHideRunLineMarker(element: PsiElement): Boolean =
    element.project.isBazelProject
}
