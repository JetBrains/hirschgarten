package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.diagnostic.getOrHandleException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.languages.projectview.imports.ImportFactory
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.utils.findVirtualFile
import java.nio.file.Path
import kotlin.io.path.readBytes

/**
 * The project view is parsed using light PSI rather than regular PSI.
 *
 * Using regular PSI was leading to problems when used in `BazelProjectStorePathCustomizer`. Also, using regular PSI is not needed.
 *
 * Regular PSI might lead to FileDocumentManagerListener.fileContentLoaded publishing,
 * which reaches the listeners of every project - including the one that might be currently initialized.
 * In our case BreakpointInstrumentationFileDocumentManagerListener, could be triggered resulting in some project-level service access,
 * but it's not allowed before the project is fully initialized.
 *
 * It is hard to reproduce because in regular usage `BazelProjectStorePathCustomizer` is called multiple times e.g. before the project init.
 * `BazelProjectStorePathCustomizer` call before actual project init is safe, and it keeps the project view file cached,
 * so the later call during the project init does not produce the event (and therefore the error).
 * However, if the GC clears the loaded file, then the call during project init needs to load the file again, and it leads to exception.
 */
@ApiStatus.Internal
object ProjectViewFactory {

  @RequiresBackgroundThread(generateAssertion = false)
  fun fromOrNull(
    project: Project,
    path: Path,
    root: Path = project.rootDir.toNioPath(),
  ): ProjectView? = runCatching { from(project, path, root) }
    // In principle, project view parse should not throw.
    // However, in case it does, it might break some code paths e.g. project opening, so it's better to keep it in a try-catch block.
    .getOrHandleException {
      log.error("Failed to load project view file at $path.", it)
    }

  @RequiresBackgroundThread(generateAssertion = false)
  fun fromDefault(
    project: Project,
    forceDefaultTemplate: Boolean = false,
  ): ProjectView = from(
    project = project,
    content = ProjectViewFileUtils.projectViewTemplate(project.rootDir, forceDefaultTemplate = forceDefaultTemplate),
  )

  @RequiresBackgroundThread(generateAssertion = false)
  fun from(
    project: Project,
    source: Path,
    root: Path = project.rootDir.toNioPath(),
  ): ProjectView = from(project, readContent(source), root, source)

  @RequiresBackgroundThread(generateAssertion = false)
  fun from(
    project: Project,
    content: CharSequence,
    root: Path = project.rootDir.toNioPath(),
    source: Path? = null,
  ): ProjectView {
    val name = source?.fileName?.toString() ?: Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
    return fromPsiFile(createLightPsiFile(project, name, content), source, root, setOfNotNull(source))
  }

  private fun fromPsiFile(
    psi: ProjectViewPsiFile,
    source: Path?,
    root: Path,
    visited: Set<Path>,
  ): ProjectView {
    val imports = mutableListOf<Import>()
    val sections = mutableMapOf<SectionKey<*>, Any>()
    for (it in psi.children) {
      when (it) {
        is ProjectViewPsiSection -> {
          val (section, value) = it.toSectionWithValue() ?: continue
          mergeSection(sections, section.sectionKey, value)
        }

        is ProjectViewPsiImportBase -> {
          val import = ImportFactory.from(root, it, source)
          imports += import
          // skip if recursion detected
          if (import is Import.Resolved && import.path !in visited) {
            parseImport(
              project = psi.project,
              import = import,
              into = sections,
              root = root,
              visited = visited,
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
    }
    else {
      target[sectionKey] = value
    }
  }

  private fun parseImport(
    project: Project,
    import: Import.Resolved,
    into: MutableMap<SectionKey<*>, Any>,
    root: Path,
    visited: Set<Path>,
  ) {
    val psiFile = runCatching { createLightPsiFile(project, import.path.fileName.toString(), readContent(import.path)) }
      .getOrHandleException { log.warn("Failed to parse imported project view file ${import.path}. Skipping the import.", it) }
      ?: return
    val otherProjectView = fromPsiFile(psiFile, import.path, root, visited.plusElement(import.path))
    for ((sectionKey, value) in otherProjectView.sections) {
      mergeSection(into, sectionKey, value)
    }
  }

  /**
   * - the file already has a document - take its text, so that unsaved editor changes are not lost.
   *   [FileDocumentManager.getCachedDocument] returns an already loaded document and never loads one.
   * - otherwise - read the file itself.
   *
   * [LoadTextUtil] decodes the bytes, so the charset of the file is respected and its byte order mark is stripped.
   */
  private fun readContent(path: Path): CharSequence {
    val document = path.findVirtualFile()?.let { FileDocumentManager.getInstance().getCachedDocument(it) }
    return document?.immutableCharSequence ?: LoadTextUtil.getTextByBinaryPresentation(path.readBytes(), Charsets.UTF_8)
  }

  private fun createLightPsiFile(project: Project, name: String, content: CharSequence): ProjectViewPsiFile {
    val lightFile = ReadOnlyLightVirtualFile(name, ProjectViewLanguage, content)
    val psiFileFactory = PsiFileFactory.getInstance(project)
    val factory = psiFileFactory as? PsiFileFactoryImpl ?: error("Expected ${PsiFileFactoryImpl::class.simpleName}, got ${psiFileFactory.javaClass.name}")
    val psiFile = factory.trySetupPsiForFile(lightFile, ProjectViewLanguage, false, false)
    return psiFile as? ProjectViewPsiFile ?: error("Expected ${ProjectViewPsiFile::class.simpleName} for $name, got ${psiFile?.javaClass?.name}")
  }

  private fun ProjectViewPsiSection.toSectionWithValue(): Pair<Section<*>, Any>? {
    val name = getKeyword().text.trim()
    val contents = getItems().map { it.text.trim() }
    val section = ProjectViewSections.getSectionByName(name) ?: return null
    val value = section.fromRawValues(contents) ?: return null
    return section to value
  }

  private val log = logger<ProjectViewFactory>()
}
