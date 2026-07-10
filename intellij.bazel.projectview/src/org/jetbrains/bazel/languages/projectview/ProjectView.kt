package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

/**
 * Immutable representation of a ProjectView: a map of section keys to parsed values.
 */
class ProjectView internal constructor(
  internal val sections: Map<SectionKey<*>, Any>,
  @get:ApiStatus.Internal
  val imports: List<Import>,
) {
  @ApiStatus.Internal
  fun isEmpty(): Boolean = sections.isEmpty() && imports.isEmpty()

  @ApiStatus.Internal
  @Suppress("UNCHECKED_CAST")
  fun <T> getSection(key: SectionKey<T>): T? {
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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ProjectView

    if (sections != other.sections) return false
    if (imports != other.imports) return false

    return true
  }

  override fun hashCode(): Int {
    var result = sections.hashCode()
    result = 31 * result + imports.hashCode()
    return result
  }

  @ApiStatus.Internal
  companion object {

    val EMPTY: ProjectView = ProjectView(mapOf(), listOf())

    @RequiresReadLock
    @RequiresBackgroundThread(generateAssertion = false)
    fun fromProjectViewPsiFile(file: ProjectViewPsiFile, rootDir: VirtualFile = file.project.rootDir): ProjectView {
      return fromProjectViewPsiFile(file, rootDir, setOfNotNull(file.virtualFile))
    }

    @RequiresReadLock
    @RequiresBackgroundThread(generateAssertion = false)
    fun fromProjectViewContent(project: Project, content: String): ProjectView {
      val psiFile = PsiFileFactory.getInstance(project)
        .createFileFromText(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME, ProjectViewLanguage, content) as ProjectViewPsiFile
      return fromProjectViewPsiFile(psiFile)
    }

    private fun fromProjectViewPsiFile(
      file: ProjectViewPsiFile,
      rootDir: VirtualFile,
      visitedFiles: Set<VirtualFile>
    ): ProjectView {
      val imports = mutableListOf<Import>()
      val sections = mutableMapOf<SectionKey<*>, Any>()
      for (it in file.children) {
        when (it) {
          is ProjectViewPsiSection -> {
            val (section, value) = it.toSectionWithValue() ?: continue
            mergeSection(sections, section.sectionKey, value)
          }
          is ProjectViewPsiImportBase -> {
            val import = Import.from(rootDir, it)
            imports += import
            // skip if recursion detected
            if (import is Import.Resolved && import.file !in visitedFiles) {
              parseImport(
                project = file.project,
                import = import,
                into = sections,
                rootDir = rootDir,
                visitedFiles = visitedFiles,
              )
            }
          }
        }
      }
      return ProjectView(sections, imports)
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

    private fun parseImport(
      project: Project,
      import: Import.Resolved,
      into: MutableMap<SectionKey<*>, Any>,
      rootDir: VirtualFile,
      visitedFiles: Set<VirtualFile>,
    ) {
      val psiFile = PsiManager.getInstance(project).findFile(import.file) as? ProjectViewPsiFile ?: return
      val otherProjectView = fromProjectViewPsiFile(psiFile, rootDir, visitedFiles + import.file)
      for ((sectionKey, value) in otherProjectView.sections) {
        mergeSection(into, sectionKey, value)
      }
    }

    private fun ProjectViewPsiSection.toSectionWithValue(): Pair<Section<*>, Any>? {
      val name = getKeyword().text.trim()
      val contents = getItems().map { it.text.trim() }
      val section = ProjectViewSections.getSectionByName(name) ?: return null
      val value = section.fromRawValues(contents) ?: return null
      return section to value
    }
  }
}
