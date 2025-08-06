package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPackage
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.getCompletionLookupElemenent

class StarlarkClassnameCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, StarlarkClassnameCompletionProvider.psiPattern, StarlarkClassnameCompletionProvider())
  }
}

private class StarlarkClassnameCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    val psiPattern = psiElement()
      .withLanguage(StarlarkLanguage)
      .inside(
        psiElement(StarlarkStringLiteralExpression::class.java)
          .with(object : PatternCondition<StarlarkStringLiteralExpression>("isClassnameValue") {
            override fun accepts(element: StarlarkStringLiteralExpression, context: ProcessingContext?): Boolean =
              element.isClassnameValue()
          })
      )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    resultSet: CompletionResultSet,
  ) {
    val element = parameters.position.parent as? StarlarkStringLiteralExpression ?: return
    if (!element.project.isBazelProject) return

    val fullText = element.getStringContents()
    val fragments = fullText.split('.')
    val packagePrefix = fragments.dropLast(1).joinToString(".")

    val packageToSearch = JavaPsiFacade.getInstance(element.project).findPackage(packagePrefix) ?: return

    resultSet.addAllElements(getClassesVariants(packageToSearch))
    resultSet.addAllElements(getSubPackagesVariants(packageToSearch))
  }

  private fun getClassesVariants(packageToSearch: PsiPackage): List<LookupElement> =
    packageToSearch.classes.mapNotNull { psiClass ->
      psiClass.qualifiedName?.let { qualifiedName ->
        getCompletionLookupElemenent(
          qualifiedName,
          PlatformIcons.CLASS_ICON,
          1.0,
        )
      }
    }

  private fun getSubPackagesVariants(packageToSearch: PsiPackage): List<LookupElement> =
    packageToSearch.subPackages.map { subPackage ->
      val subPackageName = subPackage.qualifiedName + "."
      getCompletionLookupElemenent(
        subPackageName,
        PlatformIcons.PACKAGE_ICON,
        1.0,
        false,
      )
    }
}
