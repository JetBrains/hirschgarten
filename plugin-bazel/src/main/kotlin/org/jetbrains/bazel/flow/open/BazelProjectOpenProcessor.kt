package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import java.nio.file.Path
import javax.swing.Icon

private val log = logger<BazelProjectOpenProcessor>()

val BUILD_FILE_GLOB: String = Constants.BUILD_FILE_NAMES.joinToString(
  prefix = "{",
  separator = ",",
  postfix = "}",
)

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

    val path = virtualFile.toNioPath()
    val (projectRootDir, projectViewPath) = if (path.workspaceFile != null) {
      Pair(path, null)
    }
    else {
      val projectRootDir = findProjectFolderFromFile(path)!!
      val projectViewPath = getProjectViewPath(projectRootDir, path)
      Pair(projectRootDir, projectViewPath)
    }

    return (projectViewPath ?: projectRootDir.workspaceFile!!) to
      OpenProjectTask {
        runConfigurators = true
        isRefreshVfsNeeded = !ApplicationManager.getApplication().isUnitTestMode

        this.projectRootDir = projectRootDir
        this.forceOpenInNewFrame = forceOpenInNewFrame
        this.projectToClose = projectToClose
        this.createModule = Registry.`is`("bazel.create.fake.module.on.project.import")

        beforeOpen = { project ->
          // todo gets invoked twice
          project.initProperties(projectRootDir)

          projectViewPath
            ?.refreshAndFindVirtualFile()
            ?.let { projectViewPath ->
              project.bazelProjectSettings = project.bazelProjectSettings
                .withNewProjectViewPath(projectViewPath)
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
}
