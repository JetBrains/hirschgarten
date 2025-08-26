package org.jetbrains.bazel.languages.projectview.language.sections.presets

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.commands
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ListSection
import org.jetbrains.bazel.languages.projectview.language.sections.SyncFlagsSection.Companion.COMMAND

abstract class FlagListSection(private val command: String) : ListSection<List<Flag>>() {
  final override val completionProvider = FlagCompletionProvider(command)

  final override fun fromRawValues(rawValues: List<String>): List<Flag> = rawValues.mapNotNull { Flag.byName(it) }

  final override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    val flag = Flag.byName(element.text)
    if (flag == null) {
      val message = ProjectViewBundle.getMessage("annotator.unknown.flag.error", element.text)
      holder.annotateError(element, message)
      return
    }
    if (command !in flag.commands()) {
      val message = ProjectViewBundle.getMessage("annotator.flag.not.allowed.here.error", element.text, COMMAND)
      holder.annotateError(element, message)
    }
  }
}
