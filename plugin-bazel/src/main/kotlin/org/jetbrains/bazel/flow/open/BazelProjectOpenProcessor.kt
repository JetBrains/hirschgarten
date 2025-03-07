package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants.BAZELBSP_JSON_FILE_NAME
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import java.io.IOException
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private val log = logger<BazelProjectOpenProcessor>()

val ALL_ELIGIBLE_FILES_GLOB =
  buildString {
    append("{")
    append(BAZELBSP_JSON_FILE_NAME)
    append(",")
    append(BazelPluginConstants.WORKSPACE_FILE_NAMES.joinToString(","))
    append(",")
    append(BazelPluginConstants.BUILD_FILE_NAMES.joinToString(","))
    append(",*.")
    append(BazelPluginConstants.PROJECT_VIEW_FILE_EXTENSION)
    append("}")
  }

val BUILD_FILE_GLOB = "{${BazelPluginConstants.BUILD_FILE_NAMES.joinToString(",")}}"

/**
 * Refrain from using [VirtualFile.getChildren] as it causes performance issues in large projects, such as [BAZEL-1717](https://youtrack.jetbrains.com/issue/BAZEL-1717)
 */
internal class BazelProjectOpenProcessor : BaseProjectOpenProcessor() {
  override fun calculateProjectFolderToOpen(virtualFile: VirtualFile): VirtualFile =
    findProjectFolderFromVFile(virtualFile)
      ?: error("Cannot find the suitable Bazel project folder to open for the given file $virtualFile.")

  override val icon: Icon = BazelPluginIcons.bazel

  // during EAP phase, show "Bazel (EAP)" during import only to disambiguate from OG Bazel plugin,
  // in case both are installed. See also: https://github.com/bazelbuild/intellij/pull/7092
  override val name: String = BazelPluginConstants.BAZEL_DISPLAY_NAME + " (EAP)"

  override val isStrongProjectInfoHolder: Boolean
    get() = ApplicationManager.getApplication().isHeadlessEnvironment

  /**
   * The project is only eligible to be opened with Bazel Plugin if workspace files can be reached from the given vFile
   */
  override fun canOpenProject(file: VirtualFile): Boolean = findProjectFolderFromVFile(file) != null

  private fun VirtualFile.isEligibleFile() = isBazelBspConnectionFile() || isWorkspaceFile() || isBuildFile() || isProjectViewFile()

  /**
   * Basic name matching to quickly check for an eligible bazel bsp connection file
   */
  private fun VirtualFile.isBazelBspConnectionFile() = name == BAZELBSP_JSON_FILE_NAME

  override fun calculateBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    when {
      originalVFile.isProjectViewFile() -> projectViewFileBeforeOpenCallback(originalVFile)
      originalVFile.isBuildFile() -> buildFileBeforeOpenCallback(originalVFile)
      originalVFile.isWorkspaceFile() -> { project -> }
      originalVFile.isWorkspaceRoot() -> { project -> }
      else -> {
        val buildFile = originalVFile.getContainingBuildFile()
        buildFile?.let { buildFileBeforeOpenCallback(it) } ?: {}
      }
    }

  private fun VirtualFile.getContainingBuildFile(): VirtualFile? {
    try {
      if (!isDirectory) return null
      val path = toNioPath()
      return path
        .listDirectoryEntries(
          glob = BUILD_FILE_GLOB,
        ).firstOrNull { it.isRegularFile() }
        ?.toVirtualFile()
    } catch (e: IOException) {
      log.warn("Cannot retrieve Bazel BUILD file from directory $this", e)
      return null
    }
  }

  private fun Path.toVirtualFile(): VirtualFile? = LocalFileSystem.getInstance().findFileByNioFile(this)

  private fun buildFileBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    fun(project) {
      val bazelPackageDir = originalVFile.parent ?: return
      val outputProjectViewFilePath =
        ProjectViewFileUtils.calculateProjectViewFilePath(
          project = project,
          generateContent = true,
          overwrite = true,
          bazelPackageDir = bazelPackageDir.toNioPath(),
        )
      project.bazelProjectSettings =
        project.bazelProjectSettings.withNewProjectViewPath(outputProjectViewFilePath.toAbsolutePath())
    }

  private fun projectViewFileBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    { project ->
      project.bazelProjectSettings =
        project.bazelProjectSettings.withNewProjectViewPath(originalVFile.toNioPath().toAbsolutePath())
    }

  private fun VirtualFile.isProjectViewFile() = extension == BazelPluginConstants.PROJECT_VIEW_FILE_EXTENSION
}

tailrec fun findProjectFolderFromVFile(vFile: VirtualFile?): VirtualFile? =
  when {
    vFile == null -> null
    vFile.isWorkspaceRoot() -> vFile
    else -> findProjectFolderFromVFile(vFile.parent)
  }

private fun VirtualFile.isWorkspaceRoot(): Boolean {
  if (!isDirectory) return false
  val path = toNioPath()
  return BazelPluginConstants.WORKSPACE_FILE_NAMES.any { path.resolve(it).isRegularFile() }
}

private fun VirtualFile.isWorkspaceFile() = isFile && name in BazelPluginConstants.WORKSPACE_FILE_NAMES

private fun VirtualFile.isBuildFile() = isFile && name in BazelPluginConstants.BUILD_FILE_NAMES
