package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.FiletypeCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ListSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey
import java.nio.file.Path

class ImportRunConfigurationsSection : ListSection<List<Path>>() {
  override val name = NAME
  override val sectionKey = KEY
  override val completionProvider = FiletypeCompletionProvider(".xml")
  override val doc =
    "A list of XML files which will be imported as run configurations during bazel sync. " +
      "By default these run configurations will be updated during sync to match any changes to the XML."

  override fun fromRawValues(rawValues: List<String>): List<Path> = rawValues.mapNotNull { parseItem(it) }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = annotatePath(element, holder)

  companion object {
    const val NAME = "import_run_configurations"
    val KEY = SectionKey<List<Path>>(NAME)

    private fun parseItem(path: String): Path? =
      try {
        Path.of(path)
      } catch (_: Exception) {
        null
      }
  }
}
