package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sdkcompat.createModule
import org.jetbrains.bazel.sdkcompat.setProjectRootDir
import org.jetbrains.bazel.settings.bazel.openProjectViewInEditor
import org.jetbrains.bazel.settings.bazel.setProjectViewPath
import org.jetbrains.bazel.utils.refreshAndFindVirtualFile
import java.io.IOException
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private val log = logger<BazelProjectOpenProcessor>()

val BUILD_FILE_GLOB = "{${Constants.BUILD_FILE_NAMES.joinToString(",")}}"

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
    val (projectStoreBaseDir, options) =
      calculateOpenProjectTask(virtualFile, forceOpenInNewFrame, projectToClose)

    return ProjectManagerEx
      .getInstanceEx()
      .openProject(projectStoreBaseDir, options)
  }

  override suspend fun openProjectAsync(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    log.info("Opening asynchronously project :$virtualFile")
    val (projectStoreBaseDir, options) =
      calculateOpenProjectTask(virtualFile, forceOpenInNewFrame, projectToClose)

    return ProjectManagerEx
      .getInstanceEx()
      .openProjectAsync(projectStoreBaseDir, options)
  }

  private fun calculateOpenProjectTask(
    virtualFile: VirtualFile,
    forceOpenInNewFrame: Boolean,
    projectToClose: Project?,
  ): Pair<Path, OpenProjectTask> {
    // todo why do we even need to calculate the project root dir?
    // todo refactor
    val projectRootDir = findProjectFolderFromVFile(virtualFile)!!
    val projectViewPath = getProjectViewPath(projectRootDir, virtualFile)
    val projectStoreBaseDir = projectViewPath ?: virtualFile.toNioPath()

    return projectStoreBaseDir to
      OpenProjectTask {
        runConfigurators = true
        isRefreshVfsNeeded = !ApplicationManager.getApplication().isUnitTestMode

        setProjectRootDir(projectRootDir.toNioPathOrNull())
        this.forceOpenInNewFrame = forceOpenInNewFrame
        this.projectToClose = projectToClose
        createModule(Registry.`is`("bazel.create.fake.module.on.project.import"))

        beforeOpen = { project ->
          project.initProperties(projectRootDir)

          if (projectViewPath != null) {
            project.setProjectViewPath(projectViewPath)
            openProjectViewInEditor(project, projectViewPath)
          }

          true
        }
      }
  }

  override val icon: Icon
    get() = BazelPluginIcons.bazel

  override val name: String
    get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override val isStrongProjectInfoHolder: Boolean
    get() = BazelFeatureFlags.autoOpenProjectIfPresent

  /**
   * The project is only eligible to be opened with Bazel Plugin if workspace files can be reached from the given vFile
   */
  override fun canOpenProject(file: VirtualFile): Boolean {
    if (file.isDirectory && file.findChild(Project.DIRECTORY_STORE_FOLDER) != null) return false

    return findProjectFolderFromVFile(file) != null
  }

  /**
   * this method can be used to set up additional project properties before opening the Bazel project
   * @param virtualFile the virtual file passed to the project open processor
   */
  @RequiresBackgroundThread
  private fun getProjectViewPath(projectRootDir: VirtualFile, virtualFile: VirtualFile): Path? =
    when {
      virtualFile.isProjectViewFile() -> virtualFile.toNioPath()
      // BUILD file at the root can be treated as a workspace file in this context
      virtualFile.isBuildFile() -> {
        virtualFile.parent
          // ?.takeUnless { it.isWorkspaceRoot() }
          ?.let { calculateProjectViewFilePath(projectRootDir, it) }
      }

      virtualFile.isWorkspaceFile() -> null
      virtualFile.isWorkspaceRoot() -> null
      else -> {
        getBuildFileForPackageDirectory(virtualFile)
          ?.parent
          ?.let { calculateProjectViewFilePath(projectRootDir, it) }
      }
    }

  private fun calculateProjectViewFilePath(projectRootDir: VirtualFile, bazelPackageDir: VirtualFile): Path =
    ProjectViewFileUtils.calculateProjectViewFilePath(
      projectRootDir = projectRootDir,
      projectViewPath = null,
      overwrite = true,
      bazelPackageDir = bazelPackageDir,
    )
}

/**
 * when a file/subdirectory is selected for opening a Bazel project,
 * this method provides information about the real project directory to open
 */
tailrec fun findProjectFolderFromVFile(file: VirtualFile?): VirtualFile? =
  when {
    file == null -> null
    file.isWorkspaceRoot() -> file
    // this is to prevent opening a file that is not an acceptable Bazel config file, #BAZEL-1940
    // TODO(Son): figure out how to write a test for it to avoid regression later
    file.isFile && !file.isEligibleFile() -> null

    else -> findProjectFolderFromVFile(file.parent)
  }

private fun VirtualFile.isEligibleFile() = isWorkspaceFile() || isBuildFile() || isProjectViewFile()

private fun VirtualFile.isProjectViewFile() = isFile && extension == Constants.PROJECT_VIEW_FILE_EXTENSION

private fun VirtualFile.isWorkspaceRoot(): Boolean = toNioPath().isWorkspaceRoot()

private fun Path.isWorkspaceRoot(): Boolean =
  isDirectory() &&
    Constants.WORKSPACE_FILE_NAMES
      .asSequence()
      .map { resolve(it) }
      .any { it.isRegularFile() }

private fun VirtualFile.isWorkspaceFile() = isFile && name in Constants.WORKSPACE_FILE_NAMES

fun VirtualFile.isBuildFile() = isFile && name in Constants.BUILD_FILE_NAMES

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
