package org.jetbrains.bazel.languages.bazelrc.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.flags.OptionEffectTag
import org.jetbrains.bazel.languages.bazelrc.flags.OptionMetadataTag
import org.jetbrains.bazel.languages.bazelrc.highlighting.BazelrcHighlightingColors
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine
import org.jetbrains.bazel.languages.bazelrc.quickfix.DeleteFlagUseFix
import org.jetbrains.bazel.languages.bazelrc.quickfix.RenameFlagNameFix
import kotlin.text.Regex

val flagTokenPattern =
  psiElement(BazelrcTokenTypes.FLAG)
    .withParents(
      BazelrcFlag::class.java,
      BazelrcLine::class.java,
    )!!

private val labelFlagRe = Regex("^--(?:no)?[@/].*$")

@Suppress("UnstableApiUsage")
class BazelrcFlagAnnotator : Annotator {
  val symbolReferenceService = PsiSymbolReferenceService.getService()

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!flagTokenPattern.accepts(element) || isLabel(element)) {
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
          .withFix(DeleteFlagUseFix(element, flag))
          .create()

      isDeprecated(flag, element) ->
        holder
          .newAnnotation(HighlightSeverity.WARNING, "Flag: '${element.text}' is deprecated")
          .range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
          .needsUpdateOnTyping()
          .create()

      isOld(flag, element.text) ->
        holder
          .newAnnotation(HighlightSeverity.INFORMATION, oldAnnotationMessage(flag, element.text))
          .range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
          .needsUpdateOnTyping()
          .withFix(RenameFlagNameFix(element, flag))
          .create()

      isNotApplicable(flag, element) ->
        holder
          .newAnnotation(
            HighlightSeverity.INFORMATION,
            "Flag: '${element.text}' is not applicable to command '${element.parentOfType<BazelrcLine>(false)!!.command}'",
          ).range(element.textRange)
          .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
          .needsUpdateOnTyping()
          .create()

      else -> {}
    }
  }

  private fun oldAnnotationMessage(flag: Flag, flagText: String) =
    when {
      flagText.startsWith("--no") -> "--no"
      else -> "--"
    }.let {
      "'$flagText' is deprecated. use '$it${flag.option.name}' instead."
    }

  private fun isLabel(e: PsiElement) = labelFlagRe.matches(e.text)

  private fun isHidden(flag: Flag) = flag.option.metadataTags.contains(OptionMetadataTag.HIDDEN)

  private fun isDeprecated(flag: Flag, element: PsiElement) =
    when (flag.name) {
      "watchfs" -> PsiTreeUtil.getParentOfType(element, false, BazelrcLine::class.java)?.command == "startup"

      else -> flag.option.metadataTags.contains(OptionMetadataTag.DEPRECATED)
    }

  private fun isNoOp(flag: Flag) = flag.option.effectTags.contains(OptionEffectTag.NO_OP)

  private fun isOld(flag: Flag, name: String) =
    when (flag) {
      is Flag.Boolean, is Flag.TriState ->
        name == "--${flag.option.oldName}" || name == "--no${flag.option.oldName}"

      else -> "--${flag.option.oldName}" == name
    }

  private fun isNotApplicable(flag: Flag, element: PsiElement) =
    PsiTreeUtil
      .getParentOfType(element, true, BazelrcLine::class.java)
      ?.command
      ?.let { it != "common" && !flag.option.commands.contains(it) } ?: false
}
