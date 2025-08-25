package org.jetbrains.bazel.languages.projectview.language

import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

class ProjectView(rawSections: List<Pair<String, List<String>>>) {
  val sections = mutableMapOf<SectionKey<*>, Any>()

  init {
    for ((name, values) in rawSections) {
      val section = ProjectViewSections.getSectionByName(name)
      val parsed = section?.fromRawValues(values) ?: continue
      sections[section.sectionKey] = parsed
    }
  }

  inline fun <reified T> getSection(key: SectionKey<T>): T? = sections[key] as T

  companion object {
    fun fromProjectViewPsiFile(file: ProjectViewPsiFile): ProjectView {
      val rawSections = mutableListOf<Pair<String, List<String>>>()
      val psiSections = file.getSections()
      for (section in psiSections) {
        val name = section.getKeyword().text.trim()
        val values = section.getItems().map { it.text.trim() }
        rawSections.add(Pair(name, values))
      }
      return ProjectView(rawSections)
    }
  }
}
