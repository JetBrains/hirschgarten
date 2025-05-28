package org.jetbrains.bazel.java.ui.gutters

import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.testing.BazelTestLocationHintProvider
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor

@PublicApi
open class BazelJavaRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  override fun PsiElement.shouldAddMarker(): Boolean =
    !isInsideJar() &&
      children.isEmpty() &&
      this.isClassOrMethodIdentifier()

  private fun PsiElement.isInsideJar() = containingFile.virtualFile?.fileSystem is JarFileSystem

  override fun getSingleTestFilter(element: PsiElement): String? {
    val psiIdentifier = element.getPsiIdentifierOwner() ?: return null
    val dottedClassName = psiIdentifier.getClassFQN() ?: return null
    return when {
      psiIdentifier.isClass() -> dottedClassName
      psiIdentifier.isMethod() -> "$dottedClassName.${psiIdentifier.name}$"
      else -> null
    }
  }

  override fun guessTestLocationHints(psiElement: PsiElement): List<String> {
    val psiIdentifier = psiElement.getPsiIdentifierOwner() ?: return emptyList()
    val fullClassName = psiIdentifier.getClassFQN() ?: return emptyList()
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

  private fun guessShortClassNames(fullClassName: String): List<String> =
    fullClassName // package.package.Class.NestedClass.NestedNested
      .dropWhile { !it.isUpperCase() } // Class.NestedClass.NestedNested
      .split('.') // [Class, NestedClass, NestedNested]

  override fun isTestSuite(psiElement: PsiElement): Boolean = psiElement.getPsiIdentifierOwner()?.isClass() ?: false

  protected open fun PsiNameIdentifierOwner.getClassName(): String? = PsiTreeUtil.getParentOfType(this, PsiClass::class.java, true)?.name

  protected open fun PsiNameIdentifierOwner.getClassFQN(): String? =
    PsiTreeUtil.getParentOfType(this, PsiClass::class.java, false)?.qualifiedName

  private fun PsiNameIdentifierOwner.isClassOrMethod(): Boolean = this.isClass() || this.isMethod()

  protected open fun PsiNameIdentifierOwner.isClass(): Boolean = this is PsiClass

  protected open fun PsiNameIdentifierOwner.isMethod(): Boolean = this is PsiMethod

  private fun PsiElement.getPsiIdentifierOwner(): PsiNameIdentifierOwner? =
    PsiTreeUtil.getParentOfType(this, PsiNameIdentifierOwner::class.java, true)

  private fun PsiElement.isClassOrMethodIdentifier(): Boolean =
    (this.parent as? PsiNameIdentifierOwner)?.let {
      it.isClassOrMethod() && it.identifyingElement == this
    } ?: false
}
