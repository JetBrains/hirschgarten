package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class IndexAllFilesInDirectoriesSection : ScalarSection<Boolean>() {
  override val name = NAME
  override val sectionKey = KEY
  override val completionProvider = SimpleCompletionProvider(VARIANTS)
  override val doc = "Whether to all index files inside [ProjectViewDirectoriesSection] or just sources of targets"

  override fun fromRawValue(rawValue: String): Boolean? = rawValue.toBooleanStrictOrNull()

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = variantsAnnotation(element, holder, VARIANTS)

  companion object {
    const val NAME = "index_all_files_in_directories"
    val KEY = SectionKey<Boolean>(NAME)
    val VARIANTS = listOf("true", "false")
  }
}
