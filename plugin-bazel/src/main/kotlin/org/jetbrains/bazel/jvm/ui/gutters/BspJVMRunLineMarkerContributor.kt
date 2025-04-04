package org.jetbrains.bazel.jvm.ui.gutters

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class BspJVMRunLineMarkerContributor : BspJavaRunLineMarkerContributor() {
  override fun PsiNameIdentifierOwner.getClassName(): String? =
    this.getStrictParentOfType<KtClassOrObject>()?.name ?: this.getStrictParentOfType<PsiClass>()?.name

  override fun PsiNameIdentifierOwner.isClassOrMethod(): Boolean =
    this is KtClassOrObject || this is KtNamedFunction || this is PsiClass || this is PsiMethod

  override fun PsiNameIdentifierOwner.isMethod(): Boolean = this is KtNamedFunction || this is PsiMethod
}
