package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import kotlin.io.path.Path

class ProjectView(rawSections: List<Pair<String, List<String>>>, private val project: Project) {
  val sections = mutableMapOf<SectionKey<*>, Any>()

  private fun mergeSection(sectionKey: SectionKey<*>, value: Any) {
    if (sectionKey in sections && value is Collection<*>) {
      sections[sectionKey] = (sections[sectionKey] as Collection<*>) + value
    } else {
      sections[sectionKey] = value
    }
  }

  init {
    for ((name, values) in rawSections) {
      if (name == "import") {
        val path = values.firstOrNull() ?: continue
        handleImport(path)
      } else {
        val section = ProjectViewSections.getSectionByName(name)
        val parsed = section?.fromRawValues(values) ?: continue
        mergeSection(section.sectionKey, parsed)
      }
    }
  }

  private fun handleImport(path: String) {
    var nioPath = Path(path)
    if (!nioPath.isAbsolute) {
      val projectRoot = project.rootDir.toNioPath()
      nioPath = projectRoot.resolve(path)
    }
    val virtualFile = VirtualFileManagerImpl.getInstance().findFileByNioPath(nioPath) ?: return
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? ProjectViewPsiFile ?: return
    val otherProjectView = fromProjectViewPsiFile(psiFile)
    for ((sectionKey, value) in otherProjectView.sections) {
      mergeSection(sectionKey, value)
    }
  }

  inline fun <reified T> getSection(key: SectionKey<T>): T? = sections[key] as T

  companion object {
    fun fromProjectViewPsiFile(file: ProjectViewPsiFile): ProjectView {
      val rawSections = mutableListOf<Pair<String, List<String>>>()
      val psiSections = file.getSectionsOrImports()
      for (section in psiSections) {
        if (section is ProjectViewPsiSection) {
          val name = section.getKeyword().text.trim()
          val values = section.getItems().map { it.text.trim() }
          rawSections.add(Pair(name, values))
        } else if (section is ProjectViewPsiImport) {
          val path = section.getImportPath()?.text?.trim() ?: ""
          rawSections.add(Pair("import", listOf(path)))
        }
      }
      return ProjectView(rawSections, file.project)
    }
  }
}
