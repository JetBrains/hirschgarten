package org.jetbrains.bazel.java.junit

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.bazel.junit.PsiElementConfigurationLocator

class JavaPsiElementConfigurationLocator : PsiElementConfigurationLocator {
  override fun getPsiMethod(callerPsiElement: PsiElement): PsiMethod? = runReadAction { callerPsiElement.parent as? PsiMethod }

  override fun getPsiClass(callerPsiElement: PsiElement): PsiClass? = runReadAction { callerPsiElement.parent as? PsiClass }
}
