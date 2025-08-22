package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ListSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class SyncFlagsSection : ListSection<List<Flag>>() {
  override val name = NAME
  override val default = emptyList<Flag>()
  override val sectionKey = KEY
  override val completionProvider = FlagCompletionProvider(COMMAND)
  override val doc =
    "A set of flags that get passed to build during all sync actions. Unlike" +
      "'build_flags', these are not used for run configurations, so use 'sync_flags' " +
      "only when necessary, as they can defeat caching."

  override fun fromRawValues(rawValues: List<String>): List<Flag> = rawValues.mapNotNull { Flag.byName(it) }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = annotateFlag(element, holder, COMMAND)

  companion object {
    const val NAME = "sync_flags"
    val KEY = SectionKey<List<Flag>>(NAME)
    const val COMMAND = "sync"
  }
}
