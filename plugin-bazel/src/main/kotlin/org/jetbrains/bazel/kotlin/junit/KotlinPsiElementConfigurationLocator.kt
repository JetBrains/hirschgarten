package org.jetbrains.bazel.kotlin.junit

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.bazel.jvm.junit.PsiElementConfigurationLocator
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class KotlinPsiElementConfigurationLocator : PsiElementConfigurationLocator {
  override fun getPsiMethod(callerPsiElement: PsiElement): PsiMethod? =
    runReadAction { callerPsiElement.parent.getRepresentativeLightMethod() }

  override fun getPsiClass(callerPsiElement: PsiElement): PsiClass? =
    runReadAction { callerPsiElement.getParentOfType<KtClass>(false)?.toLightClass() }
}
