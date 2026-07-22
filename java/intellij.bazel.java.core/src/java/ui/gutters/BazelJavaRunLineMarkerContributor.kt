package org.jetbrains.bazel.java.ui.gutters

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.lang.jvm.util.JvmMainMethodUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.test.createTestFilterDescriptor
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor

@ApiStatus.Internal  // External plugins (e.g., Scala) should implement BazelRunLineMarkerContributor instead
open class BazelJavaRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  override fun getGutterAction(element: PsiElement): GutterAction? {
    if (element.containingFile.virtualFile?.fileSystem is JarFileSystem) return null
    val psiIdentifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true) ?: return null
    if (psiIdentifier.nameIdentifier != element) return null
    val isMethod = psiIdentifier.isMethod()
    val isClass = psiIdentifier.isClass()
    if (!isMethod && !isClass) return null

    if (element.isMainMethod()) {
      return GutterAction()
    }

    val project = element.project
    val fullyQualifiedClassName = psiIdentifier.getFullyQualifiedClassName() ?: return null
    val testFilter = if (isMethod) {
      val methodName = psiIdentifier.getMethodName()
      if (element.project.useJetBrainsTestRunner()) {
        val methodParameterTypes = psiIdentifier.getMethodParameterTypes()
        "$fullyQualifiedClassName:$methodName:$methodParameterTypes"
      } else {
        "$fullyQualifiedClassName.$methodName$"
      }
    }
    else {
      fullyQualifiedClassName
    }
    return GutterAction(runnerActionDescriptor = createTestFilterDescriptor(project, testFilter))
  }

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

  @ApiStatus.Internal
  protected open fun PsiElement.isMainMethod(): Boolean =
    this is PsiMethod && JvmMainMethodUtil.isMainMethod(this) ||
    this is PsiClass && JvmMainMethodUtil.hasMainMethodInHierarchy(this)
}
