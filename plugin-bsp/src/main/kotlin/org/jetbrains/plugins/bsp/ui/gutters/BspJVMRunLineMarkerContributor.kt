package org.jetbrains.plugins.bsp.ui.gutters

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId

class BspJVMRunLineMarkerContributor : BspRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean =
    !isInsideJar() &&
      getStrictParentOfType<PsiNameIdentifierOwner>()
        ?.isClassOrMethod() ?: false

  private fun PsiElement.isInsideJar() = containingFile.virtualFile?.url?.startsWith("jar://") ?: false

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1316
  override fun getSingleTestFilter(element: PsiElement): String? =
    if (element.project.buildToolId == BuildToolId("bazelbsp")) {
      element.getStrictParentOfType<PsiNameIdentifierOwner>()?.getFunctionName()
    } else {
      null
    }

  private fun PsiNameIdentifierOwner.getFunctionName(): String? = if (this.isClassOrMethod()) this.name else null

  private fun PsiNameIdentifierOwner.isClassOrMethod(): Boolean =
    this is KtClassOrObject || this is KtNamedFunction || this is PsiClass || this is PsiMethod
}
