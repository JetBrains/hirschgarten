package org.jetbrains.bazel.workspace.indexAdditionalFiles

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.indexAdditionalFilesInDirectories
import org.jetbrains.bazel.languages.projectview.indexAllFilesInDirectories

private val DEFAULT_ADDITIONAL_FILE_PATTERNS: List<String> =
  Constants.WORKSPACE_FILE_NAMES.toList() +
  Constants.BUILD_FILE_NAMES.toList() +
  listOf(Constants.MODULE_BAZEL_FILE_NAME) +
  Constants.SUPPORTED_EXTENSIONS.map { extension -> "*.$extension" }

@ApiStatus.Internal
fun Project.limitedFilesIndexingGlobOrNull(): ProjectViewGlobSet? =
  limitedFilesIndexingGlobOrNull(ProjectViewService.getInstance(this).projectView)

@ApiStatus.Internal
fun Project.limitedFilesIndexingGlobOrNull(projectView: ProjectView): ProjectViewGlobSet? {
  if (projectView.indexAllFilesInDirectories) return null

  return ProjectViewGlobSet(
    rootDir = rootDir.toNioPath(),
    patterns = projectView.indexAdditionalFilesInDirectories + DEFAULT_ADDITIONAL_FILE_PATTERNS,
  )
}

@ApiStatus.Internal
class AdditionalFilesCollector(
  private val additionalFilePatterns: ProjectViewGlobSet,
  private val includedRoots: Set<VirtualFile>,
  private val excludedRoots: Set<VirtualFile>,
  private val contentRoots: Set<VirtualFile>,
) {
  /**
   * Used during sync to discover all additional files under included roots that are not already covered by content roots.
   */
  fun collectAdditionalFilesToIndex(): Set<VirtualFile> {
    val includedRootsToIterate = includedRoots.filterNot(::isUnderContentRoot)
    val visited = hashSetOf<VirtualFile>()
    val result = hashSetOf<VirtualFile>()

    for (includedRoot in includedRootsToIterate) {
      VfsUtilCore.visitChildrenRecursively(
        includedRoot,
        object : VirtualFileVisitor<Unit>() {
          override fun visitFileEx(file: VirtualFile): Result {
            if (shouldSkipChildren(file)) return SKIP_CHILDREN
            if (!visited.add(file)) return SKIP_CHILDREN
            if (file.isDirectory) return CONTINUE
            if (matchesAdditionalFilePatterns(file)) {
              result.add(file)
            }
            return CONTINUE
          }
        },
      )
    }

    return result
  }

  /**
   * Returns true if the file form VFS event matches the additional file pattern and should be indexed.
   */
  fun shouldIndexFile(file: VirtualFile): Boolean =
    file.isValid &&
    file.isUnderIncludedRoot() &&
    !file.isUnderExcludedRoot() &&
    !isUnderContentRoot(file) &&
    matchesAdditionalFilePatterns(file)

  private fun isUnderContentRoot(file: VirtualFile): Boolean {
    var current: VirtualFile? = file
    while (current != null) {
      if (current in contentRoots) return true
      if (current in excludedRoots) return false
      current = current.parent
    }
    return false
  }

  private fun shouldSkipChildren(file: VirtualFile): Boolean =
    file in excludedRoots || file in contentRoots

  private fun matchesAdditionalFilePatterns(file: VirtualFile): Boolean =
    !file.isDirectory && file.toNioPathOrNull()?.let(additionalFilePatterns::matches) == true

  private fun VirtualFile.isUnderIncludedRoot(): Boolean =
    includedRoots.any { root -> root.isValid && root.isEqualOrAncestorOf(this) }

  private fun VirtualFile.isUnderExcludedRoot(): Boolean =
    excludedRoots.any { root -> root.isValid && root.isEqualOrAncestorOf(this) }

  private fun VirtualFile.isEqualOrAncestorOf(file: VirtualFile): Boolean {
    var current: VirtualFile? = file
    while (current != null) {
      if (current == this) return true
      current = current.parent
    }
    return false
  }
}
