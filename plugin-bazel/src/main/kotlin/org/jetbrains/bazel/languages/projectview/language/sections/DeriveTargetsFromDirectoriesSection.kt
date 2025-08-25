package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class DeriveTargetsFromDirectoriesSection : ScalarSection<Boolean>() {
  override val name = NAME
  override val default = false
  override val sectionKey = KEY
  override val completionProvider = SimpleCompletionProvider(VARIANTS)
  override val doc =
    "If set to true, relevant project targets will be automatically derived from the " +
      "directories during sync. Note that directories from all transitive imports are " +
      "included."

  override fun fromRawValue(rawValue: String): Boolean? = rawValue.toBooleanStrictOrNull()

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = variantsAnnotation(element, holder, VARIANTS)

  companion object {
    const val NAME = "derive_targets_from_directories"
    val KEY = SectionKey<Boolean>(NAME)
    val VARIANTS = listOf("true", "false")
  }
}
