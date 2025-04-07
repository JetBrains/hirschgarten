package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.target.targetUtils

class StarlarkVisibilityReference(element: StarlarkStringLiteralExpression) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), false) {
  override fun resolve(): PsiElement? = null

  override fun getVariants(): Array<LookupElement> {
    val project = element.project
    if (!project.isBazelProject) return emptyArray()

    val shortTargets = project.targetUtils.allTargets().map { it.toShortString(project) }
    val targetVisibilities = shortTargets.map { visibilityLookupElement(it) }.toTypedArray()
    val pkgVisibilities = shortTargets.map { visibilityLookupElement("$it:__pkg__") }.toTypedArray()
    val subpkgVisibilities = shortTargets.map { visibilityLookupElement("$it:__subpackages__") }.toTypedArray()

    val predefined =
      listOf(
        visibilityLookupElement("//visibility:public", 1.0),
        visibilityLookupElement("//visibility:private", 1.0),
      )
    return pkgVisibilities + subpkgVisibilities + targetVisibilities + predefined
  }

  private fun visibilityLookupElement(name: String, priority: Double = 0.0): LookupElement =
    PrioritizedLookupElement.withPriority(
      LookupElementBuilder
        .create("\"" + name + "\"")
        .withIcon(PlatformIcons.VARIABLE_ICON)
        .withPresentableText(name),
      priority,
    )
}
