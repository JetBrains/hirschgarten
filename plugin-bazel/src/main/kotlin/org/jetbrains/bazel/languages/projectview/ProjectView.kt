package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiTryImport

/**
 * Immutable representation of a ProjectView: a map of section keys to parsed values.
 */
data class ProjectView(val sections: Map<SectionKey<*>, Any>) {
  inline fun <reified T> getSection(key: SectionKey<T>): T? {
    var value = sections[key] as? T
    // in case of not existing section, try to get default one
    if (value == null) {
      value = ProjectViewSections.getSectionByName(key.name)?.default as T?
      if (value != null) {
        return value
      }
    }
    return value
  }

  private sealed interface RawItem

  private data class RawSection(val name: String, val contents: List<String>) : RawItem

  private data class RawImport(val path: String, val required: Boolean) : RawItem

  companion object {
    @RequiresReadLock
    @RequiresBackgroundThread(generateAssertion = false)
    fun fromProjectViewPsiFile(file: ProjectViewPsiFile): ProjectView {
      val rawItems = collectRawItems(file)
      val sections = buildSectionsMap(file.project, rawItems)
      return ProjectView(sections)
    }

    private fun collectRawItems(file: ProjectViewPsiFile): List<RawItem> {
      val rawSections = mutableListOf<RawItem>()
      val psiSections = file.getSectionsOrImports()
      for (section in psiSections) {
        when (section) {
          is ProjectViewPsiSection -> {
            val name = section.getKeyword().text.trim()
            val values = section.getItems().map { it.text.trim() }
            rawSections.add(RawSection(name, values))
          }
          is ProjectViewPsiImport, is ProjectViewPsiTryImport -> {
            val path = section.getImportPath()?.text?.trim() ?: ""
            rawSections.add(RawImport(path, section.isImportRequired))
          }
        }
      }
      return rawSections
    }

    private fun buildSectionsMap(project: Project, rawItems: List<RawItem>): Map<SectionKey<*>, Any> {
      val result = mutableMapOf<SectionKey<*>, Any>()
      for (item in rawItems) {
        when (item) {
          is RawSection -> {
            val section = ProjectViewSections.getSectionByName(item.name)
            val parsed = section?.fromRawValues(item.contents) ?: continue
            mergeSection(result, section.sectionKey, parsed)
          }
          is RawImport -> {
            val vFile = tryResolveImportFile(project, item.path, item.required) ?: continue
            handleImport(project, vFile, result)
          }
        }
      }
      return result
    }

    private fun mergeSection(
      target: MutableMap<SectionKey<*>, Any>,
      sectionKey: SectionKey<*>,
      value: Any,
    ) {
      val existing = target[sectionKey]
      if (existing is Collection<*> && value is Collection<*>) {
        target[sectionKey] = existing + value
      } else {
        target[sectionKey] = value
      }
    }

    private fun handleImport(
      project: Project,
      virtualFile: VirtualFile,
      into: MutableMap<SectionKey<*>, Any>,
    ) {
      val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? ProjectViewPsiFile ?: return
      val otherProjectView = fromProjectViewPsiFile(psiFile)
      for ((sectionKey, value) in otherProjectView.sections) {
        mergeSection(into, sectionKey, value)
      }
    }

    private fun tryResolveImportFile(
      project: Project,
      pathString: String,
      required: Boolean,
    ): VirtualFile? {
      val file = project.rootDir.resolveFromRootOrRelative(pathString)
      if (file == null && required) {
        error("Cannot find project view file requested in an import: $pathString")
      }
      return file
    }
  }
}
