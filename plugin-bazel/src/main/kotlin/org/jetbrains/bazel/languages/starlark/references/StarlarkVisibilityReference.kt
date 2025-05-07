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
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.languages.starlark.psi.expressions.getCompletionLookupElemenent

class StarlarkVisibilityReference(element: StarlarkStringLiteralExpression) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), false) {
  override fun resolve(): PsiElement? = null

  override fun getVariants(): Array<LookupElement> {
    val project = element.project
    if (!project.isBazelProject) return emptyArray()
    val icon = PlatformIcons.VARIABLE_ICON
    val shortTargets = project.targetUtils.allTargetsAndLibrariesLabels
    val targetVisibilities = shortTargets.map { it.getCompletionLookupElemenent(icon) }.toTypedArray()
    val pkgVisibilities = shortTargets.map { "$it:__pkg__".getCompletionLookupElemenent(icon) }.toTypedArray()
    val subpkgVisibilities = shortTargets.map { "$it:__subpackages__".getCompletionLookupElemenent(icon) }.toTypedArray()

    val predefined =
      listOf(
        "//visibility:public".getCompletionLookupElemenent(icon, 1.0),
        "//visibility:private".getCompletionLookupElemenent(icon, 1.0),
      )
    return pkgVisibilities + subpkgVisibilities + targetVisibilities + predefined
  }
  }
