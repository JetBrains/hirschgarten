package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class BazelKotlinRunLineMarkerContributor : BazelJavaRunLineMarkerContributor() {
  override fun PsiNameIdentifierOwner.getClassName(): String? = this.getStrictParentOfType<KtClassOrObject>()?.name

  override fun PsiNameIdentifierOwner.isClass(): Boolean = this is KtClassOrObject

  override fun PsiNameIdentifierOwner.isMethod(): Boolean = this is KtNamedFunction
}
