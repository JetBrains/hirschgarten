package org.jetbrains.bazel.languages.bazelrc.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.openapi.diagnostic.logger
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.flags.OptionEffectTag
import org.jetbrains.bazel.languages.bazelrc.flags.OptionMetadataTag
import org.jetbrains.bazel.languages.bazelrc.highlighting.BazelrcHighlightingColors
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType

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
    if (!flagTokenPattern.accepts(element)) {
      return
    }

    val flag =
      (element.parent as? BazelrcFlag)
        ?.let(symbolReferenceService::getReferences)
        ?.flatMap(PsiSymbolReference::resolveReference)
        ?.firstOrNull()
        ?.let { it as? BazelFlagSymbol }
        ?.flag

    if (flag == null) {
      holder
        .newAnnotation(HighlightSeverity.WARNING, "Unknown flag: '${element.text}'")
        .range(element.textRange)
        .textAttributes(BazelrcHighlightingColors.UNKNOWN_FLAG)
        .needsUpdateOnTyping()
        .create()
      return
    }

    when {
      isHidden(flag) ->
        holder
          .newAnnotation(HighlightSeverity.INFORMATION, "Undocumented flag: '${element.text}'")
          .range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.UNKNOWN_FLAG)
          .needsUpdateOnTyping()
          .create()

      isNoOp(flag) ->
        holder
          .newAnnotation(HighlightSeverity.INFORMATION, "Flag: '${element.text}' doesn't do anything")
          .range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.NOOP_FLAG)
          .needsUpdateOnTyping()
          .create()

      isDeprecated(flag, element) ->
        holder
          .newAnnotation(HighlightSeverity.WARNING, "Flag: '${element.text}' is deprecated")
          .range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
          .needsUpdateOnTyping()
          .create()

      isOld(flag, element.textRange.substring(element.containingFile.text)) ->
        holder
          .newAnnotation(HighlightSeverity.INFORMATION, "Flag: '${element.text}'")
          .range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
          .needsUpdateOnTyping()
          .create()

      else -> {}
    }
  }

  private fun isHidden(flag: Flag) = flag.option.metadataTags.contains(OptionMetadataTag.HIDDEN)

  private fun isDeprecated(flag: Flag, element: PsiElement) =
    when (flag.name) {
      "watchfs" -> element.parentOfType<BazelrcLine>(false)?.command == "startup"

      else -> flag.option.metadataTags.contains(OptionMetadataTag.DEPRECATED)
    }

  private fun isNoOp(flag: Flag) = flag.option.effectTags.contains(OptionEffectTag.NO_OP)

  private fun isOld(flag: Flag, name: String) = "--${flag.option.oldName}" == name

  private companion object {
    private val log = logger<BazelrcFlagAnnotator>()
  }
}
