package org.jetbrains.bazel.workspace

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SlowOperations
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspacemodel.entities.BspDummyEntitySource

/**
 * This class is intended to replace [DummyModuleExclusionWorkspaceFileIndexContributor] as a mechanism to instruct
 * the IntelliJ Platform to skip indexing files associated with dummy modules.
 * However, a limitation of this approach is that basic syntax highlighting will not be enabled for the relevant source files,
 * and their icons in the Project view will appear as plain text icons.
 */
class DummyModuleSourcePlainTextTypeOverrider : FileTypeOverrider {
  override fun getOverriddenFileType(file: VirtualFile): FileType? {
    if (!file.isSourceFile()) return null
    val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return null
    if (!project.isBspProject) return null

    val projectFileIndex = ProjectFileIndex.getInstance(project)
    val module =
      SlowOperations.knownIssue("BAZEL-1098").use {
        runReadAction {
          projectFileIndex.getModuleForFile(file)
        }
      } ?: return null

    if (module.moduleEntity?.entitySource != BspDummyEntitySource) return null
    return BspPlainTextFileType(file.extension ?: "txt")
  }
}
