package org.jetbrains.bazel.languages.projectview.language.sections

import com.android.tools.idea.gradle.structure.model.meta.annotateWithError
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.commands
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ListSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class BuildFlagsSection : ListSection<List<Flag>>() {
  override val name = NAME
  override val default = emptyList<Flag>()
  override val sectionKey = KEY
  override val completionProvider = FlagCompletionProvider("build")
  override val doc =
    "A set of flags that get passed to all build command invocations as arguments. This" +
      "includes both sync and run configuration actions."

  override fun fromRawValues(rawValues: List<String>): List<Flag> = rawValues.mapNotNull(::parseItem)

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    val flag = parseItem(element.text)
    if (flag == null) {
      val message = ProjectViewBundle.getMessage("annotator.unknown.flag.error", element.text)
      holder.annotateError(element, message)
      return
    }
    if ("build" !in flag.commands()) {
      val message = ProjectViewBundle.getMessage("annotator.flag.not.allowed.here.error", element.text, "build")
      holder.annotateError(element, message)
    }
  }

  companion object {
    const val NAME = "build_flags"
    val KEY = SectionKey<List<Flag>>(NAME)

    private fun parseItem(item: String): Flag? = Flag.byName(item)
  }
}
