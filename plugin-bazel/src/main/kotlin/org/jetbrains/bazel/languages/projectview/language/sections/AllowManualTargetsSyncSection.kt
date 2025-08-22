package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class AllowManualTargetsSyncSection : ScalarSection<Boolean>() {
  override val name = NAME
  override val default = false
  override val sectionKey = KEY
  override val completionProvider = SimpleCompletionProvider(VARIANTS)
  override val doc =
    "If this option is set to true, build targets labeled with the manual tag can be synced " +
      "which will enable running and debugging them. Enabling this option will implictly enable " +
      "the shard_sync option to allow labels expansion for Bazel query especially in cases " +
      "where derive_targets_from_directories is disabled."

  override fun fromRawValue(rawValue: String): Boolean? = rawValue.toBooleanStrictOrNull()

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = variantsAnnotation(element, holder, VARIANTS)

  companion object {
    const val NAME = "allow_manual_targets_sync"
    val KEY = SectionKey<Boolean>(NAME)
    val VARIANTS = listOf("true", "false")
  }
}
