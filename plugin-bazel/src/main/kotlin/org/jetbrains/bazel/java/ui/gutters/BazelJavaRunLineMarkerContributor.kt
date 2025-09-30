package org.jetbrains.bazel.java.ui.gutters

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

@PublicApi
open class BazelJavaRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  override fun PsiElement.shouldAddMarker(): Boolean =
    parentOfType<PsiNameIdentifierOwner>()
      ?.takeIf { it.nameIdentifier == this }
      ?.takeIf { it.isClassOrMethod() } != null &&
      // todo replace with is in source root check
      containingFile.virtualFile?.fileSystem !is JarFileSystem

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1316
  override fun getSingleTestFilter(element: PsiElement): String? {
    val psiIdentifier = element.getStrictParentOfType<PsiNameIdentifierOwner>()
    if (psiIdentifier?.isMethod() == true) {
      val methodName = psiIdentifier.getMethodName()
      return if (element.project.useJetBrainsTestRunner()) {
        val fullyQualifiedClassName = psiIdentifier.getFullyQualifiedClassName() ?: return null
        "$fullyQualifiedClassName:$methodName"
      } else {
        val className = psiIdentifier.getClassName() ?: return methodName
        "$className.$methodName$"
      }
    } else {
      return if (element.project.useJetBrainsTestRunner()) {
        psiIdentifier?.getFullyQualifiedClassName()
      } else {
        psiIdentifier?.getClassName()
      }
    }
  }

  protected open fun PsiNameIdentifierOwner.getMethodName(): String? = if (isMethod()) name else null

  protected open fun PsiNameIdentifierOwner.getClassName(): String? = getNonStrictParentOfType<PsiClass>()?.name

  protected open fun PsiNameIdentifierOwner.getFullyQualifiedClassName(): String? {
    val psiClass = getNonStrictParentOfType<PsiClass>() ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  protected open fun PsiNameIdentifierOwner.isClassOrMethod(): Boolean = this is PsiClass || this is PsiMethod

  protected open fun PsiNameIdentifierOwner.isMethod(): Boolean = this is PsiMethod
}
