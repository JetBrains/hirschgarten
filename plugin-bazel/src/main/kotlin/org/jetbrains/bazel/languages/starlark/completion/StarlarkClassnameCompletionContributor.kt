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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
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
    val psiPattern =
      psiElement()
        .withLanguage(StarlarkLanguage)
        .inside(
          psiElement(StarlarkStringLiteralExpression::class.java)
            .with(
              object : PatternCondition<StarlarkStringLiteralExpression>("isClassnameValue") {
                override fun accepts(element: StarlarkStringLiteralExpression, context: ProcessingContext?): Boolean =
                  element.isClassnameValue()
              },
            ),
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
    val prefix = fragments.dropLast(1).joinToString(".")

    val psiFacade = JavaPsiFacade.getInstance(element.project)
    val scope = GlobalSearchScope.allScope(element.project)

    // Handle packages and their top-level classes
    psiFacade.findPackage(prefix)?.let { packageToSearch ->
      resultSet.addAllElements(getClassesVariants(packageToSearch.classes))
      resultSet.addAllElements(getSubPackagesVariants(packageToSearch.subPackages))
    }

    // Handle nested classes
    psiFacade.findClass(prefix, scope)?.let { classToSearch ->
      resultSet.addAllElements(getClassesVariants(classToSearch.innerClasses))
    }
  }

  private fun getSubPackagesVariants(subpackages: Array<PsiPackage>): List<LookupElement> =
    subpackages.map { subPackage ->
      getCompletionLookupElemenent(
        name = "${subPackage.qualifiedName}.",
        icon = PlatformIcons.PACKAGE_ICON,
        priority = 1.0,
        presentableText = "${subPackage.name}.",
        tailText = " (${subPackage.qualifiedName})",
        isExpressionFinished = false,
      )
    }

  private fun getClassesVariants(classes: Array<PsiClass>): List<LookupElement> =
    classes
      .filter { it.isCompletableClass() }
      .flatMap { psiClass ->
        val result = mutableListOf<LookupElement>()
        psiClass.qualifiedName?.let { qualifiedName ->
          val name = qualifiedName.replace('$', '.')

          // Add the class itself
          result.add(
            getCompletionLookupElemenent(
              name = name,
              icon = PlatformIcons.CLASS_ICON,
              priority = 1.0,
              presentableText = psiClass.name,
              tailText = " ($name)",
            ),
          )

          // Add the class as a path segment if it has completable inner classes
          if (psiClass.innerClasses.any { it.isCompletableClass() }) {
            result.add(
              getCompletionLookupElemenent(
                name = "$name.",
                icon = PlatformIcons.CLASS_ICON,
                priority = 1.0,
                presentableText = "${psiClass.name}.",
                tailText = " ($name)",
                isExpressionFinished = false,
              ),
            )
          }
        }
        result
      }

  private fun PsiClass.isCompletableClass(): Boolean {
    // Filter out interfaces and abstract classes
    if (this.isInterface || this.hasModifierProperty(PsiModifier.ABSTRACT)) return false

    // Filter out synthetic classes
    if (name == "WhenMappings" || name == "DefaultImpls") return false

    return true
  }
}
