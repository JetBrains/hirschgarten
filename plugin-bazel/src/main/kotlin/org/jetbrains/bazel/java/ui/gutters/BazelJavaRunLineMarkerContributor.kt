package org.jetbrains.bazel.java.ui.gutters

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.testing.BazelTestLocationHintProvider
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

@PublicApi
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
    val psiIdentifier = element.getStrictParentOfType<PsiNameIdentifierOwner>()
    if (psiIdentifier?.isMethod() == true) {
      val methodName = psiIdentifier.getMethodName()
      return if (element.project.useJetBrainsTestRunner()) {
        val fullyQualifiedClassName = psiIdentifier.getFullyQualifiedClassName() ?: return null
        val methodParameterTypes = psiIdentifier.getMethodParameterTypes()
        "$fullyQualifiedClassName:$methodName:$methodParameterTypes"
      } else {
        val className = psiIdentifier.getClassName() ?: return methodName
        "$className.$methodName$"
      }
    } else {
      if (element.project.useJetBrainsTestRunner()) {
        return psiIdentifier?.getFullyQualifiedClassName()
      }
      return if (psiIdentifier is PsiClass) {
        psiIdentifier.getFullyQualifiedClassName()
      } else {
        psiIdentifier?.getClassName()
      }
    }
  }

  override fun guessTestLocationHints(psiElement: PsiElement): List<String> {
    val psiIdentifier = psiElement.getStrictParentOfType<PsiNameIdentifierOwner>() ?: return emptyList()
    val fullClassName = psiIdentifier.getFullyQualifiedClassName() ?: return emptyList()
    val shortClassName = guessShortClassNames(fullClassName)
    return if (psiIdentifier.isClass()) {
      listOf(
        BazelTestLocationHintProvider.testSuiteLocationHint("", classname = fullClassName),
        BazelTestLocationHintProvider.testSuiteLocationHint("", parentSuites = shortClassName),
        "java:suite://$fullClassName",
      )
    } else if (psiIdentifier.isMethod()) {
      listOfNotNull(
        psiIdentifier.name?.let { BazelTestLocationHintProvider.testCaseLocationHint(it, classname = fullClassName) },
        psiIdentifier.name?.let { BazelTestLocationHintProvider.testCaseLocationHint(it, parentSuites = shortClassName) },
        "java:test://$fullClassName/${psiIdentifier.name}",
      )
    } else {
      emptyList()
    }
  }

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

  private fun guessShortClassNames(fullClassName: String): List<String> =
    fullClassName // package.package.Class.NestedClass.NestedNested
      .dropWhile { !it.isUpperCase() } // Class.NestedClass.NestedNested
      .split('.') // [Class, NestedClass, NestedNested]

  protected open fun PsiNameIdentifierOwner.getPsiParameters(): Array<out PsiParameter>? =
    (this as? PsiMethod)?.parameterList?.parameters

  protected open fun PsiNameIdentifierOwner.getClassName(): String? = getNonStrictParentOfType<PsiClass>()?.name

  protected open fun PsiNameIdentifierOwner.getFullyQualifiedClassName(): String? {
    val psiClass = getNonStrictParentOfType<PsiClass>() ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  protected open fun PsiNameIdentifierOwner.isClass(): Boolean = this is PsiClass

  protected open fun PsiNameIdentifierOwner.isMethod(): Boolean = this is PsiMethod
}
