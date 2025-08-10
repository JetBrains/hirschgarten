package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.settings.bazel.setProjectViewPath
import org.jetbrains.bazel.utils.refreshAndFindVirtualFile
import java.io.IOException
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private val log = logger<BazelProjectOpenProcessor>()

internal val BUILD_FILE_GLOB = "{${Constants.BUILD_FILE_NAMES.joinToString(",")}}"

/**
 * Refrain from using [VirtualFile.getChildren] as it causes performance issues in large projects, such as [BAZEL-1717](https://youtrack.jetbrains.com/issue/BAZEL-1717)
 */
internal class BazelProjectOpenProcessor : ProjectOpenProcessor() {
  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    log.info("Opening project :$virtualFile")
    val vFileToOpen = calculateProjectFolderToOpen(virtualFile)
    val projectPath = vFileToOpen.toNioPath()
    val openProjectTask =
      calculateOpenProjectTask(
        projectPath = projectPath,
        forceOpenInNewFrame = forceOpenInNewFrame,
        projectToClose = projectToClose,
        vFileToOpen = vFileToOpen,
        originalVFile = virtualFile,
      )

    return ProjectManagerEx.getInstanceEx().openProject(projectPath, openProjectTask)
  }

  override suspend fun openProjectAsync(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    log.info("Opening asynchronously project :$virtualFile")
    val vFileToOpen = calculateProjectFolderToOpen(virtualFile)
    val projectPath = vFileToOpen.toNioPath()
    val openProjectTask =
      calculateOpenProjectTask(
        projectPath = projectPath,
        forceOpenInNewFrame = forceOpenInNewFrame,
        projectToClose = projectToClose,
        vFileToOpen = vFileToOpen,
        originalVFile = virtualFile,
      )

    return ProjectManagerEx.getInstanceEx().openProjectAsync(projectPath, openProjectTask)
  }

  private fun calculateOpenProjectTask(
    projectPath: Path,
    forceOpenInNewFrame: Boolean,
    projectToClose: Project?,
    vFileToOpen: VirtualFile,
    originalVFile: VirtualFile,
  ): OpenProjectTask =
    OpenProjectTask {
      runConfigurators = true
      isNewProject = !ProjectUtilCore.isValidProjectPath(projectPath)
      isRefreshVfsNeeded = !ApplicationManager.getApplication().isUnitTestMode

      this.forceOpenInNewFrame = forceOpenInNewFrame
      this.projectToClose = projectToClose

      beforeOpen = {
        it.initProperties(vFileToOpen)
        calculateBeforeOpenCallback(originalVFile).invoke(it)
        true
      }
    }

  /**
   * when a file/subdirectory is selected for opening a Bazel project,
   * this method provides information about the real project directory to open
   */
  fun calculateProjectFolderToOpen(virtualFile: VirtualFile): VirtualFile =
    findProjectFolderFromVFile(virtualFile)
      ?: error("Cannot find the suitable Bazel project folder to open for the given file $virtualFile.")

  override val icon: Icon = BazelPluginIcons.bazel

  override val name: String = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override val isStrongProjectInfoHolder: Boolean
    get() = BazelFeatureFlags.autoOpenProjectIfPresent

  /**
   * The project is only eligible to be opened with Bazel Plugin if workspace files can be reached from the given vFile
   */
  override fun canOpenProject(file: VirtualFile): Boolean = findProjectFolderFromVFile(file) != null

  /**
   * this method can be used to set up additional project properties before opening the Bazel project
   * @param originalVFile the virtual file passed to the project open processor
   */
  private fun calculateBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    when {
      originalVFile.isProjectViewFile() -> projectViewFileBeforeOpenCallback(originalVFile)
      // BUILD file at the root can be treated as a workspace file in this context
      originalVFile.isBuildFile() && originalVFile.parent?.isWorkspaceRoot() == true -> { _ -> }
      originalVFile.isBuildFile() -> buildFileBeforeOpenCallback(originalVFile)
      originalVFile.isWorkspaceFile() -> { _ -> }
      originalVFile.isWorkspaceRoot() -> { _ -> }
      else -> {
        val buildFile = getBuildFileForPackageDirectory(originalVFile)
        buildFile?.let { buildFileBeforeOpenCallback(it) } ?: {}
      }
    }

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

fun getBuildFileForPackageDirectory(packageDirectory: VirtualFile): VirtualFile? {
  try {
    if (!packageDirectory.isDirectory) return null
    val path = packageDirectory.toNioPath()
    return path
      .listDirectoryEntries(
        glob = BUILD_FILE_GLOB,
      ).firstOrNull { it.isRegularFile() }
      ?.refreshAndFindVirtualFile()
  } catch (e: IOException) {
    log.warn("Cannot retrieve Bazel BUILD file from directory ${packageDirectory.toNioPath()}", e)
    return null
  }
}

fun Project.initProperties(projectRootDir: VirtualFile) {
  thisLogger().debug("Initializing properties for project: $projectRootDir")

  this.isBazelProject = true
  this.rootDir = projectRootDir
}
