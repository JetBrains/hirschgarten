package org.jetbrains.bazel.languages.projectview.sections.presets

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.ListSection
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.sections.SyncFlagsSection.Companion.COMMAND

abstract class FlagListSection(private vararg val commands: String) : ListSection<List<String>>() {
  final override val completionProvider = FlagCompletionProvider(*commands)

  final override fun fromRawValues(rawValues: List<String>): List<String> = rawValues

  final override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    val flag = Flag.byName(element.text.takeWhile { it != '=' })
    if (flag == null) {
      val message = ProjectViewBundle.getMessage("annotator.unknown.flag.error", element.text)
      holder.annotateWarning(element, message)
      return
    }
    if (commands.any { it !in flag.option.commands }) {
      val message = ProjectViewBundle.getMessage("annotator.flag.not.allowed.here.error", element.text, commands.contentToString())
      holder.annotateWarning(element, message)
    }
  }
}
