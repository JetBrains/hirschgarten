package org.jetbrains.bazel.languages.bazelrc.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.bazel.languages.bazelrc.highlighting.BazelrcHighlightingColors
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine

val flagTokenPattern =
  psiElement(BazelrcTokenTypes.FLAG)
    .withParents(
      BazelrcFlag::class.java,
      BazelrcLine::class.java,
    )!!

@Suppress("UnstableApiUsage")
class BazelrcFlagAnnotator : Annotator {
  val symbolReferenceService = PsiSymbolReferenceService.getService()

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when {
      isUnknownFlag(element) ->
        holder
          .newAnnotation(HighlightSeverity.WARNING, "Unknown flag: '${element.text}'")
          .range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.UNKNOWN_FLAG)
          .needsUpdateOnTyping()
          .create()

      else -> {}
    }
  }

  private fun isUnknownFlag(element: PsiElement): Boolean {
    if (!flagTokenPattern.accepts(element)) {
      return false
    }

    // check if the parent resolves any `Symbol` reference
    return (element.parent as? BazelrcFlag)
      ?.let(symbolReferenceService::getReferences)
      ?.flatMap(PsiSymbolReference::resolveReference)
      ?.firstOrNull()
      ?.let { true } != true
  }
}
