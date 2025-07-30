package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.getCompletionLookupElemenent

class StarlarkClassnameReference(element: StarlarkStringLiteralExpression) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), false) {
  override fun resolve(): PsiElement? {
    val project = element.project
    if (!project.isBazelProject) return null

    val scope = GlobalSearchScope.allScope(project)
    // JavaPsiFacade works here both for Java and Kotlin. It is tested in
    // org.jetbrains.bazel.languages.starlark.references.StarlarkClassnameReferenceTest.
    return JavaPsiFacade.getInstance(project).findClass(element.getStringContents(), scope)
  }

  override fun getVariants(): Array<out Any?> {
    val project = element.project
    if (!project.isBazelProject) return emptyArray()

    val suggestions = mutableListOf<Any>()
    val fragments = element.getStringContents().split('.')
    val packagePrefix = fragments.dropLast(1).joinToString(".")
    val packageToSearch = JavaPsiFacade.getInstance(project).findPackage(packagePrefix)

    if (packageToSearch != null) {
      suggestions.addAll(
        getClassesVariants(packagePrefix, packageToSearch) +
          getSubPackagesVariants(packagePrefix, packageToSearch),
      )
    }
    return suggestions.toTypedArray()
  }

  private fun getClassesVariants(packagePrefix: String, packageToSearch: PsiPackage): List<Any> {
    val suggestions = mutableListOf<Any>()

    for (psiClass in packageToSearch.classes) {
      val qualifiedName = psiClass.qualifiedName.orEmpty()

      if (qualifiedName.startsWith(packagePrefix)) {
        val lookupElement =
          getCompletionLookupElemenent(
            qualifiedName,
            PlatformIcons.CLASS_ICON,
            1.0,
          )

        suggestions.add(lookupElement)
      }
    }
    return suggestions
  }

  private fun getSubPackagesVariants(packagePrefix: String, packageToSearch: PsiPackage): List<Any> {
    val suggestions = mutableListOf<Any>()

    for (subPackage in packageToSearch.subPackages) {
      val subPackageName = subPackage.qualifiedName + "."

      if (subPackageName.startsWith(packagePrefix)) {
        val lookupElement =
          getCompletionLookupElemenent(
            subPackageName,
            PlatformIcons.PACKAGE_ICON,
            1.0,
            true,
          )

        suggestions.add(lookupElement)
      }
    }
    return suggestions
  }
}
