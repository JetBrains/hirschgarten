package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.settings.bazel.setProjectViewPath
import java.io.IOException
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private val log = logger<BazelProjectOpenProcessor>()

val BUILD_FILE_GLOB = "{${Constants.BUILD_FILE_NAMES.joinToString(",")}}"

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

  override fun calculateBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    when {
      originalVFile.isProjectViewFile() -> projectViewFileBeforeOpenCallback(originalVFile)
      // BUILD file at the root can be treated as a workspace file in this context
      originalVFile.isBuildFile() && originalVFile.parent?.isWorkspaceRoot() == true -> { project -> }
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
      project.setProjectViewPath(outputProjectViewFilePath.toAbsolutePath())
    }

  private fun projectViewFileBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    { project ->
      project.setProjectViewPath(originalVFile.toNioPath().toAbsolutePath())
    }
}

tailrec fun findProjectFolderFromVFile(vFile: VirtualFile?): VirtualFile? =
  when {
    vFile == null -> null
    vFile.isWorkspaceRoot() -> vFile
    // this is to prevent opening a file that is not an acceptable Bazel config file, #BAZEL-1940
    // TODO(Son): figure out how to write a test for it to avoid regression later
    vFile.isFile && !vFile.isEligibleFile() -> null

    else -> findProjectFolderFromVFile(vFile.parent)
  }

private fun VirtualFile.isEligibleFile() = isWorkspaceFile() || isBuildFile() || isProjectViewFile()

private fun VirtualFile.isProjectViewFile() = isFile && extension == Constants.PROJECT_VIEW_FILE_EXTENSION

private fun VirtualFile.isWorkspaceRoot(): Boolean {
  if (!isDirectory) return false
  val path = toNioPath()
  return Constants.WORKSPACE_FILE_NAMES.any { path.resolve(it).isRegularFile() }
}

private fun VirtualFile.isWorkspaceFile() = isFile && name in Constants.WORKSPACE_FILE_NAMES

private fun VirtualFile.isBuildFile() = isFile && name in Constants.BUILD_FILE_NAMES
