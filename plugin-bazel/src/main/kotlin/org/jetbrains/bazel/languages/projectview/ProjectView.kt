package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import kotlin.io.path.Path

class ProjectView(rawSections: List<RawItem>, private val project: Project) {
  val sections = mutableMapOf<SectionKey<*>, Any>()

  private fun mergeSection(sectionKey: SectionKey<*>, value: Any) {
    if (sectionKey in sections && value is Collection<*>) {
      sections[sectionKey] = (sections[sectionKey] as Collection<*>) + value
    } else {
      sections[sectionKey] = value
    }
  }

  init {
    for (item in rawSections) {
      when (item) {
        is RawSection -> {
          val section = ProjectViewSections.getSectionByName(item.name)
          val parsed = section?.fromRawValues(item.contents) ?: continue
          mergeSection(section.sectionKey, parsed)
        }
        is RawImport -> handleImport(item.path)
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

  inline fun <reified T> getSection(key: SectionKey<T>): T? = sections[key] as? T

  sealed interface RawItem

  data class RawSection(val name: String, val contents: List<String>) : RawItem

  data class RawImport(val path: String) : RawItem

  companion object {
    fun fromProjectViewPsiFile(file: ProjectViewPsiFile): ProjectView {
      val rawSections = mutableListOf<RawItem>()
      val psiSections = file.getSectionsOrImports()
      for (section in psiSections) {
        if (section is ProjectViewPsiSection) {
          val name = section.getKeyword().text.trim()
          val values = section.getItems().map { it.text.trim() }
          rawSections.add(RawSection(name, values))
        } else if (section is ProjectViewPsiImport) {
          val path = section.getImportPath()?.text?.trim() ?: ""
          rawSections.add(RawImport(path))
        }
      }
      return ProjectView(rawSections, file.project)
    }
  }
}
