package org.jetbrains.bazel.java.ui.gutters

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor

open class BazelJavaRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  override fun PsiElement.shouldAddMarker(): Boolean =
    parentOfType<PsiNameIdentifierOwner>()
      ?.takeIf { it.nameIdentifier == this }
      ?.takeIf { it.isClass() || it.isMethod() } != null &&
      // todo replace with is in source root check
      containingFile.virtualFile?.fileSystem !is JarFileSystem

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1316
  override fun getSingleTestFilter(element: PsiElement): String? {
    val psiIdentifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true) ?: return null
    val className = psiIdentifier.getFullyQualifiedClassName() ?: return null
    return if (psiIdentifier.isMethod()) {
      val methodName = psiIdentifier.getMethodName()
      if (element.project.useJetBrainsTestRunner()) {
        val methodParameterTypes = psiIdentifier.getMethodParameterTypes()
        "$className:$methodName:$methodParameterTypes"
      } else {
        "${className.normalizeNestedClassSeparator()}.$methodName$"
      }
    } else {
      if (element.project.useJetBrainsTestRunner()) {
        className
      } else {
        className.normalizeNestedClassSeparator()
      }
    }
  }

  /**
   * Any `$` separating a nested class is replaced with `.`, because `$` would otherwise be interpreted as a regex end-of-input anchor.
   */
  private fun String.normalizeNestedClassSeparator(): String = replace("$", ".")

  @ApiStatus.Internal
  protected open fun PsiNameIdentifierOwner.getMethodName(): String? = if (isMethod()) name else null

  /**
   * See [JUnit docs](https://docs.junit.org/5.2.0/api/org/junit/platform/engine/discovery/MethodSelector.html#getMethodParameterTypes())
   */
  private fun PsiNameIdentifierOwner.getMethodParameterTypes(): String =
    getPsiParameters().orEmpty().map { it.type }.mapNotNull { type ->
      if (type is PsiClassType) {
        // canonicalText will include type arguments if they are present, avoid that in simple cases
        type.resolve()?.qualifiedName
      } else {
        type.canonicalText
      }
    }.joinToString(separator = ",")

  @ApiStatus.Internal
  protected open fun PsiNameIdentifierOwner.getPsiParameters(): Array<out PsiParameter>? =
    (this as? PsiMethod)?.parameterList?.parameters

  @ApiStatus.Internal
  protected open fun PsiElement.getFullyQualifiedClassName(): String? {
    val psiClass = PsiTreeUtil.getParentOfType(this, PsiClass::class.java, false) ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  @ApiStatus.Internal
  protected open fun PsiNameIdentifierOwner.isClass(): Boolean = this is PsiClass

  @ApiStatus.Internal
  protected open fun PsiNameIdentifierOwner.isMethod(): Boolean = this is PsiMethod
}
