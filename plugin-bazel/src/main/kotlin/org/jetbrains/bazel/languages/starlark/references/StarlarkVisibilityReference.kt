package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.getCompletionLookupElemenent
import org.jetbrains.bazel.target.targetUtils

class StarlarkVisibilityReference(element: StarlarkStringLiteralExpression) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), false) {
  override fun resolve(): PsiElement? = null

  override fun getVariants(): Array<LookupElement> {
    val project = element.project
    if (!project.isBazelProject) return emptyArray()
    val icon = PlatformIcons.VARIABLE_ICON
    val shortTargets = project.targetUtils.allTargetsAndLibrariesLabels
    val targetVisibilities = shortTargets.map { getCompletionLookupElemenent(it, icon) }.toTypedArray()
    val pkgVisibilities = shortTargets.map { getCompletionLookupElemenent("$it:__pkg__", icon) }.toTypedArray()
    val subpkgVisibilities = shortTargets.map { getCompletionLookupElemenent("$it:__subpackages__", icon) }.toTypedArray()

    val predefined =
      listOf(
        getCompletionLookupElemenent("//visibility:public", icon, 1.0),
        getCompletionLookupElemenent("//visibility:private", icon, 1.0),
      )
    return pkgVisibilities + subpkgVisibilities + targetVisibilities + predefined
  }
}
