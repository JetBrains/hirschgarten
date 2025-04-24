package org.jetbrains.bazel.java.ui.gutters

import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.ui.gutters.BspRunLineMarkerContributor

open class BazelJavaRunLineMarkerContributor : BspRunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  override fun PsiElement.shouldAddMarker(): Boolean =
    !isInsideJar() &&
      PsiTreeUtil.getParentOfType(this, PsiNameIdentifierOwner::class.java, true)?.isClassOrMethod() ?: false

  private fun PsiElement.isInsideJar() = containingFile.virtualFile?.fileSystem is JarFileSystem

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1316
  override fun getSingleTestFilter(element: PsiElement): String? {
    val psiIdentifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true)
    val functionName = psiIdentifier?.getFunctionName()
    if (psiIdentifier?.isMethod() == true) {
      val className = psiIdentifier.getClassName() ?: return functionName
      return "$className.$functionName"
    }
    return "$functionName"
  }

  protected open fun PsiNameIdentifierOwner.getFunctionName(): String? = if (this.isClassOrMethod()) tryGetFQN() else null

  protected open fun PsiNameIdentifierOwner.getClassName(): String? = PsiTreeUtil.getParentOfType(this, PsiClass::class.java, true)?.name

  protected open fun PsiNameIdentifierOwner.tryGetFQN(): String? =
    if (this is PsiClass) {
      qualifiedName
    } else {
      name
    }

  protected open fun PsiNameIdentifierOwner.isClassOrMethod(): Boolean = this is PsiClass || this is PsiMethod

  protected open fun PsiNameIdentifierOwner.isMethod(): Boolean = this is PsiMethod
}
